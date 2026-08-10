package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopLayoutTest {
  @Test
  void catalogCardsAndControlsStayInsideSupportedViewports() {
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {640, 80}}) {
      ShopState provisional = state(40, 1);
      int pageSize = ShopLayout.calculate(size[0], size[1], provisional).pageSize();
      ShopLayout.Layout layout = ShopLayout.calculate(size[0], size[1], state(40, pageSize));
      UiRect viewport = new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight());

      assertEquals(Math.min(40, pageSize), layout.cards().size());
      for (UiRect control : List.of(layout.search(), layout.searchBackground(), layout.previousButton(),
          layout.pageText(), layout.nextButton())) {
        assertTrue(viewport.contains(control), size[0] + "x" + size[1] + ": " + control);
      }
      for (ShopLayout.Card card : layout.cards()) {
        assertTrue(viewport.contains(card.card()));
        assertTrue(card.card().contains(card.itemIcon()));
        assertTrue(card.card().contains(card.itemIcon()));
        assertTrue(card.card().bottom() <= layout.previousButton().y());
      }
      assertNoOverlap(layout.cards().stream().map(ShopLayout.Card::card).toList());
    }

    ShopLayout.Layout baseline = ShopLayout.calculate(640, 360, state(40, 15));
    assertEquals(5, baseline.columns());
    assertEquals(3, baseline.rows());
    assertEquals(15, baseline.pageSize());
  }

  private static ShopState state(int count, int pageSize) {
    return new ShopState(ShopTestFixtures.items(count).stream().map(ShopRow::new).toList(),
        0, pageSize, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
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
