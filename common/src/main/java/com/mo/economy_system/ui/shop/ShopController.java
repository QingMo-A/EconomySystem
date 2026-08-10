package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Shared request, filtering, paging, timeout and action state machine. */
public final class ShopController extends AbstractEconomyScreenController<ShopState, ShopEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final ShopPort port;
  private final Function<com.mo.economy_system.common.network.ShopItemSnapshot, String> displayNameResolver;
  private long startedAt;
  private boolean requestInFlight;

  public ShopController(ShopPort port) {
    this(port, ignored -> "");
  }

  /** Target-supplied native hover-name resolver; wire snapshots remain unchanged. */
  public ShopController(ShopPort port,
                        Function<com.mo.economy_system.common.network.ShopItemSnapshot, String> displayNameResolver) {
    super(new ShopState(List.of(), 0, 1, "", ScreenState.IDLE, null, -1, Set.of(ShopAction.BACK)));
    this.port = java.util.Objects.requireNonNull(port, "port");
    this.displayNameResolver = java.util.Objects.requireNonNull(displayNameResolver, "displayNameResolver");
  }

  @Override public void handle(ShopEvent event) {
    if (event instanceof ShopEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof ShopEvent.Retry value) request(value.nowNanos());
    else if (event instanceof ShopEvent.DataLoaded value) loaded(value);
    else if (event instanceof ShopEvent.DataFailed value) failed(value);
    else if (event instanceof ShopEvent.FilterChanged value) filter(value.value());
    else if (event instanceof ShopEvent.ViewportChanged value) pageSize(value.pageSize());
    else if (event instanceof ShopEvent.NextPage) page(1);
    else if (event instanceof ShopEvent.PreviousPage) page(-1);
    else if (event instanceof ShopEvent.Scroll value) page(Integer.signum(value.steps()));
    else if (event instanceof ShopEvent.ActionClicked value) action(value);
    else if (event instanceof ShopEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("shop request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(new ShopState(state().rows(), 0, state().pageSize(), state().filter(), ScreenState.LOADING,
        null, id, Set.of(ShopAction.BACK)));
    port.requestCatalog(id);
  }

  private void loaded(ShopEvent.DataLoaded event) {
    if (!requestInFlight || event.requestId() != state().requestId()
        || state().screenState() != ScreenState.LOADING) return;
    List<ShopRow> rows = event.items().stream()
        .map(item -> new ShopRow(item, displayNameResolver.apply(item))).toList();
    requestInFlight = false;
    replaceState(new ShopState(rows, 0, state().pageSize(), state().filter(),
        rows.isEmpty() ? ScreenState.EMPTY : ScreenState.READY, null, -1,
        Set.of(ShopAction.BUY, ShopAction.BACK)));
  }

  private void failed(ShopEvent.DataFailed event) {
    if (!requestInFlight || event.requestId() != state().requestId()) return;
    requestInFlight = false;
    replaceState(new ShopState(state().rows(), state().page(), state().pageSize(), state().filter(),
        ScreenState.ERROR, event.errorKey(), -1, Set.of(ShopAction.RETRY, ShopAction.BACK)));
  }

  private void filter(String value) {
    int page = Math.min(state().page(), Math.max(0, totalPages(value, state().pageSize()) - 1));
    replaceState(new ShopState(state().rows(), page, state().pageSize(), value, state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void pageSize(int value) {
    if (value == state().pageSize()) return;
    int page = Math.min(state().page(), Math.max(0, (state().filteredRows().size() + value - 1) / value - 1));
    replaceState(new ShopState(state().rows(), page, value, state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void page(int delta) {
    int next = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
    replaceState(new ShopState(state().rows(), next, state().pageSize(), state().filter(), state().screenState(),
        state().errorKey(), state().requestId(), state().actions()));
  }

  private void action(ShopEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == ShopAction.BACK) {
      navigate(new UiNavigation.Route(com.mo.economy_system.common.client.ui.EconomyUiRoute.HOME));
      return;
    }
    if (event.action() == ShopAction.RETRY) return;
    ShopRow row = state().find(event.shopItemId());
    if (row != null && event.action() == ShopAction.BUY) port.confirm(row);
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      replaceState(new ShopState(state().rows(), state().page(), state().pageSize(), state().filter(),
          ScreenState.ERROR, "screen.shop.sync_timeout", -1, Set.of(ShopAction.RETRY, ShopAction.BACK)));
    }
  }

  private int totalPages(String value, int pageSize) {
    String needle = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    int count = (int) state().rows().stream().filter(row -> {
      var item = row.item();
      return needle.isEmpty() || item.shopItemId().toLowerCase(java.util.Locale.ROOT).contains(needle)
          || item.itemId().toLowerCase(java.util.Locale.ROOT).contains(needle)
          || item.description().toLowerCase(java.util.Locale.ROOT).contains(needle)
          || row.displayName().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }).count();
    return Math.max(1, (count + pageSize - 1) / pageSize);
  }
}
