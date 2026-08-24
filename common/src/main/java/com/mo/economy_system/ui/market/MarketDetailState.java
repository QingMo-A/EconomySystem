package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable state for the market's right-side inline detail/action pane. */
public record MarketDetailState(
    MarketRow row,
    MarketAction primaryAction,
    MarketAction secondaryAction,
    int quantity,
    long amount,
    int balance,
    int inventoryCapacity,
    int matchingItems,
    int receivableHeadroom,
    boolean partialSupported,
    ScreenState screenState,
    String errorKey,
    Set<MarketDetailAction> actions) {

  public MarketDetailState {
    row = Objects.requireNonNull(row, "row");
    if (quantity < 0 || amount < 0 || balance < 0 || inventoryCapacity < 0
        || matchingItems < 0 || receivableHeadroom < 0) {
      throw new IllegalArgumentException("invalid market detail state");
    }
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public boolean can(MarketDetailAction action) {
    return actions.contains(action);
  }

  public boolean quantityMode() {
    return primaryAction == MarketAction.BUY || primaryAction == MarketAction.DELIVER_DEMAND;
  }

  public int remainingQuantity() {
    return row.order().quantity();
  }

  public int exactUnitPrice() {
    int quantity = row.order().quantity();
    int total = row.order().totalPrice();
    return quantity > 0 && total > 0 && total % quantity == 0 ? total / quantity : 0;
  }
}
