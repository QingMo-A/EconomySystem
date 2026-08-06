package com.mo.economy_system.screen.territory_system;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class TerritoryTeleportRowLayout {
  static final int ROW_HEIGHT = 26;
  enum Action { TELEPORT, MANAGE, DELETE }

  record TerritoryRow(UUID territoryId, boolean owned) {}

  record ButtonArea(int x, int y, int width, int height, UUID territoryId) {
    boolean contains(double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
  }

  record ActionArea(int x, int y, int width, int height, UUID territoryId, Action action) {
    boolean contains(double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
  }

  static List<ButtonArea> layout(List<UUID> ids, int scroll, int screenWidth, int screenHeight,
      int buttonWidth, int buttonHeight) {
    int first = Math.max(0, Math.min(scroll, Math.max(0, ids.size() - visibleCount(screenHeight))));
    int count = Math.min(visibleCount(screenHeight), ids.size() - first);
    List<ButtonArea> result = new ArrayList<>(count);
    int x = screenWidth - buttonWidth - 24;
    for (int index = 0; index < count; index++)
      result.add(new ButtonArea(x, 53 + index * ROW_HEIGHT, buttonWidth, buttonHeight, ids.get(first + index)));
    return List.copyOf(result);
  }

  /** Authorized rows teleport; owned rows also expose management and deletion. */
  static List<ActionArea> layoutActions(List<TerritoryRow> rows, int scroll, int screenWidth,
      int screenHeight, int buttonWidth, int buttonHeight) {
    int first = Math.max(0, Math.min(scroll,
        Math.max(0, rows.size() - visibleCount(screenHeight))));
    int count = Math.min(visibleCount(screenHeight), rows.size() - first);
    List<ActionArea> result = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      TerritoryRow row = rows.get(first + index);
      int y = 53 + index * ROW_HEIGHT;
      if (row.owned()) {
        int deleteWidth=28;
        int totalWidth = buttonWidth * 2 + deleteWidth + 8;
        int x = screenWidth - totalWidth - 24;
        result.add(new ActionArea(x, y, buttonWidth, buttonHeight,
            row.territoryId(), Action.TELEPORT));
        result.add(new ActionArea(x + buttonWidth + 4, y, buttonWidth, buttonHeight,
            row.territoryId(), Action.MANAGE));
        result.add(new ActionArea(x + buttonWidth * 2 + 8, y, deleteWidth, buttonHeight,
            row.territoryId(), Action.DELETE));
      } else {
        int x = screenWidth - buttonWidth - 24;
        result.add(new ActionArea(x, y, buttonWidth, buttonHeight,
            row.territoryId(), Action.TELEPORT));
      }
    }
    return List.copyOf(result);
  }

  static int visibleCount(int screenHeight) { return Math.max(0, (screenHeight - 73) / ROW_HEIGHT); }
  static int clampScroll(int scroll, int rowCount, int screenHeight) {
    return Math.max(0, Math.min(scroll, Math.max(0, rowCount - visibleCount(screenHeight))));
  }
  private TerritoryTeleportRowLayout() {}
}
