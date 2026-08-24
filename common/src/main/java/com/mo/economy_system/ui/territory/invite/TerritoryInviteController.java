package com.mo.economy_system.ui.territory.invite;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shared invite loading, filtering, paging, timeout, and duplicate-click semantics. */
public final class TerritoryInviteController
    extends AbstractEconomyScreenController<TerritoryInviteState, TerritoryInviteEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  public static final long CLICK_COOLDOWN_TICKS = 15;

  private final TerritoryInvitePort port;
  private final UUID territoryId;
  private long startedAt;
  private boolean inFlight;
  private boolean submitted;

  public TerritoryInviteController(UUID territoryId, String territoryName, UUID ownerId,
      UUID viewerId, java.util.Set<UUID> existingMemberIds, TerritoryInvitePort port) {
    super(new TerritoryInviteState(territoryId, territoryName, ownerId, viewerId,
        existingMemberIds, List.of(), "", 0, 1, ScreenState.IDLE, null, -1, 0, 0));
    this.territoryId = territoryId;
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override public void handle(TerritoryInviteEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof TerritoryInviteEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof TerritoryInviteEvent.Retry value) request(value.nowNanos());
    else if (event instanceof TerritoryInviteEvent.PlayersLoaded value) loaded(value);
    else if (event instanceof TerritoryInviteEvent.PlayersFailed value) failed(value);
    else if (event instanceof TerritoryInviteEvent.FilterChanged value) filter(value.value());
    else if (event instanceof TerritoryInviteEvent.ViewportChanged value) viewport(value.pageSize());
    else if (event instanceof TerritoryInviteEvent.Scroll value) page(Integer.signum(value.steps()));
    else if (event instanceof TerritoryInviteEvent.InviteClicked value) invite(value);
    else if (event instanceof TerritoryInviteEvent.Back) navigate(new UiNavigation.Back());
    else if (event instanceof TerritoryInviteEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("invite request id exhausted");
    startedAt = nowNanos;
    inFlight = true;
    replace(copy(0, state().pageSize(), ScreenState.LOADING, null, id,
        state().playerRevision(), state().cooldownUntilTick(), state().players()));
    port.requestPlayers(id);
  }

  private void loaded(TerritoryInviteEvent.PlayersLoaded event) {
    if (!inFlight || state().screenState() != ScreenState.LOADING
        || event.requestId() != state().requestId()
        || event.revision() < state().playerRevision()) return;
    inFlight = false;
    TerritoryInviteState next = copy(0, state().pageSize(), ScreenState.READY, null, -1,
        event.revision(), state().cooldownUntilTick(), event.players());
    ScreenState status = next.eligiblePlayers().isEmpty() ? ScreenState.EMPTY : ScreenState.READY;
    replace(copy(0, state().pageSize(), status, null, -1, event.revision(),
        state().cooldownUntilTick(), event.players()));
  }

  private void failed(TerritoryInviteEvent.PlayersFailed event) {
    if (!inFlight || state().screenState() != ScreenState.LOADING
        || event.requestId() != state().requestId()) return;
    inFlight = false;
    replace(copy(state().page(), state().pageSize(), ScreenState.ERROR, event.errorKey(), -1,
        state().playerRevision(), state().cooldownUntilTick(), state().players()));
  }

  private void filter(String value) {
    replace(copy(0, state().pageSize(), state().screenState(), state().errorKey(),
        state().requestId(), state().playerRevision(), state().cooldownUntilTick(), state().players(),
        value == null ? "" : value));
  }

  private void viewport(int pageSize) {
    int size = Math.max(1, pageSize);
    int pages = Math.max(1, (state().eligiblePlayers().size() + size - 1) / size);
    int page = Math.min(state().page(), pages - 1);
    replace(copy(page, size, state().screenState(), state().errorKey(), state().requestId(),
        state().playerRevision(), state().cooldownUntilTick(), state().players()));
  }

  private void page(int delta) {
    if (delta == 0) return;
    int page = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
    replace(copy(page, state().pageSize(), state().screenState(), state().errorKey(),
        state().requestId(), state().playerRevision(), state().cooldownUntilTick(), state().players()));
  }

  private void invite(TerritoryInviteEvent.InviteClicked event) {
    if (submitted || !state().canInvite(event.playerId())
        || event.tick() < state().cooldownUntilTick()) return;
    long until = event.tick() > Long.MAX_VALUE - CLICK_COOLDOWN_TICKS
        ? Long.MAX_VALUE : event.tick() + CLICK_COOLDOWN_TICKS;
    submitted = true;
    port.submitInvite(territoryId, event.playerId());
    replace(copy(state().page(), state().pageSize(), state().screenState(), state().errorKey(),
        state().requestId(), state().playerRevision(), until, state().players()));
    navigate(new UiNavigation.Back());
  }

  private void tick(long nowNanos) {
    if (inFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      inFlight = false;
      replace(copy(state().page(), state().pageSize(), ScreenState.ERROR,
          "screen.invite.sync_timeout", -1, state().playerRevision(), state().cooldownUntilTick(),
          state().players()));
    }
  }

  private TerritoryInviteState copy(int page, int pageSize, ScreenState status, String error,
      long requestId, long revision, long cooldown, List<PlayerSummary> players) {
    return copy(page, pageSize, status, error, requestId, revision, cooldown, players, state().filter());
  }

  private TerritoryInviteState copy(int page, int pageSize, ScreenState status, String error,
      long requestId, long revision, long cooldown, List<PlayerSummary> players, String filter) {
    return new TerritoryInviteState(state().territoryId(), state().territoryName(), state().ownerId(),
        state().viewerId(), state().existingMemberIds(), players, filter, Math.max(0, page),
        Math.max(1, pageSize), status, error, requestId, Math.max(0, revision), Math.max(0, cooldown));
  }

  private void replace(TerritoryInviteState next) { replaceState(next); }
}
