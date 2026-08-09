package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure common geometry for the market list. */
public final class MarketLayout {
  public static final int CARD_WIDTH = 180;
  public static final int CARD_HEIGHT = 80;
  private static final int GRID_START_Y = 62;
  private MarketLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight(), panel = EconomyUiTheme.PANEL_PADDING;
    int spacing = EconomyUiTheme.CARD_SPACING;
    int columnCapacity = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int controlsY = Math.max(GRID_START_Y, height - panel - 24);
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
      int x = panel + col * (CARD_WIDTH + spacing), y = GRID_START_Y + row * (CARD_HEIGHT + spacing);
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect icon = new UiRect(x + 8, y + 25, 32, 32);
      UiRect action = new UiRect(x + CARD_WIDTH - 64, y + CARD_HEIGHT - 22, 56, 17);
      cards.add(new Card(visible.get(i), card, icon, action));
    }
    UiRect previous = new UiRect(Math.max(panel, width / 2 - 110), controlsY, 48, 22);
    UiRect page = new UiRect(Math.max(panel, width / 2 - 28), controlsY, 56, 22);
    UiRect next = new UiRect(Math.min(Math.max(panel, width - panel - 48), width / 2 + 62), controlsY, 48, 22);
    UiRect search = new UiRect(panel, 20, Math.min(200, Math.max(1, width - panel * 2)), 20);
    UiRect filter = new UiRect(panel + 210, 20, Math.min(180, Math.max(1, width - panel * 2 - 210)), 20);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), GRID_START_Y + 50, Math.min(180, Math.max(1, width - panel * 2)), 24);
    UiRect title = new UiRect(panel, height - panel - 14, Math.max(1, width / 2), 14);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - 14, 90, 14);
    UiRect createSales = new UiRect(Math.max(panel, width - panel - 180), 20, 82, 20);
    UiRect createDemand = new UiRect(Math.max(panel, width - panel - 92), 20, 80, 20);
    return new Layout(scale, title, esc, search, filter, createSales, createDemand, List.copyOf(cards), previous, page, next, message, pageSize, columns, rows);
  }
  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search, UiRect filter,
                       UiRect createSales, UiRect createDemand, List<Card> cards, UiRect previousButton,
                       UiRect pageText, UiRect nextButton, UiRect message, int pageSize, int columns, int rows) {
    public Layout { cards = List.copyOf(cards); }
  }
  public record Card(MarketRow row, UiRect card, UiRect itemIcon, UiRect actionButton) {}
}
