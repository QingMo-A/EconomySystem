package com.mo.economy_system.screen.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TerritoryMembersLayoutTest {
  @Test
  void width320StaysInBoundsAndControlsDoNotOverlap() {
    var layout = TerritoryMembersLayout.layout(320, 240, members(12), 0);
    assertInBounds(layout.search(), 320, 240);
    assertInBounds(layout.invite(), 320, 240);
    assertInBounds(layout.back(), 320, 240);
    assertFalse(overlaps(layout.invite(), layout.back()));
    for (int i = 0; i < layout.rows().size(); i++) {
      var row = layout.rows().get(i);
      assertInBounds(row.row(), 320, 240);
      assertInBounds(row.removeButton(), 320, 240);
      if (i > 0) assertFalse(overlaps(layout.rows().get(i - 1).row(), row.row()));
    }
  }

  @Test
  void tinyHeightAndEmptyListCreateNoInvisibleActions() {
    var tiny = TerritoryMembersLayout.layout(320, 30, members(4), 0);
    assertTrue(tiny.rows().isEmpty());
    assertFalse(tiny.search().visible());
    assertFalse(tiny.invite().visible());
    assertFalse(tiny.back().visible());
    assertTrue(TerritoryMembersLayout.layout(320, 240, List.of(), 99).rows().isEmpty());
  }

  @Test
  void narrowWidthsHideControlsThatDoNotFitAndNeverOverlap() {
    for (int width : new int[] {1, 8, 16, 32, 64, 96, 160, 240, 320}) {
      var layout = TerritoryMembersLayout.layout(width, 180, members(8), 0);
      for (var rect : List.of(layout.search(), layout.invite(), layout.back()))
        if (rect.visible()) assertTrue(rect.inside(width, 180), "width=" + width);
      if (layout.invite().visible() && layout.back().visible())
        assertFalse(overlaps(layout.invite(), layout.back()), "width=" + width);
      for (var row : layout.rows()) {
        assertTrue(row.row().inside(width, 180), "width=" + width);
        assertTrue(row.removeButton().inside(width, 180), "width=" + width);
      }
    }
  }

  @Test
  void scrollClampsAndPreservesUuidNamePairs() {
    List<TerritoryMembersLayout.MemberValue> members = members(20);
    var layout = TerritoryMembersLayout.layout(320, 148, members, Integer.MAX_VALUE);
    int visible = Math.max(0, (148 - 100) / TerritoryMembersLayout.ROW_HEIGHT);
    assertEquals(members.size() - visible, layout.scroll());
    assertEquals(
        members.subList(layout.scroll(), members.size()),
        layout.rows().stream().map(TerritoryMembersLayout.MemberRow::member).toList());
  }

  private static List<TerritoryMembersLayout.MemberValue> members(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new TerritoryMembersLayout.MemberValue(new UUID(0, i + 1), "member-" + i))
        .toList();
  }

  private static void assertInBounds(TerritoryMembersLayout.Rect rect, int width, int height) {
    assertTrue(rect.x() >= 0 && rect.y() >= 0);
    assertTrue(rect.x() + rect.width() <= width);
    assertTrue(rect.y() + rect.height() <= height);
  }

  private static boolean overlaps(TerritoryMembersLayout.Rect a, TerritoryMembersLayout.Rect b) {
    return a.x() < b.x() + b.width()
        && b.x() < a.x() + a.width()
        && a.y() < b.y() + b.height()
        && b.y() < a.y() + a.height();
  }
}
