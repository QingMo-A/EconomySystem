package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryLayoutTest {
  @Test
  void tinyViewportsKeepStablePositiveRectsAndContainedButtons() {
    DeliveryState state = new DeliveryState(
        List.of(new DeliveryRow(DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1))),
        0, 1, "", ScreenState.READY, null, 1, Set.of(DeliveryAction.CLAIM));
    for (int[] viewport : new int[][] {{1, 1}, {80, 60}, {320, 180}, {1280, 720}}) {
      DeliveryLayout.Layout layout = DeliveryLayout.calculate(viewport[0], viewport[1], state);
      assertTrue(layout.pageSize() >= 1);
      for (DeliveryLayout.Card card : layout.cards()) {
        assertTrue(card.card().contains(card.claimButton()));
        assertTrue(card.card().contains(card.itemIcon()));
      }
    }
  }
}
