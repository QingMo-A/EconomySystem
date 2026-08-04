package com.mo.economy_system.screen.territory_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteRowLayoutTest {
  private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void rowsAreBoundedAndKeepStableIds() {
    var rows = TerritoryInviteRowLayout.layout(List.of(FIRST, SECOND), 0, 320, 200, 72, 20);
    assertEquals(2, rows.size());
    assertEquals(FIRST, rows.get(0).playerId());
    assertTrue(rows.get(0).contains(rows.get(0).x(), rows.get(0).y()));
  }

  @Test
  void scrollingAndEmptyViewAreClamped() {
    assertEquals(0, TerritoryInviteRowLayout.visibleCount(60));
    assertTrue(TerritoryInviteRowLayout.layout(List.of(), 0, 320, 125, 72, 20).isEmpty());
    assertEquals(1, TerritoryInviteRowLayout.layout(
        List.of(FIRST, SECOND), 99, 320, 100, 72, 20).size());
    assertEquals(0, TerritoryInviteRowLayout.clampScroll(-1, 2, 200));
  }
}
