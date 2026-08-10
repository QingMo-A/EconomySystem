package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.text.UiTextMetrics;
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
    assertTrue(layout.cards().get(0).card().contains(layout.cards().get(0).itemIcon()));
  }

  @Test
  void shopChromeAndPriceFormattingAreReferenceValues() {
    assertEquals(0xE04A5568, EconomyUiThemeProbe.searchBackground());
    assertEquals(0xFF4FC3F7, EconomyUiThemeProbe.searchBorder());
    assertEquals("\uFFE5" + "1,234,567", "\uFFE5" + com.mo.economy_system.ui.text.UiNumbers.formatInteger(1_234_567));
  }

  @Test
  void catalogCardIsOnlyClickTargetWithoutExtraBuyButtonOrVirtualBackground() {
    ShopState state = new ShopState(ShopTestFixtures.items(1).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopView.render(renderer, state, layout, 0, 0);

    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
            && op.value().startsWith("screen.shop.buy")),
        "legacy catalog cards are click/tooltip targets; only the purchase screen has a buy button");
    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("fill")
            && op.rect().equals(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()))),
        "fullscreen background belongs to the physical target shell, not the scaled common view");
  }

  @Test
  void pageTextUsesLegacyBaselineAndPriceUsesNativeYenPrefix() {
    ShopState state = new ShopState(ShopTestFixtures.items(30).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    UiTextMetrics metrics = new UiTextMetrics() {
      @Override public int width(String text) { return text == null ? 0 : text.length() * 7; }
      @Override public int lineHeight() { return 11; }
    };
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, state, metrics, 1.0f);
    assertEquals(325, layout.pageText().y());
    assertEquals(11, layout.pageText().height());

    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("textInRect")
            && op.value().startsWith("\uFFE5")),
        "legacy catalog price is prefixed with the native Yen symbol");
  }

  @Test
  void priceChangeTooltipCarriesLegacyUpDownSameColorSemantic() {
    ShopState state = new ShopState(ShopTestFixtures.items(1).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    TooltipLine change = ShopView.tooltipAt(state, ShopLayout.calculate(640, 360, state), 55, 56)
        .orElseThrow().lines().get(2);
    assertTrue(change.getClass().getSimpleName().contains("Colored"),
        "price change line must carry an explicit target-resolved color (red/green/gray)");
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

  private static final class EconomyUiThemeProbe {
    static int searchBackground() { return com.mo.economy_system.ui.theme.EconomyUiTheme.SHOP_SEARCH_FRAME.background(); }
    static int searchBorder() { return com.mo.economy_system.ui.theme.EconomyUiTheme.SHOP_SEARCH_FRAME.top(false); }
  }
}
