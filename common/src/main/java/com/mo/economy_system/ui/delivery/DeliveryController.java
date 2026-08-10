package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Shared delivery-box request, filtering, paging, timeout and claim logic. */
public final class DeliveryController extends AbstractEconomyScreenController<DeliveryState, DeliveryEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final DeliveryPort port;
  private final Function<com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot, String> displayNameResolver;
  private long startedAt;
  private boolean requestInFlight;

  public DeliveryController(DeliveryPort port) {
    this(port, ignored -> "");
  }

  public DeliveryController(DeliveryPort port,
                             Function<com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot, String> displayNameResolver) {
    super(new DeliveryState(List.of(), 0, 1, "", ScreenState.IDLE, null, -1,
        Set.of(DeliveryAction.BACK)));
    this.port = java.util.Objects.requireNonNull(port, "port");
    this.displayNameResolver = java.util.Objects.requireNonNull(displayNameResolver, "displayNameResolver");
  }

  @Override public void handle(DeliveryEvent event) {
    if (event instanceof DeliveryEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof DeliveryEvent.Retry value) request(value.nowNanos());
    else if (event instanceof DeliveryEvent.DataLoaded value) loaded(value);
    else if (event instanceof DeliveryEvent.DataFailed value) failed(value);
    else if (event instanceof DeliveryEvent.FilterChanged value) filter(value.value());
    else if (event instanceof DeliveryEvent.ViewportChanged value) pageSize(value.pageSize());
    else if (event instanceof DeliveryEvent.NextPage) page(1);
    else if (event instanceof DeliveryEvent.PreviousPage) page(-1);
    else if (event instanceof DeliveryEvent.Scroll value) page(Integer.signum(value.steps()));
    else if (event instanceof DeliveryEvent.ActionClicked value) action(value);
    else if (event instanceof DeliveryEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("delivery request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(new DeliveryState(state().rows(), 0, state().pageSize(), state().filter(),
        ScreenState.LOADING, null, id, Set.of(DeliveryAction.BACK)));
    port.requestData(id);
  }

  private void loaded(DeliveryEvent.DataLoaded event) {
    if (!requestInFlight || event.requestId() != state().requestId()
        || state().screenState() != ScreenState.LOADING) return;
    List<DeliveryRow> rows = event.entries().stream()
        .map(entry -> new DeliveryRow(entry, displayNameResolver.apply(entry))).toList();
    requestInFlight = false;
    replaceState(new DeliveryState(rows, 0, state().pageSize(), state().filter(),
        rows.isEmpty() ? ScreenState.EMPTY : ScreenState.READY, null, event.requestId(),
        Set.of(DeliveryAction.CLAIM, DeliveryAction.BACK)));
  }

  private void failed(DeliveryEvent.DataFailed event) {
    if (!requestInFlight || event.requestId() != state().requestId()) return;
    requestInFlight = false;
    replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
        ScreenState.ERROR, event.errorKey(), -1, Set.of(DeliveryAction.RETRY, DeliveryAction.BACK)));
  }

  private void filter(String value) {
    String next = value == null ? "" : value;
    int page = Math.min(state().page(), Math.max(0, totalPages(next, state().pageSize()) - 1));
    replaceState(new DeliveryState(state().rows(), page, state().pageSize(), next,
        state().screenState(), state().errorKey(), state().requestId(), state().actions()));
  }

  private void pageSize(int value) {
    if (value < 1 || value == state().pageSize()) return;
    int page = Math.min(state().page(), Math.max(0, (state().filteredRows().size() + value - 1) / value - 1));
    replaceState(new DeliveryState(state().rows(), page, value, state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void page(int delta) {
    int next = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
    replaceState(new DeliveryState(state().rows(), next, state().pageSize(), state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void action(DeliveryEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == DeliveryAction.BACK) {
      navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
      return;
    }
    if (event.action() == DeliveryAction.RETRY) {
      request(event.nowNanos());
      return;
    }
    DeliveryRow row = state().find(event.entryId());
    if (row == null || state().requestId() < 0) return;
    requestInFlight = true;
    startedAt = event.nowNanos();
    replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
        ScreenState.LOADING, null, state().requestId(), Set.of(DeliveryAction.BACK)));
    port.claim(row.entryId(), state().requestId());
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
          ScreenState.ERROR, "screen.delivery_box.sync_timeout", -1,
          Set.of(DeliveryAction.RETRY, DeliveryAction.BACK)));
    }
  }

  private int totalPages(String value, int pageSize) {
    String needle = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    int count = (int) state().rows().stream().filter(row -> {
      var entry = row.entry();
      return needle.isEmpty() || entry.entryId().toString().contains(needle)
          || entry.source().toLowerCase(java.util.Locale.ROOT).contains(needle)
          || entry.item().itemId().toLowerCase(java.util.Locale.ROOT).contains(needle)
          || row.displayName().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }).count();
    return Math.max(1, (count + pageSize - 1) / pageSize);
  }
}
