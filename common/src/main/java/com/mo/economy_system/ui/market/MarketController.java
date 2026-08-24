package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Shared market request, paging, filtering, timeout and action state machine. */
public final class MarketController extends AbstractEconomyScreenController<MarketState, MarketEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  public static final int NETWORK_PAGE_SIZE = EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE;
  private final MarketPort port;
  private final Function<String, String> displayNameResolver;
  private final UUID viewerId;
  private final boolean viewerCanModerate;
  private long startedAt;
  private boolean requestInFlight;
  private boolean silentRequestInFlight;

  public MarketController(UUID viewerId, MarketPort port) {
    this(viewerId, false, ignored -> "", port);
  }

  /**
   * Creates a market controller with the target-provided operator capability.  The capability is
   * intentionally client-side presentation state only; the existing remove-sales message remains
   * the authoritative server operation and re-checks the actor permission.
   */
  public MarketController(UUID viewerId, boolean viewerCanModerate, MarketPort port) {
    this(viewerId, viewerCanModerate, ignored -> "", port);
  }

  /**
   * Creates a controller with a target-provided, loader-neutral item-name resolver.  It only
   * enriches rows after the server snapshot is decoded; no network field or server authorization
   * semantics change.
   */
  public MarketController(UUID viewerId, boolean viewerCanModerate,
                          Function<String, String> displayNameResolver, MarketPort port) {
    super(new MarketState(List.of(), 0, NETWORK_PAGE_SIZE, 0, 0, 0, MarketOrderFilter.ALL, "", ScreenState.IDLE,
        null, -1, -1, navigationActions(viewerCanModerate)));
    this.viewerId = viewerId;
    this.viewerCanModerate = viewerCanModerate;
    this.displayNameResolver = java.util.Objects.requireNonNull(displayNameResolver, "displayNameResolver");
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  public UUID viewerId() {
    return viewerId;
  }

  public boolean viewerCanModerate() {
    return viewerCanModerate;
  }

  @Override public void handle(MarketEvent event) {
    if (event instanceof MarketEvent.Initialize value) request(0, value.nowNanos(), false, null);
    else if (event instanceof MarketEvent.Refresh value) request(
        state().page(), value.nowNanos(), true, value.focusTradeId());
    else if (event instanceof MarketEvent.Retry value) request(state().page(), value.nowNanos(), false, null);
    else if (event instanceof MarketEvent.DataLoaded value) loaded(value);
    else if (event instanceof MarketEvent.DataFailed value) failed(value);
    else if (event instanceof MarketEvent.FilterChanged value) filter(value.filter());
    else if (event instanceof MarketEvent.SortChanged value) sort(value.sort());
    else if (event instanceof MarketEvent.QueryChanged value) query(value.query());
    else if (event instanceof MarketEvent.ViewportChanged value) viewport(value.pageSize());
    else if (event instanceof MarketEvent.NextPage) request(
        state().page() + 1, System.nanoTime(), false, null);
    else if (event instanceof MarketEvent.PreviousPage) request(
        state().page() - 1, System.nanoTime(), false, null);
    else if (event instanceof MarketEvent.Scroll value) request(
        state().page() + Integer.signum(value.steps()), System.nanoTime(), false, null);
    else if (event instanceof MarketEvent.ActionClicked value) action(value);
    else if (event instanceof MarketEvent.Tick value) tick(value.nowNanos());
  }

  private void request(int page, long nowNanos, boolean silent, UUID focusTradeId) {
    int nextPage = Math.max(0, page);
    if (!silent && state().screenState() != ScreenState.IDLE && nextPage >= state().totalPages()) return;
    if (silent && (requestInFlight || (state().screenState() != ScreenState.READY
        && state().screenState() != ScreenState.EMPTY))) return;
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("market request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    silentRequestInFlight = silent;
    ScreenState nextState = silent ? state().screenState() : ScreenState.LOADING;
    Set<MarketAction> actions = silent ? state().actions() : navigationActions(viewerCanModerate);
    replaceState(copy(nextPage, nextState, null, id, state().rows(), actions));
    port.requestPage(id, nextPage * NETWORK_PAGE_SIZE, state().filter(), state().sort(),
        state().query(), silent ? focusTradeId : null);
  }

  private void loaded(MarketEvent.DataLoaded event) {
    if (!requestInFlight || event.requestId() != state().requestId()
        || event.revision() < state().revision()) return;
    List<MarketRow> rows = event.orders().stream()
        .map(order -> new MarketRow(order, resolveDisplayName(order.item().itemId()))).toList();
    requestInFlight = false;
    silentRequestInFlight = false;
    Set<MarketAction> actions = EnumSet.of(MarketAction.BUY, MarketAction.REMOVE_SALES,
        MarketAction.DELIVER_DEMAND, MarketAction.CONFIRM_DEMAND, MarketAction.REMOVE_DEMAND,
        MarketAction.BACK, MarketAction.CREATE_SALES, MarketAction.CREATE_DEMAND);
    if (viewerCanModerate) actions.add(MarketAction.ADMIN_REMOVE_SALES);
    replaceState(new MarketState(rows, Math.max(0, event.offset() / NETWORK_PAGE_SIZE), state().pageSize(),
        event.totalMatched(), event.totalSales(), event.totalDemand(), state().filter(), state().sort(), state().query(),
        rows.isEmpty() && event.totalMatched() == 0 ? ScreenState.EMPTY : ScreenState.READY,
        null, -1, event.revision(), actions));
  }

  private String resolveDisplayName(String itemId) {
    try {
      return java.util.Objects.requireNonNullElse(displayNameResolver.apply(itemId), "");
    } catch (RuntimeException ignored) {
      // A missing/invalid client loader must never break the server-backed market page.
      return "";
    }
  }

  private void failed(MarketEvent.DataFailed event) {
    if (!requestInFlight || event.requestId() != state().requestId()) return;
    boolean silent = silentRequestInFlight;
    requestInFlight = false;
    silentRequestInFlight = false;
    if (silent) {
      replaceState(copy(state().page(), state().rows().isEmpty() ? ScreenState.EMPTY : ScreenState.READY,
          null, -1, state().rows(), state().actions()));
      return;
    }
    replaceState(copy(state().page(), ScreenState.ERROR, event.errorKey(), -1, state().rows(),
        Set.of(MarketAction.RETRY, MarketAction.BACK)));
  }

  private void filter(MarketOrderFilter value) {
    replaceState(new MarketState(List.of(), 0, state().pageSize(), 0, 0, 0, value, state().sort(), state().query(),
        ScreenState.IDLE, null, -1, state().revision(), navigationActions(viewerCanModerate)));
    request(0, System.nanoTime(), false, null);
  }

  private void sort(MarketOrderSort value) {
    if (value == null || value == state().sort()) return;
    replaceState(new MarketState(List.of(), 0, state().pageSize(), 0, 0, 0, state().filter(), value,
        state().query(), ScreenState.IDLE, null, -1, state().revision(), navigationActions(viewerCanModerate)));
    request(0, System.nanoTime(), false, null);
  }

  private void query(String value) {
    String next = value == null ? "" : value.trim();
    replaceState(new MarketState(List.of(), 0, state().pageSize(), 0, 0, 0, state().filter(), state().sort(), next,
        ScreenState.IDLE, null, -1, state().revision(), navigationActions(viewerCanModerate)));
    request(0, System.nanoTime(), false, null);
  }

  private void viewport(int value) {
    if (value != NETWORK_PAGE_SIZE) {
      throw new IllegalArgumentException("market viewport must expose the complete network page");
    }
  }

  private void action(MarketEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == MarketAction.BACK) { navigate(new UiNavigation.Route(EconomyUiRoute.HOME)); return; }
    if (event.action() == MarketAction.RETRY) return;
    if (event.action() == MarketAction.CREATE_SALES || event.action() == MarketAction.CREATE_DEMAND) {
      port.create(event.action()); return;
    }
    MarketRow row = state().find(event.tradeId());
    if (row != null && actionAllowed(event.action(), row.order())) port.confirm(event.action(), row);
  }

  private boolean actionAllowed(MarketAction action, MarketOrderSnapshot order) {
    boolean own = viewerId != null && viewerId.equals(order.ownerId());
    return switch (action) {
      case BUY -> order.type() == MarketOrderType.SALES && !own;
      case REMOVE_SALES -> order.type() == MarketOrderType.SALES && own;
      case ADMIN_REMOVE_SALES -> viewerCanModerate && order.type() == MarketOrderType.SALES && !own;
      case DELIVER_DEMAND -> order.type() == MarketOrderType.DEMAND && !order.delivered() && !own;
      case CONFIRM_DEMAND -> order.type() == MarketOrderType.DEMAND && own && order.delivered();
      case REMOVE_DEMAND -> order.type() == MarketOrderType.DEMAND && own && !order.delivered();
      default -> false;
    };
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      boolean silent = silentRequestInFlight;
      requestInFlight = false;
      silentRequestInFlight = false;
      if (silent) {
        replaceState(copy(state().page(), state().rows().isEmpty() ? ScreenState.EMPTY : ScreenState.READY,
            null, -1, state().rows(), state().actions()));
      } else {
        replaceState(copy(state().page(), ScreenState.ERROR, "screen.market.sync_timeout", -1, state().rows(),
            Set.of(MarketAction.RETRY, MarketAction.BACK)));
      }
    }
  }

  private MarketState copy(int page, ScreenState screenState, String error, long requestId,
                           List<MarketRow> rows, Set<MarketAction> actions) {
    return new MarketState(rows, Math.max(0, page), state().pageSize(), state().totalMatched(), state().totalSales(),
        state().totalDemand(), state().filter(), state().sort(), state().query(), screenState, error, requestId,
        state().revision(), actions);
  }

  private static Set<MarketAction> navigationActions(boolean viewerCanModerate) {
    EnumSet<MarketAction> actions = EnumSet.of(MarketAction.BACK, MarketAction.CREATE_SALES,
        MarketAction.CREATE_DEMAND);
    if (viewerCanModerate) actions.add(MarketAction.ADMIN_REMOVE_SALES);
    return Set.copyOf(actions);
  }
}
