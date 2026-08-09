package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarketCreateLayoutTest {
  @Test void salesSlotsAndControlsStayInsideBothPanels() {
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.SALES,
        List.of(new MarketInventoryItem(0, "minecraft:stone", 1, 64),
            new MarketInventoryItem(1, "minecraft:dirt", 2, 64)), new EmptyPort());
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, controller.state());
    assertTrue(layout.inventoryPanel().contains(layout.slots().get(0).rect()));
    assertTrue(layout.inventoryPanel().contains(layout.slots().get(1).rect()));
    assertTrue(layout.formPanel().contains(layout.submit()));
    assertFalse(layout.inventoryPanel().overlaps(layout.formPanel()));
  }

  private static final class EmptyPort implements MarketCreatePort {
    @Override public boolean isKnownItem(String itemId) { return false; }
    @Override public int maxStackSize(String itemId) { return 64; }
    @Override public void submitSales(int slot, int quantity, int totalPrice) {}
    @Override public void submitDemand(String itemId, int quantity, int totalPrice) {}
  }
}
