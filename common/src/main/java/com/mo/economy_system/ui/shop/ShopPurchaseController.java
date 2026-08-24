package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Set;

/** Common validation for both the inline shop purchase pane and the legacy standalone flow. */
public final class ShopPurchaseController
    extends AbstractEconomyScreenController<ShopPurchaseState, ShopPurchaseEvent> {
  private final ShopPurchasePort port;
  private final boolean closeAfterSubmit;

  public ShopPurchaseController(ShopRow row, ShopPurchasePort port) {
    this(row, port, true);
  }

  /**
   * @param closeAfterSubmit true for the legacy standalone dialog; false for the inline 3/4 + 1/4 shop.
   */
  public ShopPurchaseController(ShopRow row, ShopPurchasePort port, boolean closeAfterSubmit) {
    super(state(row, 1, Math.max(0, port.availableQuantity(row)),
        Math.max(0, port.currentBalance()), ScreenState.READY, null));
    this.port = java.util.Objects.requireNonNull(port, "port");
    this.closeAfterSubmit = closeAfterSubmit;
  }

  @Override public void handle(ShopPurchaseEvent event) {
    if (event instanceof ShopPurchaseEvent.QuantityChanged value) {
      replaceState(state(state().row(), Math.max(0, value.quantity()),
          state().availableQuantity(), state().balance(), ScreenState.READY, null));
    } else if (event instanceof ShopPurchaseEvent.FactsChanged value) {
      replaceState(state(state().row(), state().quantity(), Math.max(0, value.availableQuantity()),
          Math.max(0, value.balance()), ScreenState.READY, null));
    } else if (event instanceof ShopPurchaseEvent.ItemRefreshed value) {
      replaceState(state(value.row(), state().quantity(), state().availableQuantity(),
          state().balance(), ScreenState.READY, null));
    } else if (event instanceof ShopPurchaseEvent.ActionClicked value
        && value.action() != null && state().can(value.action())) {
      if (value.action() == ShopPurchaseAction.BACK) {
        navigate(new UiNavigation.Back());
      } else {
        submit();
      }
    }
  }

  private void submit() {
    // Re-evaluate target facts immediately before emitting the purchase intent. This remains only
    // a client preflight: ShopPurchaseService performs authoritative balance/inventory checks.
    int available = Math.max(0, port.availableQuantity(state().row()));
    int balance = Math.max(0, port.currentBalance());
    ShopPurchaseState refreshed = state(state().row(), state().quantity(), available, balance,
        ScreenState.READY, null);
    replaceState(refreshed);
    if (!refreshed.can(ShopPurchaseAction.CONFIRM)) return;

    port.submit(refreshed.row(), refreshed.quantity());
    if (closeAfterSubmit) {
      replaceState(new ShopPurchaseState(refreshed.row(), refreshed.quantity(), refreshed.totalPrice(),
          refreshed.availableQuantity(), refreshed.balance(), refreshed.screenState(), refreshed.errorKey(), Set.of()));
      navigate(new UiNavigation.Back());
    } else {
      // Keep the pane usable for repeated purchases; target-side facts will refresh on the next tick.
      replaceState(state(refreshed.row(), refreshed.quantity(), refreshed.availableQuantity(),
          refreshed.balance(), ScreenState.READY, null));
    }
  }

  private static ShopPurchaseState state(
      ShopRow row, int quantity, int available, int balance, ScreenState screenState, String error) {
    long price;
    try {
      price = Math.multiplyExact((long) row.item().currentPrice(), quantity);
    } catch (ArithmeticException ignored) {
      price = Long.MAX_VALUE;
    }

    ScreenState resolvedState = screenState;
    String resolvedError = error;
    if (screenState == ScreenState.READY) {
      if (quantity < 1) {
        resolvedState = ScreenState.ERROR;
        resolvedError = "screen.shop.purchase.invalid_quantity";
      } else if (quantity > available) {
        resolvedState = ScreenState.ERROR;
        resolvedError = "screen.shop.purchase.inventory_full";
      } else if (price == Long.MAX_VALUE) {
        resolvedState = ScreenState.ERROR;
        resolvedError = "screen.shop.purchase.price_overflow";
      } else if (price > balance) {
        resolvedState = ScreenState.ERROR;
        resolvedError = "screen.shop.purchase.insufficient_balance";
      }
    }

    Set<ShopPurchaseAction> actions = resolvedState == ScreenState.ERROR
        ? Set.of(ShopPurchaseAction.BACK)
        : Set.of(ShopPurchaseAction.CONFIRM, ShopPurchaseAction.BACK);
    return new ShopPurchaseState(row, quantity, price, available, balance,
        resolvedState, resolvedError, actions);
  }
}
