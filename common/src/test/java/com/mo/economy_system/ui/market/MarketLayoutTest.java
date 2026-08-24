package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketLayoutTest {
  @Test
  void v2ExposesCompleteNineOrderPageAndSeparatedDetailPane() {
    MarketState state = readyState(9);
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {640, 80}}) {
      MarketLayout.Layout layout = MarketLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      assertEquals(MarketController.NETWORK_PAGE_SIZE, layout.pageSize());
      assertEquals(9, layout.cards().size());
      assertEquals(3, layout.columns());
      assertEquals(3, layout.rows());
      assertTrue(layout.catalogArea().right() < layout.detailPanel().x());
      assertFalse(layout.catalogArea().overlaps(layout.detailPanel()));
      assertTrue(viewport.contains(layout.catalogArea()));
      assertTrue(viewport.contains(layout.detailPanel()));
      for (UiRect control : List.of(layout.search(), layout.createSales(), layout.createDemand(),
          layout.previousButton(), layout.pageText(), layout.nextButton())) {
        assertTrue(viewport.contains(control), size[0] + "x" + size[1] + ": " + control);
      }
      for (MarketLayout.Card card : layout.cards()) {
        assertTrue(layout.catalogArea().contains(card.card()));
        assertTrue(card.card().contains(card.itemIcon()));
        assertTrue(card.card().contains(card.name()));
        assertTrue(card.card().contains(card.typeBadge()));
        assertTrue(card.card().contains(card.unitPrice()));
        assertTrue(card.card().contains(card.summary()));
      }
      assertNoOverlap(layout.cards().stream().map(MarketLayout.Card::card).toList());
    }
  }

  @Test
  void baselineKeepsApproximatelySeventyThirtyBrowserDetailSplit() {
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, readyState(9));
    double catalogShare = layout.catalogArea().width()
        / (double) (layout.catalogArea().width() + layout.detailPanel().width());
    assertTrue(catalogShare >= 0.68 && catalogShare <= 0.73, "catalog share=" + catalogShare);
  }

  @Test
  void detailControlsStayInsidePaneAndDoNotCollide() {
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, readyState(9));
    for (UiRect rect : List.of(layout.detailTitle(), layout.detailItem(), layout.detailName(),
        layout.detailType(), layout.detailRemaining(), layout.detailUnitPrice(), layout.detailOrderTotal(),
        layout.detailOwner(), layout.detailFacts(), layout.quantityInput(), layout.decrement(), layout.increment(),
        layout.all(), layout.detailAmount(), layout.detailError(), layout.primaryAction(), layout.secondaryAction())) {
      assertTrue(layout.detailPanel().contains(rect), rect + " escapes detail panel");
    }
    assertFalse(layout.quantityInput().overlaps(layout.decrement()));
    assertFalse(layout.all().overlaps(layout.detailAmount()));
    assertFalse(layout.detailError().overlaps(layout.secondaryAction()));
    assertFalse(layout.secondaryAction().overlaps(layout.primaryAction()));
  }

  private static MarketState readyState(int count) {
    return new MarketState(MarketTestFixtures.orders(count).stream().map(MarketRow::new).toList(),
        0, MarketController.NETWORK_PAGE_SIZE, count, count, 0, MarketOrderFilter.ALL, "",
        ScreenState.READY, null, -1, 1, Set.of(MarketAction.values()));
  }

  private static void assertNoOverlap(List<UiRect> rectangles) {
    for (int left = 0; left < rectangles.size(); left++) {
      for (int right = left + 1; right < rectangles.size(); right++) {
        assertFalse(rectangles.get(left).overlaps(rectangles.get(right)),
            rectangles.get(left) + " overlaps " + rectangles.get(right));
      }
    }
  }
}
