package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure common geometry for the Market v2 browser + inline detail page. */
public final class MarketLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int PAGE_SIZE = EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE;
  public static final int COLUMNS = 3;
  public static final int ROWS = 3;
  public static final int PANE_GAP = 8;
  public static final int CARD_GAP = 7;
  public static final int CARD_HEIGHT = 66;
  public static final int ICON_SIZE = 24;
  public static final int DETAIL_ICON_SIZE = 32;
  public static final int TAB_GAP = 12;

  private MarketLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, MarketState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(
      int physicalWidth,
      int physicalHeight,
      MarketState state,
      UiTextMetrics metrics,
      float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int pagePad = EconomyUiTheme.PANEL_PADDING;
    float progress = Math.max(0f, Math.min(1f, animationProgress));
    int contentOffset = MarketOpenAnimation.contentOffset(progress);
    int searchOffset = MarketOpenAnimation.searchOffset(progress);
    int topButtonOffset = MarketOpenAnimation.topButtonOffset(progress);

    int contentWidth = Math.max(1, width - pagePad * 2);
    int detailWidth = Math.max(164, Math.round((contentWidth - PANE_GAP) * 0.29f));
    int catalogWidth = Math.max(1, contentWidth - PANE_GAP - detailWidth);
    int catalogX = pagePad;
    int detailX = catalogX + catalogWidth + PANE_GAP;
    int panelTop = 18 + contentOffset;
    int panelBottom = height - 20 + contentOffset;
    int panelHeight = Math.max(1, panelBottom - panelTop);

    UiRect catalogArea = new UiRect(catalogX, panelTop, catalogWidth, panelHeight);
    UiRect detailPanel = new UiRect(detailX, panelTop, detailWidth, panelHeight);

    int innerPad = 10;
    int topButtonY = 18 - topButtonOffset;
    int createWidth = 72;
    UiRect createSales = new UiRect(catalogArea.right() - innerPad - createWidth, topButtonY,
        createWidth, 22);
    UiRect createDemand = new UiRect(createSales.x() - 8 - createWidth, topButtonY,
        createWidth, 22);
    int searchMax = Math.max(90, createDemand.x() - 10 - (catalogX + innerPad));
    UiRect search = new UiRect(catalogX + innerPad, 20 - searchOffset,
        Math.min(190, searchMax), 20);

    int lineHeight = Math.max(1, metrics.lineHeight());
    int filterY = 46 + contentOffset;
    List<FilterTab> filterTabs = tabsForFilters(catalogX + innerPad, filterY, metrics);
    int sortY = filterY + lineHeight + 8;
    List<SortTab> sortTabs = tabsForSorts(catalogX + innerPad, sortY, metrics);

    int gridY = sortY + lineHeight + 11;
    int gridWidth = Math.max(1, catalogWidth - innerPad * 2);
    int cardWidth = Math.max(92, (gridWidth - CARD_GAP * (COLUMNS - 1)) / COLUMNS);
    List<Card> cards = new ArrayList<>();
    for (int index = 0; index < state.rows().size() && index < PAGE_SIZE; index++) {
      int col = index % COLUMNS;
      int row = index / COLUMNS;
      int x = catalogX + innerPad + col * (cardWidth + CARD_GAP);
      int y = gridY + row * (CARD_HEIGHT + CARD_GAP);
      UiRect card = new UiRect(x, y, cardWidth, CARD_HEIGHT);
      UiRect icon = new UiRect(x + 7, y + 22, ICON_SIZE, ICON_SIZE);
      UiRect typeBadge = new UiRect(x + cardWidth - 44, y + 6, 38, lineHeight + 2);
      UiRect name = new UiRect(x + 7, y + 6, Math.max(1, cardWidth - 58), lineHeight + 2);
      UiRect unitPrice = new UiRect(x + 38, y + 25, Math.max(1, cardWidth - 44), lineHeight + 2);
      UiRect summary = new UiRect(x + 38, y + 43, Math.max(1, cardWidth - 44), lineHeight + 2);
      cards.add(new Card(state.rows().get(index), card, icon, typeBadge, name, unitPrice, summary));
    }

    int footerY = Math.max(gridY + ROWS * CARD_HEIGHT + (ROWS - 1) * CARD_GAP + 8,
        catalogArea.bottom() - 28);
    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    int footerCenter = catalogX + catalogWidth / 2;
    UiRect pageText = new UiRect(footerCenter - pageTextWidth / 2, footerY + 5,
        pageTextWidth, lineHeight);
    UiRect previous = new UiRect(pageText.x() - 52 - 10, footerY, 52, 22);
    UiRect next = new UiRect(pageText.right() + 10, footerY, 52, 22);

    UiRect message = new UiRect(catalogX + innerPad, gridY,
        Math.max(1, catalogWidth - innerPad * 2),
        Math.max(1, footerY - gridY - 4));
    UiRect retryButton = new UiRect(
        message.x() + Math.max(0, (message.width() - 82) / 2),
        message.y() + Math.max(0, message.height() / 2 + 12), 82, 20);

    int dx = detailPanel.x() + 12;
    int dw = Math.max(1, detailPanel.width() - 24);
    UiRect detailTitle = new UiRect(dx, detailPanel.y() + 10, dw, lineHeight + 4);
    UiRect detailItem = new UiRect(
        detailPanel.x() + Math.max(0, (detailPanel.width() - DETAIL_ICON_SIZE) / 2),
        detailPanel.y() + 30, DETAIL_ICON_SIZE, DETAIL_ICON_SIZE);
    UiRect detailName = new UiRect(dx, detailPanel.y() + 66, dw, lineHeight + 4);
    UiRect detailType = new UiRect(dx, detailPanel.y() + 83, dw, lineHeight + 2);
    UiRect detailRemaining = new UiRect(dx, detailPanel.y() + 101, dw, lineHeight + 2);
    UiRect detailUnitPrice = new UiRect(dx, detailPanel.y() + 117, dw, lineHeight + 2);
    UiRect detailOrderTotal = new UiRect(dx, detailPanel.y() + 133, dw, lineHeight + 2);
    UiRect detailOwner = new UiRect(dx, detailPanel.y() + 149, dw, lineHeight + 2);
    UiRect detailFacts = new UiRect(dx, detailPanel.y() + 160, dw, lineHeight + 4);

    UiRect quantityInput = new UiRect(dx, detailPanel.y() + 178, dw, 19);
    int smallButton = 36;
    int quantityButtonsY = quantityInput.bottom() + 5;
    UiRect decrement = new UiRect(dx, quantityButtonsY, smallButton, 18);
    UiRect increment = new UiRect(decrement.right() + 6, quantityButtonsY, smallButton, 18);
    UiRect all = new UiRect(increment.right() + 6, quantityButtonsY,
        Math.max(34, detailPanel.right() - 12 - (increment.right() + 6)), 18);

    UiRect detailAmount = new UiRect(dx, quantityButtonsY + 24, dw, lineHeight + 4);
    UiRect detailError = new UiRect(dx, quantityButtonsY + 42, dw, lineHeight + 4);
    UiRect primaryAction = new UiRect(dx, detailPanel.bottom() - 34, dw, 22);
    UiRect secondaryAction = new UiRect(dx, primaryAction.y() - 27, dw, 20);

    UiRect esc = new UiRect(Math.max(pagePad, width - pagePad - 90),
        height - pagePad - lineHeight + contentOffset, 90, lineHeight);

    return new Layout(scale, catalogArea, detailPanel, esc, search,
        List.copyOf(filterTabs), List.copyOf(sortTabs), createSales, createDemand,
        List.copyOf(cards), previous, pageText, next, message, retryButton,
        detailTitle, detailItem, detailName, detailType, detailRemaining, detailUnitPrice,
        detailOrderTotal, detailOwner, detailFacts, quantityInput, decrement, increment, all,
        detailAmount, detailError, primaryAction, secondaryAction,
        PAGE_SIZE, COLUMNS, ROWS, metrics, progress);
  }

  private static List<FilterTab> tabsForFilters(int x, int y, UiTextMetrics metrics) {
    List<FilterTab> result = new ArrayList<>();
    int cursor = x;
    for (MarketOrderFilter value : MarketOrderFilter.values()) {
      String key = filterKey(value);
      int width = Math.max(1, metrics.translatedWidth(key, List.of()));
      UiRect text = new UiRect(cursor, y, width, metrics.lineHeight() + 3);
      UiRect hit = new UiRect(cursor - 2, y - 2, width + 4, metrics.lineHeight() + 7);
      result.add(new FilterTab(value, text, hit));
      cursor += width + TAB_GAP;
    }
    return result;
  }

  private static List<SortTab> tabsForSorts(int x, int y, UiTextMetrics metrics) {
    List<SortTab> result = new ArrayList<>();
    int cursor = x;
    for (MarketOrderSort value : MarketOrderSort.values()) {
      String key = sortKey(value);
      int width = Math.max(1, metrics.translatedWidth(key, List.of()));
      UiRect text = new UiRect(cursor, y, width, metrics.lineHeight() + 3);
      UiRect hit = new UiRect(cursor - 2, y - 2, width + 4, metrics.lineHeight() + 7);
      result.add(new SortTab(value, text, hit));
      cursor += width + 9;
    }
    return result;
  }

  public static String filterKey(MarketOrderFilter value) {
    return switch (value) {
      case ALL -> "screen.market.filter.all";
      case MINE -> "screen.market.filter.mine";
      case SALES -> "screen.market.filter.sales";
      case DEMAND -> "screen.market.filter.demand";
    };
  }

  public static String sortKey(MarketOrderSort value) {
    return switch (value) {
      case DEFAULT -> "screen.market.sort.default";
      case UNIT_PRICE_ASC -> "screen.market.sort.unit_asc";
      case UNIT_PRICE_DESC -> "screen.market.sort.unit_desc";
      case NEWEST -> "screen.market.sort.newest";
      case EXPIRING_SOON -> "screen.market.sort.expiring";
    };
  }

  public record Layout(
      UiScale scale,
      UiRect catalogArea,
      UiRect detailPanel,
      UiRect esc,
      UiRect search,
      List<FilterTab> filterTabs,
      List<SortTab> sortTabs,
      UiRect createSales,
      UiRect createDemand,
      List<Card> cards,
      UiRect previousButton,
      UiRect pageText,
      UiRect nextButton,
      UiRect message,
      UiRect retryButton,
      UiRect detailTitle,
      UiRect detailItem,
      UiRect detailName,
      UiRect detailType,
      UiRect detailRemaining,
      UiRect detailUnitPrice,
      UiRect detailOrderTotal,
      UiRect detailOwner,
      UiRect detailFacts,
      UiRect quantityInput,
      UiRect decrement,
      UiRect increment,
      UiRect all,
      UiRect detailAmount,
      UiRect detailError,
      UiRect primaryAction,
      UiRect secondaryAction,
      int pageSize,
      int columns,
      int rows,
      UiTextMetrics metrics,
      float animationProgress) {
    public Layout {
      filterTabs = List.copyOf(filterTabs);
      sortTabs = List.copyOf(sortTabs);
      cards = List.copyOf(cards);
    }
  }

  public record FilterTab(MarketOrderFilter filter, UiRect textRect, UiRect hitRect) {}
  public record SortTab(MarketOrderSort sort, UiRect textRect, UiRect hitRect) {}
  public record Card(
      MarketRow row,
      UiRect card,
      UiRect itemIcon,
      UiRect typeBadge,
      UiRect name,
      UiRect unitPrice,
      UiRect summary) {}
}
