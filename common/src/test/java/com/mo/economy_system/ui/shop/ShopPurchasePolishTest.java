package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopPurchasePolishTest {
  @Test
  void titleAndItemNameHaveIndependentNonOverlappingRegions() {
    ShopPurchaseState state = new ShopPurchaseState(new ShopRow(ShopTestFixtures.item(0)),
        1, 20, 64, ScreenState.READY, null, Set.of(ShopPurchaseAction.values()));
    ShopPurchaseLayout.Layout layout = ShopPurchaseLayout.calculate(640, 360, state);

    assertFalse(layout.title().overlaps(layout.itemName()));
    assertTrue(layout.title().x() == layout.card().x() + 12,
        "title must be anchored at the card's upper-left content inset");
    assertTrue(EconomyUiTheme.SHOP_PURCHASE_PANEL.accentWidth() > 0,
        "purchase card must retain its left accent stripe");

    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopPurchaseView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("itemDisplayName")
            && operation.rect().equals(layout.itemName())));
  }
}
