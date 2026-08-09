package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Centered, stable confirmation geometry in the shared virtual canvas. */
public final class MarketConfirmLayout {
  private MarketConfirmLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketConfirmState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int cardWidth = Math.min(360, Math.max(220, width - 32)), cardHeight = 174;
    int x = Math.max(8, (width - cardWidth) / 2), y = Math.max(8, (height - cardHeight) / 2);
    return new Layout(scale, new UiRect(x, y, cardWidth, cardHeight),
        new UiRect(x + 24, y + 38, 36, 36), new UiRect(x + 72, y + 38, cardWidth - 96, 36),
        new UiRect(x + cardWidth / 2 - 104, y + cardHeight - 34, 96, 22),
        new UiRect(x + cardWidth / 2 + 8, y + cardHeight - 34, 96, 22));
  }

  public record Layout(UiScale scale, UiRect card, UiRect item, UiRect details,
                       UiRect confirm, UiRect cancel) {}
}
