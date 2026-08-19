package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketCreateLayoutTest {
  @Test void salesSlotsAndControlsStayInsideBothPanels() {
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.SALES,
        List.of(new MarketInventoryItem(0, "minecraft:stone", 1, 64),
            new MarketInventoryItem(1, "minecraft:dirt", 2, 64)), new EmptyPort());
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, controller.state());
    assertEquals(420, layout.formPanel().width());
    assertEquals((640 - layout.formPanel().width()) / 2, layout.formPanel().x());
    assertEquals(layout.formPanel().x(), layout.inventoryPanel().x());
    assertEquals(layout.formPanel().width(), layout.inventoryPanel().width());
    assertTrue(layout.inventoryPanel().contains(layout.slots().get(0).rect()));
    assertTrue(layout.inventoryPanel().contains(layout.slots().get(1).rect()));
    assertTrue(layout.inventoryPanel().y() > layout.formPanel().bottom(),
        "sales settings must be above the inventory chooser");
    assertTrue(layout.submit().y() >= layout.inventoryPanel().bottom(),
        "sales submit button belongs below the inventory chooser");
    assertFalse(layout.inventoryPanel().overlaps(layout.formPanel()));
  }

  @Test void salesInventoryWrapsAfterNineColumns() {
    List<MarketInventoryItem> inventory = java.util.stream.IntStream.range(0, 10)
        .mapToObj(index -> new MarketInventoryItem(index, "minecraft:stone", 1, 64))
        .toList();
    MarketCreateController controller = new MarketCreateController(
        MarketCreateMode.SALES, inventory, new EmptyPort());
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, controller.state());

    assertEquals(layout.slots().get(0).rect().y(), layout.slots().get(8).rect().y());
    assertEquals(layout.slots().get(0).rect().x(), layout.slots().get(9).rect().x());
    assertTrue(layout.slots().get(9).rect().y() > layout.slots().get(0).rect().y());
  }

  @Test void demandFormKeepsResponsiveFieldWidth() {
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.DEMAND,
        List.of(), new EmptyPort());
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, controller.state());
    assertTrue(layout.formPanel().width() <= 360,
        "demand form should not consume nearly two thirds of the viewport");
    assertTrue(layout.formPanel().contains(layout.completionDropdown()),
        "completion dropdown must stay inside the responsive form panel");
  }

  @Test void createPagesUseShopStyleFooterAndNoVisibleBackButton() {
    MarketCreateState state = new MarketCreateState(MarketCreateMode.DEMAND, List.of(), 0, "",
        1, 20, ScreenState.READY, null, Set.of(MarketCreateAction.values()));
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, state);
    assertEquals(348, layout.title().bottom());
    assertEquals(0, layout.back().width());

    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketCreateView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("scaledIconTranslatedText")
        && op.value().contains("screen.market.create.demand_title")));
    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().contains("screen.market.create.back")));
  }

  private static final class EmptyPort implements MarketCreatePort {
    @Override public List<String> itemIdSuggestions(String prefix) { return List.of(); }
    @Override public boolean isKnownItem(String itemId) { return false; }
    @Override public int maxStackSize(String itemId) { return 64; }
    @Override public void submitSales(int slot, int quantity, int totalPrice) {}
    @Override public void submitDemand(String itemId, int quantity, int totalPrice) {}
  }
}
