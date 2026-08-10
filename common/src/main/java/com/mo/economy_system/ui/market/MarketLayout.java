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
  private static final int GRID_START_Y = 55;
  private MarketLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state,
                                 UiTextMetrics metrics, float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), panel = EconomyUiTheme.PANEL_PADDING;
    int spacing = EconomyUiTheme.CARD_SPACING;
    float progress = Math.max(0f, Math.min(1f, animationProgress));
    int contentOffset = MarketOpenAnimation.contentOffset(progress);
    int topButtonOffset = MarketOpenAnimation.topButtonOffset(progress);
    int searchOffset = MarketOpenAnimation.searchOffset(progress);
    int columnCapacity = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int controlsY = Math.max(GRID_START_Y, height - panel - 28) + contentOffset;
    int availableHeight = Math.max(CARD_HEIGHT, controlsY - GRID_START_Y);
    int rowCapacity = Math.max(1, (availableHeight + spacing) / (CARD_HEIGHT + spacing));
    int pageSize = EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE;
    if (columnCapacity * rowCapacity < pageSize) {
      throw new IllegalStateException("virtual market viewport cannot expose a complete network page");
    }
    int columns = Math.min(pageSize, columnCapacity);
    int rows = (pageSize + columns - 1) / columns;
    List<Card> cards = new ArrayList<>();
    List<MarketRow> visible = state.rows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns, row = i / columns;
      int x = panel + col * (CARD_WIDTH + spacing), y = GRID_START_Y + row * (CARD_HEIGHT + spacing) + contentOffset;
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect icon = new UiRect(x + (CARD_WIDTH - 32) / 2, y + 26, 32, 32);
      UiRect action = new UiRect(x + CARD_WIDTH - 8 - 62, y + CARD_HEIGHT - 8 - 18, 62, 18);
      cards.add(new Card(visible.get(i), card, icon, action));
    }
    UiRect previous = new UiRect(Math.max(panel, width / 2 - 110), controlsY, 50, 24);
    UiRect page = new UiRect(Math.max(panel, width / 2 - 28), controlsY, 56, 24);
    UiRect next = new UiRect(Math.min(Math.max(panel, width - panel - 50), width / 2 + 62), controlsY, 50, 24);
    UiRect search = new UiRect(panel, 20 - searchOffset, Math.min(200, Math.max(1, width - panel * 2)), 20);
    int lineHeight = Math.max(1, metrics.lineHeight());
    int filterY = height - panel - lineHeight + contentOffset;
    List<FilterTab> filterTabs = new ArrayList<>();
    int filterX = panel;
    for (com.mo.economy_system.common.network.MarketOrderFilter value : com.mo.economy_system.common.network.MarketOrderFilter.values()) {
      int tabWidth = Math.max(38, metrics.width(filterLabel(value)) + 20);
      filterTabs.add(new FilterTab(value, new UiRect(filterX, filterY, tabWidth, lineHeight + 3)));
      filterX += tabWidth;
    }
    UiRect filter = new UiRect(panel, filterY, Math.max(1, filterX - panel), lineHeight + 3);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), GRID_START_Y + contentOffset + 50, Math.min(180, Math.max(1, width - panel * 2)), 24);
    UiRect title = new UiRect(panel, filterY, Math.max(1, metrics.width("Market")), lineHeight);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight + contentOffset, 90, lineHeight);
    int topButtonY = 18 - topButtonOffset;
    UiRect createSales = new UiRect(Math.max(panel, width - panel - 84 - 10 - 84), topButtonY, 84, 24);
    UiRect createDemand = new UiRect(Math.max(panel, width - panel - 84), topButtonY, 84, 24);
    return new Layout(scale, title, esc, search, filter, filterTabs, createSales, createDemand, List.copyOf(cards), previous, page, next, message, pageSize, columns, rows, metrics, progress);
  }
  private static String filterLabel(com.mo.economy_system.common.network.MarketOrderFilter value) {
    return switch (value) { case ALL -> "All"; case MINE -> "Mine"; case SALES -> "Sales"; case DEMAND -> "Demand"; };
  }
  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search, UiRect filter,
                       List<FilterTab> filterTabs, UiRect createSales, UiRect createDemand, List<Card> cards, UiRect previousButton,
                       UiRect pageText, UiRect nextButton, UiRect message, int pageSize, int columns, int rows,
                       UiTextMetrics metrics, float animationProgress) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record FilterTab(com.mo.economy_system.common.network.MarketOrderFilter filter, UiRect rect) {}
  public record Card(MarketRow row, UiRect card, UiRect itemIcon, UiRect actionButton) {}
}
