package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable purchase form state owned by common UI code. */
public record ShopPurchaseState(ShopRow row, int quantity, long totalPrice, int availableQuantity,
                                ScreenState screenState, String errorKey, Set<ShopPurchaseAction> actions) {
  public ShopPurchaseState {
    row = Objects.requireNonNull(row, "row");
    if (quantity < 0 || totalPrice < 0 || availableQuantity < 0) throw new IllegalArgumentException("invalid shop purchase state");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }
  public boolean can(ShopPurchaseAction action) { return actions.contains(action); }
}
