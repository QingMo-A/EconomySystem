package com.mo.economy_system.screen.territory_system;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TerritoryMembersLayout {
  static final int ROW_HEIGHT = 24;

  record Rect(int x, int y, int width, int height) {
    Rect {
      if (width < 0 || height < 0) throw new IllegalArgumentException("negative size");
    }

    boolean contains(double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
  }

  record MemberValue(UUID playerId, String playerName) {}

  record MemberRow(MemberValue member, Rect row, Rect removeButton) {}

  record Layout(Rect search, Rect invite, Rect back, List<MemberRow> rows, int scroll) {}

  static Layout layout(int width, int height, List<MemberValue> members, int requestedScroll) {
    int safeWidth = Math.max(0, width);
    int safeHeight = Math.max(0, height);
    Rect search =
        new Rect(
            Math.max(8, safeWidth / 2 - 100), 24, Math.min(200, Math.max(0, safeWidth - 16)), 20);
    Rect invite =
        safeHeight >= 44
            ? new Rect(12, safeHeight - 24, Math.min(100, Math.max(0, safeWidth / 2 - 18)), 20)
            : new Rect(0, 0, 0, 0);
    Rect back =
        safeHeight >= 44
            ? new Rect(
                Math.max(12, safeWidth - 72),
                safeHeight - 24,
                Math.min(60, Math.max(0, safeWidth - 24)),
                20)
            : new Rect(0, 0, 0, 0);
    int visible = Math.max(0, (safeHeight - 100) / ROW_HEIGHT);
    int scroll = Math.max(0, Math.min(requestedScroll, Math.max(0, members.size() - visible)));
    List<MemberRow> rows = new ArrayList<>();
    for (int index = 0; index < Math.min(visible, members.size() - scroll); index++) {
      int y = 66 + index * ROW_HEIGHT;
      Rect row = new Rect(12, y, Math.max(0, safeWidth - 24), 20);
      Rect remove =
          new Rect(Math.max(12, safeWidth - 82), y, Math.min(62, Math.max(0, safeWidth - 24)), 20);
      rows.add(new MemberRow(members.get(scroll + index), row, remove));
    }
    return new Layout(search, invite, back, List.copyOf(rows), scroll);
  }

  private TerritoryMembersLayout() {}
}
