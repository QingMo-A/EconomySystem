package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarketCreateQuantitySyncTest {
  @Test
  void salesQuantityButtonsUpdateControllerStateImmediately() {
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.SALES,
        List.of(new MarketInventoryItem(0, "minecraft:stone", 8, 64)), new Port());

    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.INCREMENT));
    assertEquals(2, controller.state().quantity());
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.DECREMENT));
    assertEquals(1, controller.state().quantity());
    controller.handle(new MarketCreateEvent.ActionClicked(MarketCreateAction.SELECT_ALL));
    assertEquals(8, controller.state().quantity());
  }

  private static final class Port implements MarketCreatePort {
    @Override public List<String> itemIdSuggestions(String prefix) { return List.of(); }
    @Override public boolean isKnownItem(String itemId) { return true; }
    @Override public int maxStackSize(String itemId) { return 64; }
    @Override public void submitSales(int slot, int quantity, int totalPrice) {}
    @Override public void submitDemand(String itemId, int quantity, int totalPrice) {}
  }
}
