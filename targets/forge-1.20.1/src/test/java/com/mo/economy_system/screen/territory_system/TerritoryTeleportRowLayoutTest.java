package com.mo.economy_system.screen.territory_system;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryTeleportRowLayoutTest {
  private static final UUID FIRST=UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SECOND=UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID THIRD=UUID.fromString("00000000-0000-0000-0000-000000000003");
  @Test void adjacentButtonsDoNotOverlapAndMapIds() {
    var areas=TerritoryTeleportRowLayout.layout(List.of(FIRST,SECOND),0,320,200,72,20);
    assertEquals(FIRST,areas.get(0).territoryId());assertEquals(SECOND,areas.get(1).territoryId());
    assertTrue(areas.get(0).y()+areas.get(0).height()<=areas.get(1).y());
    assertTrue(areas.get(0).contains(areas.get(0).x()+1,areas.get(0).y()+1));
    assertFalse(areas.get(1).contains(areas.get(0).x()+1,areas.get(0).y()+1));
  }
  @Test void filteredAndScrolledMappingsRemainExact() {
    assertEquals(SECOND,TerritoryTeleportRowLayout.layout(List.of(SECOND),0,320,100,72,20).get(0).territoryId());
    var scrolled=TerritoryTeleportRowLayout.layout(List.of(FIRST,SECOND,THIRD),1,320,125,72,20);
    assertEquals(SECOND,scrolled.get(0).territoryId());
  }
  @Test void scrollIsClampedSoEveryRowCanBecomeVisible() {
    int visible=TerritoryTeleportRowLayout.visibleCount(125);
    int end=TerritoryTeleportRowLayout.clampScroll(99,5,125);
    assertEquals(5-visible,end);
    assertEquals(5,TerritoryTeleportRowLayout.layout(List.of(FIRST,SECOND,THIRD,UUID.randomUUID(),UUID.randomUUID()),end,320,125,72,20).size()+end);
  }
  @Test void emptyExactOverflowAndTinyLayoutsAreBounded() {
    assertTrue(TerritoryTeleportRowLayout.layout(List.of(),0,320,125,72,20).isEmpty());
    assertEquals(1,TerritoryTeleportRowLayout.layout(List.of(FIRST),0,320,125,72,20).size());
    int visible=TerritoryTeleportRowLayout.visibleCount(125);List<UUID> exact=java.util.stream.IntStream.range(0,visible).mapToObj(i->UUID.randomUUID()).toList();
    assertEquals(visible,TerritoryTeleportRowLayout.layout(exact,0,320,125,72,20).size());
    assertEquals(1,TerritoryTeleportRowLayout.clampScroll(9,visible+1,125));
    assertEquals(0,TerritoryTeleportRowLayout.visibleCount(60));assertTrue(TerritoryTeleportRowLayout.layout(List.of(FIRST),0,320,60,72,20).isEmpty());
  }

  @Test void ownedRowsHaveSeparateTeleportAndInviteHitboxes() {
    var rows = List.of(new TerritoryTeleportRowLayout.TerritoryRow(FIRST, true),
        new TerritoryTeleportRowLayout.TerritoryRow(SECOND, false));
    var areas = TerritoryTeleportRowLayout.layoutActions(rows, 0, 320, 200, 72, 20);
    assertEquals(3, areas.size());
    var teleport = areas.get(0);
    var invite = areas.get(1);
    assertEquals(TerritoryTeleportRowLayout.Action.TELEPORT, teleport.action());
    assertEquals(TerritoryTeleportRowLayout.Action.INVITE, invite.action());
    assertEquals(FIRST, teleport.territoryId());
    assertEquals(FIRST, invite.territoryId());
    assertTrue(teleport.x() + teleport.width() <= invite.x());
    assertFalse(teleport.contains(invite.x() + 1, invite.y() + 1));
    assertEquals(TerritoryTeleportRowLayout.Action.TELEPORT, areas.get(2).action());
    assertEquals(SECOND, areas.get(2).territoryId());
  }
}
