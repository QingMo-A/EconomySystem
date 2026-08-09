package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Shared request, filter, paging and action state machine for the territory list. */
public final class TerritoryListController
    extends AbstractEconomyScreenController<TerritoryListState, TerritoryListEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;

  private final TerritoryListPort port;
  private long startedAt;
  private boolean requestInFlight;

  public TerritoryListController(TerritoryListPort port) {
    super(new TerritoryListState(List.of(), 0, 1, "", ScreenState.IDLE, null, -1,
        Set.of(TerritoryListAction.BACK)));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(TerritoryListEvent event) {
    if (event instanceof TerritoryListEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof TerritoryListEvent.Retry value) request(value.nowNanos());
    else if (event instanceof TerritoryListEvent.DataLoaded value) loaded(value);
    else if (event instanceof TerritoryListEvent.DataFailed value) failed(value);
    else if (event instanceof TerritoryListEvent.FilterChanged value) filter(value.value());
    else if (event instanceof TerritoryListEvent.ViewportChanged value) pageSize(value.pageSize());
    else if (event instanceof TerritoryListEvent.NextPage) changePage(1);
    else if (event instanceof TerritoryListEvent.PreviousPage) changePage(-1);
    else if (event instanceof TerritoryListEvent.Scroll value) changePage(Integer.signum(value.steps()));
    else if (event instanceof TerritoryListEvent.ActionClicked value) action(value);
    else if (event instanceof TerritoryListEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("territory list request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(copy(0, state().filter(), ScreenState.LOADING, null, id, state().rows(),
        Set.of(TerritoryListAction.BACK)));
    port.requestTerritories(id);
  }

  private void loaded(TerritoryListEvent.DataLoaded event) {
    if (!requestInFlight || state().requestId() != event.requestId()
        || state().screenState() != ScreenState.LOADING) return;
    Map<UUID, TerritoryListRow> rows = new LinkedHashMap<>();
    for (Owned value : event.owned()) rows.putIfAbsent(value.summary().territoryId(), TerritoryListRow.owned(value));
    for (Summary value : event.authorized()) rows.putIfAbsent(value.territoryId(), TerritoryListRow.authorized(value));
    List<TerritoryListRow> next = List.copyOf(new ArrayList<>(rows.values()));
    requestInFlight = false;
    replaceState(copy(0, state().filter(), next.isEmpty() ? ScreenState.EMPTY : ScreenState.READY,
        null, -1, next, Set.of(TerritoryListAction.TELEPORT, TerritoryListAction.MANAGE,
            TerritoryListAction.BACK)));
  }

  private void failed(TerritoryListEvent.DataFailed event) {
    if (!requestInFlight || state().requestId() != event.requestId()) return;
    requestInFlight = false;
    replaceState(copy(state().page(), state().filter(), ScreenState.ERROR, event.errorKey(), -1,
        state().rows(), Set.of(TerritoryListAction.RETRY, TerritoryListAction.BACK)));
  }

  private void filter(String value) {
    replaceState(copy(0, value == null ? "" : value, state().screenState(), state().errorKey(),
        state().requestId(), state().rows(), state().actions()));
  }

  private void pageSize(int value) {
    if (value == state().pageSize()) return;
    int page = Math.min(state().page(), Math.max(0, totalPages(state().filteredRows().size(), value) - 1));
    replaceState(new TerritoryListState(state().rows(), page, value, state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void changePage(int delta) {
    int page = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
    replaceState(new TerritoryListState(state().rows(), page, state().pageSize(), state().filter(),
        state().screenState(), state().errorKey(), state().requestId(), state().actions()));
  }

  private void action(TerritoryListEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == TerritoryListAction.BACK) {
      navigate(new UiNavigation.Route(com.mo.economy_system.common.client.ui.EconomyUiRoute.HOME));
      return;
    }
    if (event.action() == TerritoryListAction.RETRY) return;
    TerritoryListRow row = state().find(event.territoryId());
    if (row == null || (event.action() == TerritoryListAction.MANAGE && !row.owned())) return;
    port.submit(event.action(), row);
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      replaceState(copy(state().page(), state().filter(), ScreenState.ERROR,
          "screen.territory.list.sync_timeout", -1, state().rows(),
          Set.of(TerritoryListAction.RETRY, TerritoryListAction.BACK)));
    }
  }

  private TerritoryListState copy(int page, String filter, ScreenState status, String error,
                                  long requestId, List<TerritoryListRow> rows,
                                  Set<TerritoryListAction> actions) {
    return new TerritoryListState(rows, page, Math.max(1, state().pageSize()), filter, status,
        error, requestId, actions);
  }

  private static int totalPages(int size, int pageSize) {
    return Math.max(1, (size + pageSize - 1) / pageSize);
  }
}
