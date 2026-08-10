package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Shared virtual layout for the shop purchase dialog. */
public final class ShopPurchaseLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private ShopPurchaseLayout() {}
  public static Layout calculate(int physicalWidth, int physicalHeight, ShopPurchaseState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), cardWidth = Math.min(320, Math.max(220, width - 32)), cardHeight = 160;
    int x = Math.max(8, (width - cardWidth) / 2), y = Math.max(8, (height - cardHeight) / 2);
    UiRect card = new UiRect(x, y, cardWidth, cardHeight);
    UiRect item = new UiRect(x + cardWidth / 2 - 8, y + 34, 16, 16);
    UiRect quantityLabel = new UiRect(x + 28, y + 96, 74, 18);
    UiRect quantity = new UiRect(x + 110, y + 98, Math.max(90, cardWidth - 134), 20);
    UiRect confirm = new UiRect(x + (cardWidth - 96) / 2, y + cardHeight - 36, 96, 24);
    UiRect back = new UiRect(x + cardWidth - 74, y + cardHeight - 36, 62, 24);
    UiRect message = new UiRect(x + 12, y + 78, cardWidth - 24, 16);
    return new Layout(scale, card, item, quantityLabel, quantity, confirm, back, message);
  }
  public record Layout(UiScale scale, UiRect card, UiRect item, UiRect quantityLabel, UiRect quantity,
                       UiRect confirm, UiRect back, UiRect message) {}
}
