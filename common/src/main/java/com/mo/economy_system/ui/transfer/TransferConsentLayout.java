package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Shared virtual-coordinate layout for checked-file transfer consent. */
public final class TransferConsentLayout {
  private TransferConsentLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int pad = EconomyUiTheme.PANEL_PADDING;
    int cardWidth = Math.min(500, Math.max(260, width - pad * 2));
    int cardHeight = Math.min(260, Math.max(210, height - pad * 2));
    int x = Math.max(pad, (width - cardWidth) / 2);
    int y = Math.max(pad, (height - cardHeight) / 2);
    UiRect card = new UiRect(x, y, cardWidth, cardHeight);
    int contentX = x + 18;
    int contentWidth = cardWidth - 36;
    List<UiRect> details = List.of(
        new UiRect(contentX, y + 48, contentWidth, 16),
        new UiRect(contentX, y + 67, contentWidth, 16),
        new UiRect(contentX, y + 86, contentWidth, 16),
        new UiRect(contentX, y + 105, contentWidth, 16),
        new UiRect(contentX, y + 124, contentWidth, 16));
    UiRect warning = new UiRect(contentX, y + 150, contentWidth, 28);
    int buttonY = card.bottom() - 34;
    int buttonWidth = Math.max(76, Math.min(138, (cardWidth - 46) / 2));
    UiRect allow = new UiRect(x + (cardWidth - buttonWidth * 2 - 10) / 2, buttonY, buttonWidth, 22);
    UiRect decline = new UiRect(allow.right() + 10, buttonY, buttonWidth, 22);
    return new Layout(
        scale,
        card,
        new UiRect(contentX, y + 16, contentWidth, 18),
        details,
        warning,
        allow,
        decline);
  }

  public record Layout(
      UiScale scale,
      UiRect card,
      UiRect title,
      List<UiRect> details,
      UiRect warning,
      UiRect allow,
      UiRect decline) {
    public Layout {
      details = List.copyOf(details);
    }
  }
}
