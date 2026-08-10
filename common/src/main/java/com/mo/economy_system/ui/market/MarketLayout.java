package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure common geometry for the market list. */
public final class MarketLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int CARD_WIDTH = 200;
  public static final int CARD_HEIGHT = 80;
  public static final int CARD_SPACING = 8;
  public static final int CARD_PADDING = 8;
  public static final int GRID_START_Y = 55;
  public static final int ACTION_BUTTON_WIDTH = 62;
  public static final int ACTION_BUTTON_HEIGHT = 18;
  public static final int ADMIN_BUTTON_WIDTH = 72;
  public static final int ACTION_BUTTON_GAP = 6;
  public static final int ICON_SIZE = 32;
  public static final int ICON_OFFSET_Y = 26;
  private MarketLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state,
                                 UiTextMetrics metrics, float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), panel = EconomyUiTheme.PANEL_PADDING;
    int spacing = CARD_SPACING;
    float progress = Math.max(0f, Math.min(1f, animationProgress));
    int contentOffset = MarketOpenAnimation.contentOffset(progress);
    int topButtonOffset = MarketOpenAnimation.topButtonOffset(progress);
    int searchOffset = MarketOpenAnimation.searchOffset(progress);
    int columnCapacity = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int controlsY = Math.max(GRID_START_Y, height - panel - 28) + contentOffset;
    int availableHeight = Math.max(CARD_HEIGHT, controlsY - GRID_START_Y);
    int rowCapacity = Math.max(1, (availableHeight + spacing) / (CARD_HEIGHT + spacing));
    int pageSize = EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE;
    int columns = Math.min(pageSize, columnCapacity);
    int rows = (pageSize + columns - 1) / columns;
    List<Card> cards = new ArrayList<>();
    List<MarketRow> visible = state.rows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns, row = i / columns;
      int x = panel + col * (CARD_WIDTH + spacing), y = GRID_START_Y + row * (CARD_HEIGHT + spacing) + contentOffset;
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect icon = new UiRect(x + (CARD_WIDTH - ICON_SIZE) / 2, y + ICON_OFFSET_Y, ICON_SIZE, ICON_SIZE);
      UiRect action = new UiRect(x + CARD_WIDTH - CARD_PADDING - ACTION_BUTTON_WIDTH,
          y + CARD_HEIGHT - CARD_PADDING - ACTION_BUTTON_HEIGHT,
          ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
      UiRect admin = new UiRect(action.x() - ACTION_BUTTON_GAP - ADMIN_BUTTON_WIDTH, action.y(),
          ADMIN_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
      cards.add(new Card(visible.get(i), card, icon, action, admin));
    }
    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    int pageTextX = width / 2 - pageTextWidth / 2;
    UiRect previous = new UiRect(pageTextX - 50 - 12, controlsY, 50, 24);
    // Legacy Screen_Market draws the label five pixels above its 24px footer buttons;
    // keep the native line height so target font metrics determine the text hit rect.
    int lineHeight = Math.max(1, metrics.lineHeight());
    UiRect page = new UiRect(pageTextX, height - 35 + contentOffset, pageTextWidth, lineHeight);
    UiRect next = new UiRect(pageTextX + pageTextWidth + 12, controlsY, 50, 24);
    UiRect search = new UiRect(panel, 20 - searchOffset, Math.min(200, Math.max(1, width - panel * 2)), 20);
    int filterY = height - panel - lineHeight + contentOffset;
    List<FilterTab> filterTabs = new ArrayList<>();
    int filterX = panel;
    for (com.mo.economy_system.common.network.MarketOrderFilter value : com.mo.economy_system.common.network.MarketOrderFilter.values()) {
      int textWidth = Math.max(1, metrics.translatedWidth(filterKey(value), List.of()));
      UiRect textRect = new UiRect(filterX, filterY, textWidth, lineHeight + 3);
      UiRect hitRect = new UiRect(filterX, filterY - 2, textWidth + 1, lineHeight + 8);
      filterTabs.add(new FilterTab(value, textRect, hitRect));
      filterX += textWidth + 20;
    }
    UiRect filter = new UiRect(panel, filterY, Math.max(1, filterX - panel), lineHeight + 3);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), GRID_START_Y + contentOffset + 50, Math.min(180, Math.max(1, width - panel * 2)), 24);
    UiRect title = new UiRect(panel, filterY, 120, lineHeight + 10);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight + contentOffset, 90, lineHeight);
    int topButtonY = 18 - topButtonOffset;
    UiRect createSales = new UiRect(Math.max(panel, width - panel - 84), topButtonY, 84, 24);
    UiRect createDemand = new UiRect(Math.max(panel, width - panel - 84 - 10 - 84), topButtonY, 84, 24);
    return new Layout(scale, title, esc, search, filter, filterTabs, createSales, createDemand, List.copyOf(cards), previous, page, next, message, pageSize, columns, rows, metrics, progress);
  }
  private static String filterKey(com.mo.economy_system.common.network.MarketOrderFilter value) {
    return switch (value) {
      case ALL -> "screen.market.filter.all";
      case MINE -> "screen.market.filter.mine";
      case SALES -> "screen.market.filter.sales";
      case DEMAND -> "screen.market.filter.demand";
    };
  }
  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search, UiRect filter,
                       List<FilterTab> filterTabs, UiRect createSales, UiRect createDemand, List<Card> cards, UiRect previousButton,
                       UiRect pageText, UiRect nextButton, UiRect message, int pageSize, int columns, int rows,
                       UiTextMetrics metrics, float animationProgress) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record FilterTab(com.mo.economy_system.common.network.MarketOrderFilter filter,
                          UiRect textRect, UiRect hitRect) {}
  public record Card(MarketRow row, UiRect card, UiRect itemIcon, UiRect actionButton,
                     UiRect adminActionButton) {}
}
