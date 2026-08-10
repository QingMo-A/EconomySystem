package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Shared virtual layout for the shop purchase dialog. */
public final class ShopPurchaseLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int PANEL_WIDTH = 320;
  public static final int PANEL_HEIGHT = 160;
  public static final int PANEL_PADDING = 12;
  public static final int INPUT_WIDTH = 140;
  public static final int INPUT_MIN_WIDTH = 90;
  public static final int INPUT_HEIGHT = 20;
  public static final int BUTTON_WIDTH = 96;
  public static final int BUTTON_HEIGHT = 24;
  public static final int PANEL_BACKGROUND = 0xB01A2A3A;
  public static final int PANEL_BORDER = 0xFF4A8ACF;
  private ShopPurchaseLayout() {}
  public static Layout calculate(int physicalWidth, int physicalHeight, ShopPurchaseState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), cardWidth = PANEL_WIDTH, cardHeight = PANEL_HEIGHT;
    int x = Math.max(8, (width - cardWidth) / 2), y = Math.max(8, (height - cardHeight) / 2);
    UiRect card = new UiRect(x, y, cardWidth, cardHeight);
    UiRect item = new UiRect(x + cardWidth / 2 - 8, y + 34, 16, 16);
    UiRect quantityLabel = new UiRect(x + 28, y + 96, 74, 18);
    // Legacy positions the quantity field after the native translated label (typically 36px)
    // plus the 8px inline gap; at the reference Chinese/English fonts this is x + 112.
    UiRect quantity = new UiRect(x + 112, y + 98, Math.max(INPUT_MIN_WIDTH, Math.min(INPUT_WIDTH, cardWidth - 136)), INPUT_HEIGHT);
    UiRect confirm = new UiRect(x + (cardWidth - BUTTON_WIDTH) / 2, y + cardHeight - 36, BUTTON_WIDTH, BUTTON_HEIGHT);
    UiRect back = new UiRect(x + cardWidth - 74, y + cardHeight - 36, 62, 24);
    UiRect message = new UiRect(x + 12, y + 78, cardWidth - 24, 16);
    return new Layout(scale, card, item, quantityLabel, quantity, confirm, back, message);
  }
  public record Layout(UiScale scale, UiRect card, UiRect item, UiRect quantityLabel, UiRect quantity,
                       UiRect confirm, UiRect back, UiRect message) {}
}
