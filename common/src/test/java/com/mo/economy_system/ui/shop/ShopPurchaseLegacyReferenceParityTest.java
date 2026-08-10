package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact purchase-dialog geometry/chrome assertions from legacy Screen_BuyItem. */
class ShopPurchaseLegacyReferenceParityTest {
  @Test
  void purchasePanelUsesLegacyItemPriceTotalAndOnlyBuyAction() {
    ShopRow row = new ShopRow(ShopTestFixtures.item(0));
    ShopPurchaseState state = new ShopPurchaseState(row, 2, 20, 40, ScreenState.READY, null,
        Set.of(ShopPurchaseAction.values()));
    ShopPurchaseLayout.Layout layout = ShopPurchaseLayout.calculate(640, 360, state);
    assertEquals(new UiRect(160, 100, 320, 160), layout.card());
    assertEquals(new UiRect(312, 134, 16, 16), layout.item());
    assertEquals(new UiRect(272, 198, 140, 20), layout.quantity());
    assertEquals(new UiRect(272, 224, 96, 24), layout.confirm());

    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopPurchaseView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayName")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.shop.purchase.total") && op.rect().y() == 169),
        "legacy total price baseline is unit price baseline + lineHeight + 4");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.shop.purchase.confirm")));
    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.shop.purchase.back")),
        "legacy purchase uses ESC to cancel and has no second visible back button");
  }
}
