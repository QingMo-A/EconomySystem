package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import org.junit.jupiter.api.Test;

class ShopPurchaseControllerTest {
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

  private static ShopRow row() {
    return new ShopRow(new ShopItemSnapshot("stone", "minecraft:stone", 7, 7, 7, "", 1, "", "", 0, 1, 1));
  }
  private static final class FakePort implements ShopPurchasePort {
    int quantity;
    int calls;
    @Override public int availableQuantity(ShopRow row) { return 2; }
    @Override public void submit(ShopRow row, int quantity) { calls++; this.quantity = quantity; }
  }
}
