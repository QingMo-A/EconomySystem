package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate card-grid geometry for the common shop page. */
public final class ShopLayout {
  public static final int CARD_WIDTH = 100;
  public static final int CARD_HEIGHT = 82;
  private static final int GRID_START_Y = 55;
  private static final int FOOTER_HEIGHT = 28;
  private ShopLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, ShopState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING, spacing = EconomyUiTheme.CARD_SPACING;
    int columns = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int availableHeight = Math.max(CARD_HEIGHT, height - GRID_START_Y - FOOTER_HEIGHT - panel);
    int rows = Math.max(1, (availableHeight + spacing) / (CARD_HEIGHT + spacing));
    int pageSize = Math.max(1, columns * rows);
    List<Card> cards = new ArrayList<>();
    List<ShopRow> visible = state.visibleRows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns, row = i / columns;
      int x = panel + col * (CARD_WIDTH + spacing), y = GRID_START_Y + row * (CARD_HEIGHT + spacing);
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect buy = new UiRect(x + 8, y + CARD_HEIGHT - 21, CARD_WIDTH - 16, 17);
      UiRect item = new UiRect(x + CARD_WIDTH / 2 - 16, y + 22, 32, 32);
      cards.add(new Card(visible.get(i), card, item, buy));
    }
    int controlsY = Math.max(GRID_START_Y, height - panel - 24);
    UiRect previous = new UiRect(Math.max(panel, width / 2 - 110), controlsY, 48, 22);
    UiRect page = new UiRect(Math.max(panel, width / 2 - 28), controlsY, 56, 22);
    UiRect next = new UiRect(Math.min(Math.max(panel, width - panel - 48), width / 2 + 62), controlsY, 48, 22);
    UiRect search = new UiRect(panel, 20, Math.min(200, Math.max(1, width - panel * 2)), 20);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2, search.width() + 8, search.height() + 4);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), GRID_START_Y + 50,
        Math.min(180, Math.max(1, width - panel * 2)), 24);
    UiRect title = new UiRect(panel, height - panel - 14, Math.max(1, width / 2), 14);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - 14, 90, 14);
    return new Layout(scale, title, esc, search, searchBackground, List.copyOf(cards), previous, page, next,
        message, pageSize, columns, rows);
  }

  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search, UiRect searchBackground,
                       List<Card> cards, UiRect previousButton, UiRect pageText, UiRect nextButton,
                       UiRect message, int pageSize, int columns, int rows) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record Card(ShopRow row, UiRect card, UiRect itemIcon, UiRect buyButton) {}
}
