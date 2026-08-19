package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketCreateInventoryOverlayTest {
  @Test
  void inventoryCountUsesMandatoryItemWithCountSemantic() {
    MarketCreateState state = new MarketCreateState(MarketCreateMode.SALES,
        List.of(new MarketInventoryItem(0, "minecraft:stone", 8, 64)), 0, "minecraft:stone",
        1, 1, ScreenState.READY, null, Set.of(MarketCreateAction.values()));
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    MarketCreateView.render(renderer, state, layout, 0, 0);

    int itemWithCountIndex = -1;
    for (int index = 0; index < renderer.operations().size(); index++) {
      var operation = renderer.operations().get(index);
      if (itemWithCountIndex < 0 && operation.kind().equals("itemWithCount")
          && operation.value().equals("minecraft:stone:8")) itemWithCountIndex = index;
    }
    assertTrue(itemWithCountIndex >= 0,
        "inventory quantity must use the mandatory native item-with-count semantic");
  }
}
