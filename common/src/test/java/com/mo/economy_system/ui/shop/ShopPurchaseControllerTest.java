package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import org.junit.jupiter.api.Test;

class ShopPurchaseControllerTest {
  @Test void quantityChangedImmediatelyDisablesConfirmWhenInventoryIsFull() {
    FakePort port = new FakePort();
    ShopPurchaseController controller = new ShopPurchaseController(row(), port);

    controller.handle(new ShopPurchaseEvent.QuantityChanged(3));

    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.inventory_full", controller.state().errorKey());
    assertFalse(controller.state().can(ShopPurchaseAction.CONFIRM));
    assertTrue(controller.state().can(ShopPurchaseAction.BACK));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(0, port.calls);
  }

  @Test void quantityChangedInvalidThenValidRestoresReadyAndConfirm() {
    FakePort port = new FakePort();
    ShopPurchaseController controller = new ShopPurchaseController(row(), port);

    controller.handle(new ShopPurchaseEvent.QuantityChanged(0));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.invalid_quantity", controller.state().errorKey());
    assertFalse(controller.state().can(ShopPurchaseAction.CONFIRM));

    controller.handle(new ShopPurchaseEvent.QuantityChanged(2));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(null, controller.state().errorKey());
    assertTrue(controller.state().can(ShopPurchaseAction.CONFIRM));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(1, port.calls);
    assertEquals(2, port.quantity);
  }

  @Test void typedQuantityOverCapacityNeverReachesThePort() {
    FakePort port = new FakePort();
    ShopPurchaseController controller = new ShopPurchaseController(row(), port);

    controller.handle(new ShopPurchaseEvent.QuantityChanged(3));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));

    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.inventory_full", controller.state().errorKey());
    assertEquals(0, port.calls);
  }

  @Test void validatesQuantityAndInventoryBeforeSubmit() {
    FakePort port = new FakePort();
    ShopPurchaseController controller = new ShopPurchaseController(row(), port);
    controller.handle(new ShopPurchaseEvent.QuantityChanged(5));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.inventory_full", controller.state().errorKey());
    controller.handle(new ShopPurchaseEvent.QuantityChanged(2));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(2, port.quantity);
    assertEquals(14, controller.state().totalPrice());
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(1, port.calls);
  }

  @Test void insufficientBalanceDisablesConfirmBeforeNetworkSubmit() {
    FakePort port = new FakePort();
    port.balance = 10;
    ShopPurchaseController controller = new ShopPurchaseController(row(), port, false);

    controller.handle(new ShopPurchaseEvent.QuantityChanged(2));

    assertEquals(14, controller.state().totalPrice());
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.insufficient_balance", controller.state().errorKey());
    assertFalse(controller.state().can(ShopPurchaseAction.CONFIRM));
    controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));
    assertEquals(0, port.calls);

    port.balance = 20;
    controller.handle(new ShopPurchaseEvent.FactsChanged(2, 20));
    assertTrue(controller.state().can(ShopPurchaseAction.CONFIRM));
  }

  @Test void livePriceRefreshKeepsQuantityAndRevalidatesTotal() {
    FakePort port = new FakePort();
    port.balance = 20;
    ShopPurchaseController controller = new ShopPurchaseController(row(), port, false);
    controller.handle(new ShopPurchaseEvent.QuantityChanged(2));
    assertEquals(14, controller.state().totalPrice());

    ShopRow refreshed = new ShopRow(new ShopItemSnapshot(
        "stone", "minecraft:stone", 7, 11, 7, "", 1, "", "", 0, 1, 1));
    controller.handle(new ShopPurchaseEvent.ItemRefreshed(refreshed));

    assertEquals(2, controller.state().quantity());
    assertEquals(22, controller.state().totalPrice());
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.shop.purchase.insufficient_balance", controller.state().errorKey());
  }

  private static ShopRow row() {
    return new ShopRow(new ShopItemSnapshot("stone", "minecraft:stone", 7, 7, 7, "", 1, "", "", 0, 1, 1));
  }
  private static final class FakePort implements ShopPurchasePort {
    int quantity;
    int calls;
    int balance = Integer.MAX_VALUE;
    @Override public int availableQuantity(ShopRow row) { return 2; }
    @Override public int currentBalance() { return balance; }
    @Override public void submit(ShopRow row, int quantity) { calls++; this.quantity = quantity; }
  }
}
