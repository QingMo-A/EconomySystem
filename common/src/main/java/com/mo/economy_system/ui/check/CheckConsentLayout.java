package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Virtual-coordinate layout shared by Forge and NeoForge consent shells. */
public final class CheckConsentLayout {
  private CheckConsentLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int cardWidth = Math.min(440, Math.max(220, width - EconomyUiTheme.PANEL_PADDING * 2));
    int cardHeight = Math.min(210, Math.max(150, height - EconomyUiTheme.PANEL_PADDING * 2));
    int x = Math.max(EconomyUiTheme.PANEL_PADDING, (width - cardWidth) / 2);
    int y = Math.max(EconomyUiTheme.PANEL_PADDING, (height - cardHeight) / 2);
    UiRect card = new UiRect(x, y, cardWidth, cardHeight);
    int buttonY = card.bottom() - 34;
    int buttonWidth = Math.max(68, Math.min(130, (cardWidth - 42) / 2));
    UiRect allow = new UiRect(x + (cardWidth - buttonWidth * 2 - 10) / 2, buttonY, buttonWidth, 22);
    UiRect decline = new UiRect(allow.right() + 10, buttonY, buttonWidth, 22);
    return new Layout(
        scale,
        card,
        new UiRect(x + 18, y + 14, cardWidth - 36, 18),
        new UiRect(x + 18, y + 50, cardWidth - 36, 16),
        new UiRect(x + 18, y + 72, cardWidth - 36, 16),
        new UiRect(x + 18, y + 94, cardWidth - 36, 16),
        new UiRect(x + 18, y + 122, cardWidth - 36, 16),
        new UiRect(x + 18, y + 140, cardWidth - 36, 16),
        allow,
        decline);
  }

  public record Layout(
      UiScale scale,
      UiRect card,
      UiRect title,
      UiRect requester,
      UiRect type,
      UiRect folder,
      UiRect dataNotice,
      UiRect noContentNotice,
      UiRect allow,
      UiRect decline) {}
}
