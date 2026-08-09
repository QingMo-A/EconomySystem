package com.mo.economy_system.ui.balance;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Set;

/** Common balance-log paging, category, scroll, stale-response and timeout logic. */
public final class BalanceLogController extends AbstractEconomyScreenController<BalanceLogState, BalanceLogEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final BalanceLogPort port;
  private long startedAt;
  private boolean requestInFlight;

  public BalanceLogController(BalanceLogPort port) {
    super(new BalanceLogState(List.of(), BalanceLogRequestMessage.ALL_CATEGORIES, 0,
        BalanceLogRequestMessage.DEFAULT_LIMIT, 0, 0, 8, ScreenState.IDLE, null, -1,
        Set.of(BalanceLogAction.BACK)));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(BalanceLogEvent event) {
    if (event instanceof BalanceLogEvent.Initialize value) request(0, value.nowNanos());
    else if (event instanceof BalanceLogEvent.Retry value) request(state().offset(), value.nowNanos());
    else if (event instanceof BalanceLogEvent.DataLoaded value) loaded(value);
    else if (event instanceof BalanceLogEvent.DataFailed value) failed(value);
    else if (event instanceof BalanceLogEvent.CategoryChanged value) category(value.category());
    else if (event instanceof BalanceLogEvent.ViewportChanged value) viewport(value.visibleRows());
    else if (event instanceof BalanceLogEvent.NextPage) nextPage();
    else if (event instanceof BalanceLogEvent.PreviousPage) previousPage();
    else if (event instanceof BalanceLogEvent.Scroll value) scroll(value.steps());
    else if (event instanceof BalanceLogEvent.ActionClicked value) action(value);
    else if (event instanceof BalanceLogEvent.Tick value) tick(value.nowNanos());
  }

  private void request(int offset, long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("balance-log request id must be non-negative");
    int nextOffset = Math.max(0, offset);
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(new BalanceLogState(state().rows(), state().category(), nextOffset, state().limit(),
        state().total(), 0, state().visibleRows(), ScreenState.LOADING, null, id,
        Set.of(BalanceLogAction.BACK)));
    port.requestPage(id, state().category(), nextOffset, state().limit());
  }

  private void loaded(BalanceLogEvent.DataLoaded event) {
    if (!requestInFlight || event.requestId() != state().requestId()
        || state().screenState() != ScreenState.LOADING
        || !event.category().equals(state().category()) || event.offset() != state().offset()) return;
    List<BalanceLogRow> rows = event.entries().stream().map(BalanceLogRow::new).toList();
    requestInFlight = false;
    int limit = Math.max(1, event.limit());
    replaceState(new BalanceLogState(rows, event.category(), Math.max(0, event.offset()), limit,
        Math.max(0, event.total()), 0, state().visibleRows(),
        rows.isEmpty() && event.total() == 0 ? ScreenState.EMPTY : ScreenState.READY, null, -1,
        Set.of(BalanceLogAction.BACK)));
  }

  private void failed(BalanceLogEvent.DataFailed event) {
    if (!requestInFlight || event.requestId() != state().requestId()) return;
    requestInFlight = false;
    replaceState(new BalanceLogState(state().rows(), state().category(), state().offset(), state().limit(),
        state().total(), state().scroll(), state().visibleRows(), ScreenState.ERROR,
        event.errorKey(), -1, Set.of(BalanceLogAction.RETRY, BalanceLogAction.BACK)));
  }

  private void category(String value) {
    String next = BalanceLogState.CATEGORIES.contains(value) ? value : BalanceLogRequestMessage.ALL_CATEGORIES;
    replaceState(new BalanceLogState(List.of(), next, 0, state().limit(), 0, 0, state().visibleRows(),
        ScreenState.IDLE, null, -1, Set.of(BalanceLogAction.BACK)));
    request(0, System.nanoTime());
  }

  private void viewport(int value) {
    if (value < 1 || value == state().visibleRows()) return;
    int maxScroll = Math.max(0, state().rows().size() - value);
    replaceState(new BalanceLogState(state().rows(), state().category(), state().offset(), state().limit(),
        state().total(), Math.min(state().scroll(), maxScroll), value, state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void nextPage() {
    if (requestInFlight || !state().hasNextPage()) return;
    request(state().offset() + state().limit(), System.nanoTime());
  }

  private void previousPage() {
    if (requestInFlight || !state().hasPreviousPage()) return;
    request(Math.max(0, state().offset() - state().limit()), System.nanoTime());
  }

  private void scroll(int delta) {
    int max = Math.max(0, state().rows().size() - state().visibleRows());
    int next = Math.max(0, Math.min(max, state().scroll() + delta));
    replaceState(new BalanceLogState(state().rows(), state().category(), state().offset(), state().limit(),
        state().total(), next, state().visibleRows(), state().screenState(), state().errorKey(),
        state().requestId(), state().actions()));
  }

  private void action(BalanceLogEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == BalanceLogAction.BACK) navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
    else if (event.action() == BalanceLogAction.RETRY) request(state().offset(), event.nowNanos());
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      replaceState(new BalanceLogState(state().rows(), state().category(), state().offset(), state().limit(),
          state().total(), state().scroll(), state().visibleRows(), ScreenState.ERROR,
          "screen.balance_log.sync_timeout", -1,
          Set.of(BalanceLogAction.RETRY, BalanceLogAction.BACK)));
    }
  }
}
