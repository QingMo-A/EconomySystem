package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Shared virtual layout for the shop purchase dialog. */
public final class ShopPurchaseLayout {
  private ShopPurchaseLayout() {}
  public static Layout calculate(int physicalWidth, int physicalHeight, ShopPurchaseState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), cardWidth = Math.min(330, Math.max(220, width - 32)), cardHeight = 184;
    int x = Math.max(8, (width - cardWidth) / 2), y = Math.max(8, (height - cardHeight) / 2);
    return new Layout(scale, new UiRect(x, y, cardWidth, cardHeight), new UiRect(x + cardWidth / 2 - 16, y + 38, 32, 32),
        new UiRect(x + 92, y + 91, cardWidth - 112, 20), new UiRect(x + cardWidth / 2 - 104, y + cardHeight - 34, 96, 22),
        new UiRect(x + cardWidth / 2 + 8, y + cardHeight - 34, 96, 22), new UiRect(x + 12, y + 120, cardWidth - 24, 18));
  }
  public record Layout(UiScale scale, UiRect card, UiRect item, UiRect quantity, UiRect confirm, UiRect back, UiRect message) {}
}
