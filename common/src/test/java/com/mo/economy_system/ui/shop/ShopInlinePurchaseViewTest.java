package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopInlinePurchaseViewTest {
  @Test
  void emptySelectionKeepsPurchasePaneBlank() {
    ShopState shop = shopState();
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, shop);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    ShopView.render(renderer, shop, layout, null, 0, 0);

    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("item")
        && op.rect().equals(layout.purchaseItem())));
    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.shop.purchase.confirm")));
  }

  @Test
  void selectedItemRendersInlineDetailsAndConfirm() {
    ShopState shop = shopState();
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, shop);
    ShopPurchaseState purchase = new ShopPurchaseState(shop.rows().get(0), 2, 14, 64, 100,
        ScreenState.READY, null, Set.of(ShopPurchaseAction.CONFIRM, ShopPurchaseAction.BACK));
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    ShopView.render(renderer, shop, layout, purchase, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("item")
        && op.rect().equals(layout.purchaseItem())));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.shop.purchase.confirm") && op.enabled()));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.shop.purchase.balance")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.shop.purchase.capacity")));
  }

  @Test
  void invalidPurchaseRendersErrorAndDisabledConfirm() {
    ShopState shop = shopState();
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, shop);
    ShopPurchaseState purchase = new ShopPurchaseState(shop.rows().get(0), 2, 14, 64, 10,
        ScreenState.ERROR, "screen.shop.purchase.insufficient_balance", Set.of(ShopPurchaseAction.BACK));
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    ShopView.render(renderer, shop, layout, purchase, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.shop.purchase.insufficient_balance")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.shop.purchase.confirm") && !op.enabled()));
  }

  private static ShopState shopState() {
    ShopRow row = new ShopRow(new ShopItemSnapshot("stone", "minecraft:stone", 7, 7, 7,
        "", 1, "", "", 0, 64, 64));
    return new ShopState(List.of(row), 0, 12, "", ScreenState.READY, null, -1,
        Set.of(ShopAction.values()));
  }
}
