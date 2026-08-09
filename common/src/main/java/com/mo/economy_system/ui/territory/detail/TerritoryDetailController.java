package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shared state machine for the territory detail and nested administration views. */
public final class TerritoryDetailController
    extends AbstractEconomyScreenController<TerritoryDetailState, TerritoryDetailEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;

  private final TerritoryDetailPort port;
  private final UUID territoryId;
  private long startedAt;
  private boolean territoryInFlight;

  public TerritoryDetailController(Owned initial, TerritoryDetailPort port) {
    this(initial, List.of(), 0, port);
  }

  public TerritoryDetailController(
      Owned initial, List<PlayerSummary> players, long playerRevision, TerritoryDetailPort port) {
    super(new TerritoryDetailState(
        Objects.requireNonNull(initial, "initial"),
        Objects.requireNonNull(players, "players"),
        TerritoryDetailViewKind.MAIN,
        0,
        1,
        "",
        ScreenState.IDLE,
        null,
        -1,
        Math.max(0, playerRevision)));
    this.port = Objects.requireNonNull(port, "port");
    this.territoryId = initial.summary().territoryId();
  }

  /** Selects the nested view requested by an entry shell before the first request starts. */
  public void selectInitialView(TerritoryDetailViewKind view) {
    Objects.requireNonNull(view, "view");
    if (state().screenState() != ScreenState.IDLE) return;
    replaceState(copy(state().territory(), state().players(), view, 0, state().pageSize(),
        state().filter(), state().screenState(), state().errorKey(), state().requestId(),
        state().playerRevision()));
  }

  @Override
  public void handle(TerritoryDetailEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof TerritoryDetailEvent.Initialize value) {
      request(value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.Retry value) {
      request(value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.TerritoryLoaded value) {
      loaded(value);
    } else if (event instanceof TerritoryDetailEvent.TerritoryFailed value) {
      failed(value);
    } else if (event instanceof TerritoryDetailEvent.PlayersLoaded value) {
      playersLoaded(value);
    } else if (event instanceof TerritoryDetailEvent.ViewSelected value) {
      selectView(value.view());
    } else if (event instanceof TerritoryDetailEvent.FilterChanged value) {
      filter(value.value());
    } else if (event instanceof TerritoryDetailEvent.ViewportChanged value) {
      viewport(value.pageSize());
    } else if (event instanceof TerritoryDetailEvent.Scroll value) {
      page(Integer.signum(value.steps()));
    } else if (event instanceof TerritoryDetailEvent.RuleClicked value) {
      cycleRule(value.action(), value.nowNanos());
    } else if (event instanceof TerritoryDetailEvent.ActionClicked value) {
      action(value);
    } else if (event instanceof TerritoryDetailEvent.Tick value) {
      tick(value.nowNanos());
    }
  }

  private void request(long nowNanos) {
    long requestId = port.nextRequestId();
    if (requestId < 0) throw new IllegalStateException("territory detail request id must be non-negative");
    startedAt = nowNanos;
    territoryInFlight = true;
    replaceState(copy(
        state().territory(), state().players(), state().view(), 0, state().pageSize(),
        state().filter(), ScreenState.LOADING, null, requestId, state().playerRevision()));
    port.requestTerritory(territoryId, requestId);
    port.requestPlayers();
  }

  private void loaded(TerritoryDetailEvent.TerritoryLoaded event) {
    if (!territoryInFlight || state().screenState() != ScreenState.LOADING
        || state().requestId() != event.requestId()) return;
    territoryInFlight = false;
    Owned value = Objects.requireNonNull(event.territory(), "territory");
    replaceState(copy(value, state().players(), state().view(), 0, state().pageSize(),
        state().filter(), ScreenState.READY, null, -1, state().playerRevision()));
  }

  private void failed(TerritoryDetailEvent.TerritoryFailed event) {
    if (!territoryInFlight || state().screenState() != ScreenState.LOADING
        || state().requestId() != event.requestId()) return;
    territoryInFlight = false;
    replaceState(copy(state().territory(), state().players(), state().view(), state().scroll(),
        state().pageSize(), state().filter(), ScreenState.ERROR,
        event.errorKey() == null || event.errorKey().isBlank()
            ? "screen.territory.detail.sync_failed" : event.errorKey(),
        -1, state().playerRevision()));
  }

  private void playersLoaded(TerritoryDetailEvent.PlayersLoaded event) {
    if (event.revision() < state().playerRevision()) return;
    List<PlayerSummary> players = Objects.requireNonNull(event.players(), "players");
    TerritoryDetailState next = copy(state().territory(), players, state().view(),
        state().scroll(), state().pageSize(), state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), event.revision());
    int page = Math.max(0, Math.min(next.scroll(), next.totalPages() - 1));
    replaceState(copy(next.territory(), next.players(), next.view(), page, next.pageSize(),
        next.filter(), next.screenState(), next.errorKey(), next.requestId(), next.playerRevision()));
  }

  private void selectView(TerritoryDetailViewKind view) {
    Objects.requireNonNull(view, "view");
    if (state().screenState() != ScreenState.IDLE
        && state().screenState() != ScreenState.READY
        && state().screenState() != ScreenState.EMPTY) return;
    replaceState(copy(state().territory(), state().players(), view, 0, state().pageSize(),
        state().filter(), state().screenState(), state().errorKey(), state().requestId(),
        state().playerRevision()));
  }

  private void filter(String value) {
    String next = value == null ? "" : value;
    replaceState(copy(state().territory(), state().players(), state().view(), 0,
        state().pageSize(), next, state().screenState(), state().errorKey(),
        state().requestId(), state().playerRevision()));
  }

  private void viewport(int size) {
    int pageSize = Math.max(1, size);
    replaceState(copy(state().territory(), state().players(), state().view(),
        clampPage(state().scroll(), pageSize), pageSize, state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().playerRevision()));
  }

  private void page(int delta) {
    if (delta == 0) return;
    replaceState(copy(state().territory(), state().players(), state().view(),
        clampPage(state().scroll() + delta), state().pageSize(), state().filter(),
        state().screenState(), state().errorKey(), state().requestId(), state().playerRevision()));
  }

  private void action(TerritoryDetailEvent.ActionClicked event) {
    TerritoryDetailAction action = event.action();
    if (action == null) return;
    if (action == TerritoryDetailAction.BACK) {
      if (state().view() == TerritoryDetailViewKind.MAIN) navigate(new UiNavigation.Back());
      else selectView(TerritoryDetailViewKind.MAIN);
      return;
    }
    if (action == TerritoryDetailAction.RETRY) return;
    if (!state().can(action)) return;
    switch (action) {
      case RESIZE -> port.resize(territoryId);
      case BUFFS -> navigate(new UiNavigation.Target("territory-buffs"));
      case ACCESS -> selectView(TerritoryDetailViewKind.ACCESS);
      case RULES -> selectView(TerritoryDetailViewKind.RULES);
      case TRANSFER -> selectView(TerritoryDetailViewKind.TRANSFER);
      case TOGGLE_ACCESS -> toggleAccess(event.targetId(), event.nowNanos());
      case TRANSFER_OWNERSHIP -> transfer(event.targetId());
      default -> { }
    }
  }

  private void cycleRule(RuleAction action, long nowNanos) {
    if (!state().can(TerritoryDetailAction.CYCLE_RULE)) return;
    Rule current = state().territory().rules().stream()
        .filter(rule -> rule.action() == action).findFirst().orElse(null);
    if (current == null) return;
    RuleLevel next = switch (current.level()) {
      case OWNER_ONLY -> RuleLevel.MEMBERS;
      case MEMBERS -> RuleLevel.EVERYONE;
      case EVERYONE -> RuleLevel.OWNER_ONLY;
    };
    port.submitRule(territoryId, action, next);
    request(nowNanos);
  }

  private void toggleAccess(UUID playerId, long nowNanos) {
    if (playerId == null) return;
    TerritoryAccessRow row = state().accessRows().stream()
        .filter(value -> value.playerId().equals(playerId)).findFirst().orElse(null);
    if (row == null) return;
    port.submitAccess(territoryId, playerId, !row.allowed());
    request(nowNanos);
  }

  private void transfer(UUID playerId) {
    if (playerId == null || state().transferRows().stream()
        .noneMatch(value -> value.playerId().equals(playerId))) return;
    port.submitTransfer(territoryId, playerId);
    navigate(new UiNavigation.Back());
  }

  private void tick(long nowNanos) {
    if (territoryInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      territoryInFlight = false;
      replaceState(copy(state().territory(), state().players(), state().view(), state().scroll(),
          state().pageSize(), state().filter(), ScreenState.ERROR,
          "screen.territory.detail.sync_timeout", -1, state().playerRevision()));
    }
  }

  private int clampPage(int page) { return clampPage(page, state().pageSize()); }

  private int clampPage(int page, int pageSize) {
    int pages = Math.max(1, (rowCount(state()) + pageSize - 1) / pageSize);
    return Math.max(0, Math.min(page, pages - 1));
  }

  private static int rowCount(TerritoryDetailState state) { return state.rowCount(); }

  private TerritoryDetailState copy(
      Owned territory, List<PlayerSummary> players, TerritoryDetailViewKind view, int scroll,
      int pageSize, String filter, ScreenState screenState, String errorKey, long requestId,
      long playerRevision) {
    return new TerritoryDetailState(territory, players, view, Math.max(0, scroll),
        Math.max(1, pageSize), filter, screenState, errorKey, requestId,
        Math.max(0, playerRevision));
  }
}
