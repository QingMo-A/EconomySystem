package com.mo.economy_system.screen.territory_system;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pure invite-list geometry shared by the Forge screen and headless tests. */
final class TerritoryInviteRowLayout {
  static final int ROW_HEIGHT = 26;

  record ButtonArea(int x, int y, int width, int height, UUID playerId) {
    boolean contains(double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
  }

  static List<ButtonArea> layout(List<UUID> ids, int scroll, int screenWidth, int screenHeight,
      int buttonWidth, int buttonHeight) {
    int first = Math.max(0, Math.min(scroll, Math.max(0, ids.size() - visibleCount(screenHeight))));
    int count = Math.min(visibleCount(screenHeight), ids.size() - first);
    List<ButtonArea> result = new ArrayList<>(Math.max(0, count));
    int x = screenWidth - buttonWidth - 24;
    for (int index = 0; index < count; index++) {
      result.add(new ButtonArea(x, 53 + index * ROW_HEIGHT, buttonWidth, buttonHeight,
          ids.get(first + index)));
    }
    return List.copyOf(result);
  }

  static int visibleCount(int screenHeight) {
    return Math.max(0, (screenHeight - 73) / ROW_HEIGHT);
  }

  static int clampScroll(int scroll, int rowCount, int screenHeight) {
    return Math.max(0, Math.min(scroll, Math.max(0, rowCount - visibleCount(screenHeight))));
  }

  private TerritoryInviteRowLayout() {}
}
