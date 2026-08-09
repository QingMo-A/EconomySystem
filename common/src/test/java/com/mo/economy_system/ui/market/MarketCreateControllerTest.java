package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketCreateControllerTest {
  @Test void salesFormRejectsMissingPriceAndSubmitsValidatedSlot() {
    FakePort port = new FakePort();
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.SALES,
        List.of(new MarketInventoryItem(4, "minecraft:stone", 8, 64)), port);
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.market.create.invalid_price", controller.state().errorKey());
    controller.handle(new MarketCreateEvent.PriceChanged(12));
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT));
    assertEquals(4, port.salesSlot);
    assertEquals(1, port.quantity);
    assertEquals(12, port.price);
    assertTrue(controller.pollNavigation().isPresent());
  }

  @Test void demandFormValidatesRegistryAndStackLimit() {
    FakePort port = new FakePort();
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.DEMAND, List.of(), port);
    controller.handle(new MarketCreateEvent.ItemIdChanged("minecraft:diamond"));
    controller.handle(new MarketCreateEvent.QuantityChanged(65));
    controller.handle(new MarketCreateEvent.PriceChanged(3));
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT));
    assertEquals("screen.market.create.invalid_quantity", controller.state().errorKey());
    controller.handle(new MarketCreateEvent.QuantityChanged(4));
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SUBMIT));
    assertEquals("minecraft:diamond", port.demandItem);
    assertEquals(4, port.quantity);
  }

  private static final class FakePort implements MarketCreatePort {
    int salesSlot = -1, quantity, price;
    String demandItem;
    @Override public boolean isKnownItem(String itemId) { return "minecraft:diamond".equals(itemId); }
    @Override public int maxStackSize(String itemId) { return 64; }
    @Override public void submitSales(int slot, int quantity, int totalPrice) { salesSlot = slot; this.quantity = quantity; price = totalPrice; }
    @Override public void submitDemand(String itemId, int quantity, int totalPrice) { demandItem = itemId; this.quantity = quantity; price = totalPrice; }
  }
}
