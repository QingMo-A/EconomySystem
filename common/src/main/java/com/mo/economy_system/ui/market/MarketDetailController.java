package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Shared state/validation for the market's inline detail pane. */
public final class MarketDetailController
    extends AbstractEconomyScreenController<MarketDetailState, MarketDetailEvent> {
  private final UUID viewerId;
  private final boolean viewerCanModerate;
  private final MarketDetailPort port;

  public MarketDetailController(
      MarketRow row, UUID viewerId, boolean viewerCanModerate, MarketDetailPort port) {
    super(build(row, viewerId, viewerCanModerate, 1, 0, 0, 0, 0));
    this.viewerId = viewerId;
    this.viewerCanModerate = viewerCanModerate;
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(MarketDetailEvent event) {
    if (event instanceof MarketDetailEvent.QuantityChanged value) {
      if (isInvalidated()) return;
      replace(value.quantity(), state().balance(), state().inventoryCapacity(), state().matchingItems(),
          state().receivableHeadroom(), state().row());
    } else if (event instanceof MarketDetailEvent.FactsChanged value) {
      if (isInvalidated()) return;
      replace(state().quantity(), value.balance(), value.inventoryCapacity(), value.matchingItems(),
          value.receivableHeadroom(), state().row());
    } else if (event instanceof MarketDetailEvent.RowRefreshed value) {
      if (!value.row().order().tradeId().equals(state().row().order().tradeId())) return;
      int quantity = state().quantity();
      if (quantity > value.row().order().quantity()) quantity = value.row().order().quantity();
      replace(quantity, state().balance(), state().inventoryCapacity(), state().matchingItems(),
          state().receivableHeadroom(), value.row());
    } else if (event instanceof MarketDetailEvent.OrderInvalidated) {
      invalidate();
    } else if (event instanceof MarketDetailEvent.ActionClicked value) {
      action(value.action());
    }
  }

  private boolean isInvalidated() {
    return "screen.market.detail.order_invalidated".equals(state().errorKey());
  }

  private void invalidate() {
    replaceState(new MarketDetailState(
        state().row(), null, null, state().quantity(), state().amount(), state().balance(),
        state().inventoryCapacity(), state().matchingItems(), state().receivableHeadroom(),
        state().partialSupported(), ScreenState.ERROR, "screen.market.detail.order_invalidated", Set.of()));
  }

  private void action(MarketDetailAction action) {
    if (action == null || !state().can(action)) return;
    switch (action) {
      case DECREMENT -> replace(Math.max(1, state().quantity() - 1), state().balance(),
          state().inventoryCapacity(), state().matchingItems(), state().receivableHeadroom(), state().row());
      case INCREMENT -> replace(Math.min(state().remainingQuantity(), state().quantity() + 1), state().balance(),
          state().inventoryCapacity(), state().matchingItems(), state().receivableHeadroom(), state().row());
      case SELECT_ALL -> replace(selectAllQuantity(), state().balance(), state().inventoryCapacity(),
          state().matchingItems(), state().receivableHeadroom(), state().row());
      case SUBMIT_PRIMARY -> {
        if (state().primaryAction() != null) {
          port.submit(state().primaryAction(), state().row(), state().quantityMode() ? state().quantity() : 0);
        }
      }
      case SUBMIT_SECONDARY -> {
        if (state().secondaryAction() != null) port.submit(state().secondaryAction(), state().row(), 0);
      }
    }
  }

  private int selectAllQuantity() {
    if (!state().quantityMode()) return 0;
    int remaining = state().remainingQuantity();
    int unit = state().exactUnitPrice();
    if (!state().partialSupported() || unit <= 0) return remaining;
    if (state().primaryAction() == MarketAction.BUY) {
      int affordable = unit <= 0 ? 0 : state().balance() / unit;
      int safe = Math.min(remaining, Math.min(state().inventoryCapacity(), affordable));
      return Math.max(1, safe);
    }
    if (state().primaryAction() == MarketAction.DELIVER_DEMAND) {
      int receivable = unit <= 0 ? 0 : state().receivableHeadroom() / unit;
      int safe = Math.min(remaining, Math.min(state().matchingItems(), receivable));
      return Math.max(1, safe);
    }
    return remaining;
  }

  private void replace(
      int quantity, int balance, int inventoryCapacity, int matchingItems,
      int receivableHeadroom, MarketRow row) {
    replaceState(build(row, viewerId, viewerCanModerate, quantity, balance,
        inventoryCapacity, matchingItems, receivableHeadroom));
  }

  private static MarketDetailState build(
      MarketRow row,
      UUID viewerId,
      boolean viewerCanModerate,
      int requestedQuantity,
      int balance,
      int inventoryCapacity,
      int matchingItems,
      int receivableHeadroom) {
    var order = row.order();
    boolean own = viewerId != null && viewerId.equals(order.ownerId());
    MarketAction primary = primaryAction(order.type(), order.delivered(), own);
    MarketAction secondary = viewerCanModerate && !own && order.type() == MarketOrderType.SALES
        ? MarketAction.ADMIN_REMOVE_SALES : null;
    boolean quantityMode = primary == MarketAction.BUY || primary == MarketAction.DELIVER_DEMAND;
    boolean partialSupported = order.quantity() > 0 && order.totalPrice() > 0
        && order.totalPrice() % order.quantity() == 0;

    int quantity = quantityMode ? Math.max(0, Math.min(requestedQuantity, order.quantity())) : 0;
    if (quantityMode && !partialSupported) quantity = order.quantity();
    int unitPrice = partialSupported ? order.totalPrice() / order.quantity() : 0;
    long amount = quantityMode
        ? partialSupported ? (long) unitPrice * quantity : order.totalPrice()
        : order.totalPrice();

    String error = null;
    if (quantityMode) {
      if (quantity < 1 || quantity > order.quantity()) {
        error = "screen.market.detail.invalid_quantity";
      } else if (!partialSupported && quantity != order.quantity()) {
        error = "screen.market.detail.whole_order_only";
      } else if (amount > Integer.MAX_VALUE) {
        error = "screen.market.detail.price_overflow";
      } else if (primary == MarketAction.BUY && amount > balance) {
        error = "screen.market.detail.insufficient_balance";
      } else if (primary == MarketAction.BUY && quantity > inventoryCapacity) {
        error = "screen.market.detail.inventory_full";
      } else if (primary == MarketAction.DELIVER_DEMAND && quantity > matchingItems) {
        error = "screen.market.detail.insufficient_items";
      } else if (primary == MarketAction.DELIVER_DEMAND && amount > receivableHeadroom) {
        error = "screen.market.detail.balance_limit";
      }
    }

    EnumSet<MarketDetailAction> actions = EnumSet.noneOf(MarketDetailAction.class);
    if (quantityMode && partialSupported) {
      actions.add(MarketDetailAction.DECREMENT);
      actions.add(MarketDetailAction.INCREMENT);
      actions.add(MarketDetailAction.SELECT_ALL);
    }
    if (primary != null && error == null) actions.add(MarketDetailAction.SUBMIT_PRIMARY);
    if (secondary != null) actions.add(MarketDetailAction.SUBMIT_SECONDARY);

    return new MarketDetailState(row, primary, secondary, quantity, amount,
        Math.max(0, balance), Math.max(0, inventoryCapacity), Math.max(0, matchingItems),
        Math.max(0, receivableHeadroom), partialSupported,
        error == null ? ScreenState.READY : ScreenState.ERROR, error, Set.copyOf(actions));
  }

  private static MarketAction primaryAction(MarketOrderType type, boolean delivered, boolean own) {
    if (type == MarketOrderType.SALES) return own ? MarketAction.REMOVE_SALES : MarketAction.BUY;
    if (own) return delivered ? MarketAction.CONFIRM_DEMAND : MarketAction.REMOVE_DEMAND;
    return delivered ? null : MarketAction.DELIVER_DEMAND;
  }
}
