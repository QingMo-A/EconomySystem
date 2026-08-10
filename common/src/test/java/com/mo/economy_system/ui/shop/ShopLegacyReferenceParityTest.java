package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.text.UiNumbers;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact assertions transcribed from the legacy NeoForge shop catalog. */
class ShopLegacyReferenceParityTest {
  @Test
  void canonicalCatalogGeometryAndCardHitTargetMatchLegacy() {
    ShopState state = new ShopState(ShopTestFixtures.items(15).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, state);

    assertEquals(5, layout.columns());
    assertEquals(3, layout.rows());
    assertEquals(15, layout.pageSize());
    assertEquals(54, layout.cards().get(0).card().x());
    assertEquals(162, layout.cards().get(1).card().x());
    assertEquals(270, layout.cards().get(2).card().x());
    assertEquals(378, layout.cards().get(3).card().x());
    assertEquals(486, layout.cards().get(4).card().x());
    assertEquals(55, layout.cards().get(0).card().y());
    assertEquals(143, layout.cards().get(5).card().y());
    assertEquals(231, layout.cards().get(10).card().y());
    assertEquals(26, layout.cards().get(0).itemIcon().y() - layout.cards().get(0).card().y());
    assertSame(layout.cards().get(0).card(), layout.cards().get(0).buyButton());
  }

  @Test
  void shopChromeAndPriceFormattingAreReferenceValues() {
    assertEquals(0xE04A5568, EconomyUiThemeProbe.searchBackground());
    assertEquals(0xFF4FC3F7, EconomyUiThemeProbe.searchBorder());
    assertEquals("￥1,234,567", "￥" + UiNumbers.formatInteger(1_234_567));
  }

  @Test
  void purchaseUsesNativeItemDisplayNameSemantic() {
    ShopState catalog = new ShopState(ShopTestFixtures.items(1).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    ShopPurchaseState purchase = new ShopPurchaseState(catalog.rows().get(0), 1, 20, 64,
        ScreenState.READY, null, Set.of(ShopPurchaseAction.values()));
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopPurchaseView.render(renderer, purchase, ShopPurchaseLayout.calculate(640, 360, purchase), 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayName")),
        "legacy purchase renders ItemStack.getHoverName(), not item description/id");
  }

  /** Keep test values independent of implementation-private renderer details. */
  private static final class EconomyUiThemeProbe {
    static int searchBackground() { return com.mo.economy_system.ui.theme.EconomyUiTheme.SHOP_SEARCH_FRAME.background(); }
    static int searchBorder() { return com.mo.economy_system.ui.theme.EconomyUiTheme.SHOP_SEARCH_FRAME.top(false); }
  }
}
