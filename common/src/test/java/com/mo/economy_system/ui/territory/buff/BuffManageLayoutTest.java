package com.mo.economy_system.ui.territory.buff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuffManageLayoutTest {
  @Test
  void freezesBaselineGeometryAndKeepsControlsInsideViewport() {
    List<BuffRow> rows = java.util.stream.IntStream.range(0, 12)
        .mapToObj(i -> BuffRow.inspect(
            BuffManageTestFixtures.buff("buff" + i, i % 2 == 0, 0, 3, 0, 0, 0),
            BuffResourceSnapshot.unknown())).toList();
    var state = new BuffManageState(UUID.randomUUID(), "territory", rows, 0, 6, 0, "",
        ScreenState.READY, null, -1);
    var baseline = BuffManageLayout.calculate(640, 360, state);
    assertEquals(304, baseline.cards().get(0).card().width());
    assertEquals(98, baseline.cards().get(0).card().height());
    assertEquals(60, baseline.cards().get(0).card().y());
    for (int[] size : new int[][]{{320, 180}, {640, 360}, {854, 480}, {1280, 720},
        {1920, 1080}, {180, 120}, {160, 90}, {640, 80}}) {
      var layout = BuffManageLayout.calculate(size[0], size[1], state);
      int width = layout.scale().virtualWidth();
      int height = layout.scale().virtualHeight();
      for (UiRect rect : List.of(layout.search(), layout.header(), layout.previousButton(),
          layout.nextButton(), layout.pageText(), layout.retryButton(), layout.footerTitle(),
          layout.escHint())) {
        assertTrue(rect.x() >= 0 && rect.y() >= 0);
        assertTrue(rect.right() <= width && rect.bottom() <= height);
      }
      assertTrue(layout.cards().get(0).card().width() >= BuffManageLayout.MIN_CARD_WIDTH);
      assertEquals(BuffManageLayout.CARD_HEIGHT, layout.cards().get(0).card().height());
      assertEquals(BuffManageLayout.LIST_START_Y, layout.cards().get(0).card().y());
      assertEquals(12, layout.search().x());
      assertEquals(20, layout.search().y());
      assertEquals(200, layout.search().width());
      assertEquals(20, layout.search().height());
      assertFalse(layout.previousButton().overlaps(layout.nextButton()));
      assertFalse(layout.previousButton().overlaps(layout.footerTitle()));
      assertFalse(layout.nextButton().overlaps(layout.escHint()));
      for (var card : layout.cards()) {
        assertTrue(card.card().contains(card.icon()));
        assertTrue(card.card().contains(card.actionButton()));
        assertTrue(card.card().contains(card.name()));
        assertTrue(card.card().contains(card.level()));
        assertTrue(card.card().contains(card.levelTrack()));
        assertTrue(card.card().contains(card.status()));
        assertTrue(card.card().contains(card.cost()));
        assertTrue(card.card().contains(card.availability()));
        assertFalse(card.name().overlaps(card.level()));
        assertFalse(card.cost().overlaps(card.actionButton()));
        assertFalse(card.availability().overlaps(card.actionButton()));
      }
    }
  }

  @Test
  void cardGridHasNoOverlappingCards() {
    List<BuffRow> rows = java.util.stream.IntStream.range(0, 12)
        .mapToObj(i -> BuffRow.inspect(
            BuffManageTestFixtures.buff("buff" + i, true, 0, 3, 0, 0, 0),
            BuffResourceSnapshot.unknown())).toList();
    var state = new BuffManageState(UUID.randomUUID(), "territory", rows, 0, 12, 0, "",
        ScreenState.READY, null, -1);
    var layout = BuffManageLayout.calculate(1280, 720, state);
    for (int left = 0; left < layout.cards().size(); left++) {
      for (int right = left + 1; right < layout.cards().size(); right++) {
        assertFalse(layout.cards().get(left).card().overlaps(layout.cards().get(right).card()));
      }
    }
  }
}
