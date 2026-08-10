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
    int pad = EconomyUiTheme.PANEL_PADDING;
    int requestedHeight = state.terminal() ? 180 : 260;
    int cardWidth = Math.min(500, Math.max(260, width - pad * 2));
    int cardHeight = Math.min(requestedHeight, Math.max(160, height - pad * 2));
    int x = Math.max(pad, (width - cardWidth) / 2);
    int y = Math.max(pad, (height - cardHeight) / 2);
    UiRect card = new UiRect(x, y, cardWidth, cardHeight);
    int contentX = x + 18;
    int contentWidth = cardWidth - 36;
    int detailCount = state.terminal() ? 2 : 7;
    java.util.ArrayList<UiRect> details = new java.util.ArrayList<>(detailCount);
    for (int index = 0; index < detailCount; index++) {
      details.add(new UiRect(contentX, y + 48 + index * 19, contentWidth, 16));
    }
    int buttonY = card.bottom() - 34;
    UiRect primary = null;
    UiRect secondary = null;
    UiRect close = null;
    if (state.terminal()) {
      int buttonWidth = Math.max(90, Math.min(160, cardWidth - 72));
      close = new UiRect(x + (cardWidth - buttonWidth) / 2, buttonY, buttonWidth, 22);
    } else {
      int buttonWidth = Math.max(76, Math.min(138, (cardWidth - 46) / 2));
      primary = new UiRect(x + (cardWidth - buttonWidth * 2 - 10) / 2, buttonY, buttonWidth, 22);
      secondary = new UiRect(primary.right() + 10, buttonY, buttonWidth, 22);
    }
    return new Layout(
        scale,
        card,
        new UiRect(contentX, y + 16, contentWidth, 18),
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
