package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate card-grid geometry for the common shop page. */
public final class ShopLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int CARD_WIDTH = 100;
  public static final int CARD_HEIGHT = 80;
  public static final int CARD_SPACING = 8;
  public static final int PANEL_PADDING = 12;
  public static final int GRID_START_Y = 55;
  public static final int ROWS = 3;
  public static final int ICON_SIZE = 32;
  public static final int SEARCH_WIDTH = 200;
  public static final int SEARCH_HEIGHT = 20;
  public static final int PAGE_BUTTON_WIDTH = 50;
  public static final int PAGE_BUTTON_HEIGHT = 24;
  private ShopLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, ShopState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, ShopState state,
                                 UiTextMetrics metrics, float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = PANEL_PADDING, spacing = CARD_SPACING;
    int contentOffset = ShopOpenAnimation.contentOffset(animationProgress);
    int searchOffset = ShopOpenAnimation.searchOffset(animationProgress);
    int contentWidth = Math.max(1, width - panel * 2);
    int columns = Math.max(1, (contentWidth + spacing) / (CARD_WIDTH + spacing));
    int rows = ROWS;
    int pageSize = columns * rows;
    int totalGridWidth = columns * CARD_WIDTH + (columns - 1) * spacing;
    int gridStartX = panel + Math.max(0, (contentWidth - totalGridWidth) / 2);
    List<Card> cards = new ArrayList<>();
    List<ShopRow> visible = state.visibleRows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns, row = i / columns;
      int x = gridStartX + col * (CARD_WIDTH + spacing), y = GRID_START_Y + row * (CARD_HEIGHT + spacing) + contentOffset;
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect item = new UiRect(x + (CARD_WIDTH - ICON_SIZE) / 2, y + 26, ICON_SIZE, ICON_SIZE);
      cards.add(new Card(visible.get(i), card, item));
    }
    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    int pageTextX = width / 2 - pageTextWidth / 2;
    int controlsY = height - 40 + contentOffset;
    UiRect previous = new UiRect(pageTextX - PAGE_BUTTON_WIDTH - 12, controlsY,
        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
    // Legacy draws the page label at virtualHeight - 35 using the target font's native line
    // height; the buttons remain anchored at virtualHeight - 40.
    UiRect page = new UiRect(pageTextX, height - 35 + contentOffset, pageTextWidth,
        Math.max(1, metrics.lineHeight()));
    UiRect next = new UiRect(pageTextX + pageTextWidth + 12, controlsY,
        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
    UiRect search = new UiRect(panel, 20 - searchOffset, Math.min(SEARCH_WIDTH, Math.max(1, width - panel * 2)), SEARCH_HEIGHT);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2, search.width() + 8, search.height() + 4);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), GRID_START_Y + contentOffset + 50,
        Math.min(180, Math.max(1, width - panel * 2)), 24);
    int lineHeight = Math.max(1, metrics.lineHeight());
    UiRect title = new UiRect(panel, height - panel - lineHeight - 10 + contentOffset,
        120, lineHeight + 10);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight + contentOffset,
        90, lineHeight);
    return new Layout(scale, title, esc, search, searchBackground, List.copyOf(cards), previous, page, next,
        message, pageSize, columns, rows, metrics, Math.max(0f, Math.min(1f, animationProgress)));
  }

  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search, UiRect searchBackground,
                       List<Card> cards, UiRect previousButton, UiRect pageText, UiRect nextButton,
                       UiRect message, int pageSize, int columns, int rows, UiTextMetrics metrics,
                       float animationProgress) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record Card(ShopRow row, UiRect card, UiRect itemIcon) {}
}
