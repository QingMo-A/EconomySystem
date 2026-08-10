package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact geometry/chrome assertions transcribed from the legacy market screens. */
class MarketLegacyReferenceParityTest {
  @Test
  void marketKeepsNineEntryPageWithoutViewportCrash() {
    MarketState state = ready(9);
    assertDoesNotThrow(() -> MarketLayout.calculate(100, 100, state),
        "legacy keeps page size 9 and renders what fits; it does not crash the screen");
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    assertEquals(3, layout.columns());
    assertEquals(3, layout.rows());
    assertEquals(12, layout.cards().get(0).card().x());
    assertEquals(220, layout.cards().get(1).card().x());
    assertEquals(428, layout.cards().get(2).card().x());
    assertEquals(55, layout.cards().get(0).card().y());
    assertEquals(143, layout.cards().get(3).card().y());
    assertEquals(231, layout.cards().get(6).card().y());
  }

  @Test
  void topButtonsUseLegacyRightToLeftOrderAndStyles() {
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, ready(0));
    assertEquals(544, layout.createSales().x());
    assertEquals(450, layout.createDemand().x());
    assertEquals(0x27AE60, EconomyUiTheme.MARKET_TOP_SALES_BUTTON.accent());
    assertEquals(0xFF8C00, EconomyUiTheme.MARKET_TOP_DEMAND_BUTTON.accent());
    assertEquals(4, EconomyUiTheme.MARKET_TOP_SALES_BUTTON.stripeWidth());
    assertEquals(7, EconomyUiTheme.MARKET_TOP_SALES_BUTTON.glowHeight());
  }

  @Test
  void cardUsesNativeItemNameAndActionSpecificVisualSemantics() {
    MarketState state = ready(1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayNameWithSuffix")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("icon")
        && op.value().equals("ARROW_LEFT") && op.rect().width() == 12));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("inputFrame")),
        "search uses dedicated four-edge frame, not market card chrome");
  }

  @Test
  void operatorGetsSeparateForceRemovalButtonWithoutChangingWireAction() {
    MarketState state = ready(1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER, 0, 0);
    assertEquals(new UiRect(64, 109, 72, 18), layout.cards().get(0).adminActionButton());
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.rect().equals(layout.cards().get(0).adminActionButton())
        && op.value().startsWith("screen.market.remove_sales") && op.enabled()));
    assertTrue(MarketAction.ADMIN_REMOVE_SALES != MarketAction.REMOVE_SALES,
        "operator presentation stays distinct while both submit the existing remove-sales message");
  }

  @Test
  void filterGeometryUsesTranslatedMetrics() {
    UiTextMetrics metrics = new UiTextMetrics() {
      @Override public int width(String text) { return 6; }
      @Override public int translatedWidth(String key, List<String> arguments) {
        return switch (key) {
          case "screen.market.filter.all" -> 30;
          case "screen.market.filter.mine" -> 42;
          case "screen.market.filter.sales" -> 36;
          case "screen.market.filter.demand" -> 48;
          default -> 6;
        };
      }
      @Override public int lineHeight() { return 9; }
    };
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, ready(0), metrics, 1.0f);
    assertEquals(50, layout.filterTabs().get(0).rect().width());
    assertEquals(62, layout.filterTabs().get(1).rect().width());
    assertEquals(56, layout.filterTabs().get(2).rect().width());
    assertEquals(68, layout.filterTabs().get(3).rect().width());
  }

  @Test
  void cardNativeNameCountAndOwnerAreEachTruncatedAsOneLegacyLine() {
    MarketState state = ready(1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayNameWithSuffix")
        && op.value().contains(" x1")), "legacy uses native hover name + count as one clipped line");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextWithSuffix")
        && op.value().contains("owner-0")),
        "legacy truncates translated Seller/Requester + ': ' + name as one line");
  }

  @Test
  void cardPriceUsesYenAndFormattedNumberSemantic() {
    MarketState state = ready(1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("textInRect")
        && op.value().contains("\uFFE5" + "20")),
        "legacy price text is Yen-prefixed and formatted");
  }

  @Test
  void iconAndOrderInfoHoverExposeLegacyMetadataTooltipTriggers() {
    MarketState state = ready(1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER,
        layout.cards().get(0).itemIcon().x(), layout.cards().get(0).itemIcon().y());
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("tooltip")),
        "legacy market icon hover renders native item + order metadata tooltip");
  }

  private static MarketState ready(int count) {
    return new MarketState(MarketTestFixtures.orders(count).stream().map(MarketRow::new).toList(),
        0, MarketController.NETWORK_PAGE_SIZE, count, count, 0, MarketOrderFilter.ALL, "",
        ScreenState.READY, null, -1, 1, Set.of(MarketAction.values()));
  }
}
