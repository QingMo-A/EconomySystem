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
  void everyProtocolPageEntryIsReachableAtSupportedViewports() {
    MarketState state = readyState(9);
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {640, 80}}) {
      MarketLayout.Layout layout = MarketLayout.calculate(size[0], size[1], state);
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());
      assertEquals(MarketController.NETWORK_PAGE_SIZE, layout.pageSize());
      assertEquals(9, layout.cards().size());
      for (UiRect control : List.of(layout.search(), layout.filter(), layout.createSales(),
          layout.createDemand(), layout.previousButton(), layout.pageText(), layout.nextButton())) {
        assertTrue(viewport.contains(control), size[0] + "x" + size[1] + ": " + control);
      }
      for (MarketLayout.Card card : layout.cards()) {
        assertTrue(viewport.contains(card.card()));
        assertTrue(card.card().contains(card.itemIcon()));
        assertTrue(card.card().contains(card.actionButton()));
        assertTrue(card.card().bottom() <= layout.previousButton().y());
      }
      assertNoOverlap(layout.cards().stream().map(MarketLayout.Card::card).toList());
    }

    MarketLayout.Layout baseline = MarketLayout.calculate(640, 360, state);
    assertEquals(3, baseline.columns());
    assertEquals(3, baseline.rows());
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
