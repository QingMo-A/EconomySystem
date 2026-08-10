package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Centered, stable confirmation geometry in the shared virtual canvas. */
public final class MarketConfirmLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private MarketConfirmLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketConfirmState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int cardWidth = Math.min(360, Math.max(220, width - 32)), cardHeight = 200;
    int x = Math.max(8, (width - cardWidth) / 2), y = Math.max(8, (height - cardHeight) / 2);
    return new Layout(scale, new UiRect(x, y, cardWidth, cardHeight),
        new UiRect(x + 20, y + 16, 32, 32), new UiRect(x + 20, y + 68, cardWidth - 40, 50),
        new UiRect(x + cardWidth / 2 - 146, y + cardHeight - 40, 140, 24),
        new UiRect(x + cardWidth / 2 + 6, y + cardHeight - 40, 140, 24));
  }

  public record Layout(UiScale scale, UiRect card, UiRect item, UiRect details,
                       UiRect confirm, UiRect cancel) {}
}
