package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryLayoutTest {
  @Test
  void tinyViewportsKeepStablePositiveRectsAndContainedMailboxControls() {
    DeliveryState state = new DeliveryState(
        List.of(new DeliveryRow(DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1))),
        0, 1, "", ScreenState.READY, null, 1, Set.of(DeliveryAction.CLAIM));
    for (int[] viewport : new int[][] {{1, 1}, {80, 60}, {320, 180}, {1280, 720}}) {
      DeliveryLayout.Layout layout = DeliveryLayout.calculate(viewport[0], viewport[1], state);
      assertTrue(layout.pageSize() >= 1);
      assertTrue(layout.categoryPanel().width() > 0);
      assertTrue(layout.detailPanel().width() > 0);
      assertEquals(layout.detailPanel(), layout.message(), "empty/loading feedback belongs to the detail pane");
      assertTrue(layout.attachmentCard().contains(layout.detailItemIcon()));
      assertTrue(layout.attachmentCard().width() == 28 && layout.attachmentCard().height() == 28);
      for (DeliveryLayout.CategoryTab tab : layout.categoryTabs()) {
        assertTrue(layout.categoryPanel().contains(tab.rect()));
      }
      for (DeliveryLayout.Card card : layout.cards()) {
        assertTrue(card.card().contains(card.itemIcon()));
      }
    }
  }
}
