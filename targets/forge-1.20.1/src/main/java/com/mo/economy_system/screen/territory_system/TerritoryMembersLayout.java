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
      return visible() && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    boolean visible() {
      return width > 0 && height > 0;
    }

    boolean inside(int screenWidth, int screenHeight) {
      return visible()
          && x >= 0
          && y >= 0
          && x + width <= screenWidth
          && y + height <= screenHeight;
    }
  }

  record MemberValue(UUID playerId, String playerName) {}

  record MemberRow(MemberValue member, Rect row, Rect removeButton) {}

  record Layout(Rect search, Rect invite, Rect back, List<MemberRow> rows, int scroll) {}

  static Layout layout(int width, int height, List<MemberValue> members, int requestedScroll) {
    int safeWidth = Math.max(0, width);
    int safeHeight = Math.max(0, height);
    int searchWidth = Math.min(200, Math.max(0, safeWidth - 16));
    Rect search =
        safeHeight >= 44 && searchWidth >= 16
            ? new Rect((safeWidth - searchWidth) / 2, 24, searchWidth, 20)
            : new Rect(0, 0, 0, 0);
    Rect invite =
        safeHeight >= 44 && safeWidth >= 196
            ? new Rect(12, safeHeight - 24, 100, 20)
            : new Rect(0, 0, 0, 0);
    Rect back =
        safeHeight >= 44 && safeWidth >= 84
            ? new Rect(safeWidth - 72, safeHeight - 24, 60, 20)
            : new Rect(0, 0, 0, 0);
    int visible = safeWidth >= 24 ? Math.max(0, (safeHeight - 100) / ROW_HEIGHT) : 0;
    int scroll = Math.max(0, Math.min(requestedScroll, Math.max(0, members.size() - visible)));
    List<MemberRow> rows = new ArrayList<>();
    for (int index = 0; index < Math.min(visible, members.size() - scroll); index++) {
      int y = 66 + index * ROW_HEIGHT;
      Rect row = new Rect(12, y, Math.max(0, safeWidth - 24), 20);
      Rect remove =
          new Rect(Math.max(12, safeWidth - 82), y, Math.min(62, Math.max(0, safeWidth - 24)), 20);
      if (row.inside(safeWidth, safeHeight) && remove.inside(safeWidth, safeHeight))
        rows.add(new MemberRow(members.get(scroll + index), row, remove));
    }
    return new Layout(search, invite, back, List.copyOf(rows), scroll);
  }

  private TerritoryMembersLayout() {}
}
