package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.text.UiTextMetrics;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Market v2 presentation contracts replacing the old card-action parity assertions. */
class MarketLegacyReferenceParityTest {
  @Test
  void marketV2KeepsNineEntryProtocolPageWithoutViewportCrash() {
    MarketState state = ready(9);
    assertDoesNotThrow(() -> MarketLayout.calculate(100, 100, state));
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    assertEquals(3, layout.columns());
    assertEquals(3, layout.rows());
    assertEquals(9, layout.cards().size());
  }

  @Test
  void orderCardsAreSelectionSurfacesNotActionButtonContainers() {
    MarketState state = readyPaged(1, MarketController.NETWORK_PAGE_SIZE + 1);
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.market.card.unit_price")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.market.card.summary")));
    assertFalse(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && layout.cards().stream().anyMatch(card -> card.card().contains(op.rect()))),
        "Market v2 order cards must not contain buy/deliver/remove buttons");
  }

  @Test
  void sortTabsUseTranslatedMetricsAndRemainDistinctFromFilters() {
    UiTextMetrics metrics = new UiTextMetrics() {
      @Override public int width(String text) { return 6; }
      @Override public int translatedWidth(String key, List<String> arguments) {
        return switch (key) {
          case "screen.market.filter.all" -> 30;
          case "screen.market.filter.mine" -> 42;
          case "screen.market.filter.sales" -> 36;
          case "screen.market.filter.demand" -> 48;
          case "screen.market.sort.default" -> 24;
          case "screen.market.sort.unit_asc" -> 30;
          case "screen.market.sort.unit_desc" -> 32;
          case "screen.market.sort.newest" -> 26;
          case "screen.market.sort.expiring" -> 40;
          default -> 6;
        };
      }
      @Override public int lineHeight() { return 9; }
    };

    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, ready(0), metrics, 1.0f);
    assertEquals(4, layout.filterTabs().size());
    assertEquals(MarketOrderFilter.ALL, layout.filterTabs().get(0).filter());
    assertEquals(5, layout.sortTabs().size());
    assertEquals(MarketOrderSort.DEFAULT, layout.sortTabs().get(0).sort());
    assertEquals(24, layout.sortTabs().get(0).textRect().width());
    assertTrue(layout.sortTabs().get(0).textRect().y() > layout.filterTabs().get(0).textRect().y());
  }

  @Test
  void operatorForceRemovalLivesInDetailPaneInsteadOfOrderCard() {
    MarketRow row = new MarketRow(MarketTestFixtures.order(
        0, MarketOrderType.SALES, new java.util.UUID(2, 2), false));
    MarketDetailController detail = new MarketDetailController(
        row, MarketTestFixtures.VIEWER, true, (action, selected, quantity) -> { });

    assertEquals(MarketAction.BUY, detail.state().primaryAction());
    assertEquals(MarketAction.ADMIN_REMOVE_SALES, detail.state().secondaryAction());
    assertTrue(detail.state().can(MarketDetailAction.SUBMIT_SECONDARY));
  }

  @Test
  void selectedCardUsesDetailTradeIdAsSelectionAnchor() {
    MarketState state = ready(1);
    MarketRow row = state.rows().get(0);
    MarketDetailState detail = new MarketDetailState(row, MarketAction.REMOVE_SALES, null,
        0, row.order().totalPrice(), 0, 0, 0, 0, true,
        ScreenState.READY, null, Set.of(MarketDetailAction.SUBMIT_PRIMARY));
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();

    MarketView.render(renderer, state, layout, detail, MarketTestFixtures.VIEWER, 0, 0);

    assertTrue(renderer.paints().stream().anyMatch(paint ->
        paint.argb() == com.mo.economy_system.ui.theme.EconomyUiTheme.MARKET_ACCENT
            && paint.rect().overlaps(layout.cards().get(0).card())));
  }

  private static MarketState ready(int count) {
    return new MarketState(MarketTestFixtures.orders(count).stream().map(MarketRow::new).toList(),
        0, MarketController.NETWORK_PAGE_SIZE, count, count, 0, MarketOrderFilter.ALL, "",
        ScreenState.READY, null, -1, 1, Set.of(MarketAction.values()));
  }

  private static MarketState readyPaged(int page, int total) {
    return new MarketState(MarketTestFixtures.orders(MarketController.NETWORK_PAGE_SIZE).stream()
        .map(MarketRow::new).toList(), page, MarketController.NETWORK_PAGE_SIZE, total, total, 0,
        MarketOrderFilter.ALL, "", ScreenState.READY, null, -1, 1, Set.of(MarketAction.values()));
  }
}
