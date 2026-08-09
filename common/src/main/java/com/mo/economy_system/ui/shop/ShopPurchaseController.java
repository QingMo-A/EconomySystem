package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Set;

/** Common validation for the shop quantity and total-price confirmation flow. */
public final class ShopPurchaseController extends AbstractEconomyScreenController<ShopPurchaseState, ShopPurchaseEvent> {
  private final ShopPurchasePort port;

  public ShopPurchaseController(ShopRow row, ShopPurchasePort port) {
    super(state(row, 1, Math.max(0, port.availableQuantity(row)), ScreenState.READY, null));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(ShopPurchaseEvent event) {
    if (event instanceof ShopPurchaseEvent.QuantityChanged value) {
      replaceState(state(state().row(), Math.max(0, value.quantity()), state().availableQuantity(), ScreenState.READY, null));
    } else if (event instanceof ShopPurchaseEvent.ActionClicked value && value.action() != null && state().can(value.action())) {
      if (value.action() == ShopPurchaseAction.BACK) navigate(new UiNavigation.Back()); else submit();
    }
  }

  private void submit() {
    if (state().quantity() < 1) {
      replaceState(state(state().row(), state().quantity(), state().availableQuantity(), ScreenState.ERROR, "screen.shop.purchase.invalid_quantity"));
      return;
    }
    if (state().quantity() > state().availableQuantity()) {
      replaceState(state(state().row(), state().quantity(), state().availableQuantity(), ScreenState.ERROR, "screen.shop.purchase.inventory_full"));
      return;
    }
    if (state().totalPrice() == Long.MAX_VALUE) {
      replaceState(state(state().row(), state().quantity(), state().availableQuantity(), ScreenState.ERROR, "screen.shop.purchase.price_overflow"));
      return;
    }
    replaceState(new ShopPurchaseState(state().row(), state().quantity(), state().totalPrice(),
        state().availableQuantity(), state().screenState(), state().errorKey(), Set.of()));
    port.submit(state().row(), state().quantity());
    navigate(new UiNavigation.Back());
  }

  private static ShopPurchaseState state(ShopRow row, int quantity, int available, ScreenState screenState, String error) {
    long price;
    try { price = Math.multiplyExact((long) row.item().currentPrice(), quantity); }
    catch (ArithmeticException ignored) { price = Long.MAX_VALUE; }
    return new ShopPurchaseState(row, quantity, price, available, screenState, error,
        Set.of(ShopPurchaseAction.CONFIRM, ShopPurchaseAction.BACK));
  }
}
