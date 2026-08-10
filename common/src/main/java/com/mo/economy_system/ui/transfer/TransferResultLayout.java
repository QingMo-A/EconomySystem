package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Shared virtual-coordinate layout for artifact decisions and terminal transfer messages. */
public final class TransferResultLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private TransferResultLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, TransferResultState state) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    // Legacy result pages were plain text screens.  The full viewport record keeps
    // existing interaction invariants while the view deliberately emits no card.
    UiRect card = new UiRect(0, 0, width, height);
    int contentX = 12;
    int contentWidth = Math.max(1, width - 24);
    int detailCount = state.terminal() ? 2 : 7;
    java.util.ArrayList<UiRect> details = new java.util.ArrayList<>(detailCount);
    for (int index = 0; index < detailCount; index++) {
      details.add(new UiRect(contentX, 42 + index * 12, contentWidth, 12));
    }
    int buttonY = Math.max(0, height - 25);
    UiRect primary = null;
    UiRect secondary = null;
    UiRect close = null;
    if (state.terminal()) {
      close = new UiRect(Math.max(0, width / 2 - 50), buttonY, 100, 20);
    } else {
      primary = new UiRect(width / 2 - 105, buttonY, 100, 20);
      secondary = new UiRect(width / 2 + 5, buttonY, 100, 20);
    }
    return new Layout(
        scale,
        card,
        new UiRect(0, 18, width, 14),
        List.copyOf(details),
        primary,
        secondary,
        close);
  }

  public record Layout(
      UiScale scale,
      UiRect card,
      UiRect title,
      List<UiRect> details,
      UiRect primary,
      UiRect secondary,
      UiRect close) {
    public Layout {
      details = List.copyOf(details);
    }
  }
}
