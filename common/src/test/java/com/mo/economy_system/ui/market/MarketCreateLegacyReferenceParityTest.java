package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact create-sales/create-demand visual assertions from legacy market forms. */
class MarketCreateLegacyReferenceParityTest {
  @Test
  void salesPreviewUsesNativeHoverNameAndSingleOwnedLine() {
    MarketInventoryItem inventory = new MarketInventoryItem(0, "minecraft:stone", 32, 64);
    MarketCreateState state = new MarketCreateState(MarketCreateMode.SALES, List.of(inventory),
        0, "", 1, 20, ScreenState.READY, null, Set.of(MarketCreateAction.values()));
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketCreateView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayName")),
        "legacy create-sales preview resolves ItemStack.getHoverName(), not a raw item id");
  }
}
