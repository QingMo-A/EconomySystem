package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiVersionInfoLayout;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate geometry for the 3/4 catalog + 1/4 inline purchase shop. */
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
  public static final int PANE_GAP = 8;
  public static final int PURCHASE_ICON_SIZE = 32;

  private ShopLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, ShopState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, ShopState state,
                                 UiTextMetrics metrics, float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int panel = PANEL_PADDING;
    int spacing = CARD_SPACING;
    int contentOffset = ShopOpenAnimation.contentOffset(animationProgress);
    int searchOffset = ShopOpenAnimation.searchOffset(animationProgress);

    int contentWidth = Math.max(1, width - panel * 2);
    int leftWidth = Math.max(360, (contentWidth - PANE_GAP) * 3 / 4);
    leftWidth = Math.min(leftWidth, Math.max(1, contentWidth - PANE_GAP - 120));
    int rightWidth = Math.max(1, contentWidth - PANE_GAP - leftWidth);
    int leftX = panel;
    int rightX = leftX + leftWidth + PANE_GAP;

    UiRect catalogArea = new UiRect(leftX, 48 + contentOffset, leftWidth,
        Math.max(1, height - 96));
    UiRect purchasePanel = new UiRect(rightX, 20 + contentOffset, rightWidth,
        Math.max(1, height - 60));

    int columns = Math.max(1,
        Math.min(4, (Math.max(1, leftWidth - panel * 2) + spacing) / (CARD_WIDTH + spacing)));
    int rows = ROWS;
    int pageSize = columns * rows;
    int totalGridWidth = columns * CARD_WIDTH + (columns - 1) * spacing;
    int gridStartX = leftX + Math.max(panel, (leftWidth - totalGridWidth) / 2);

    List<Card> cards = new ArrayList<>();
    List<ShopRow> visible = state.visibleRows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns;
      int row = i / columns;
      int x = gridStartX + col * (CARD_WIDTH + spacing);
      int y = GRID_START_Y + row * (CARD_HEIGHT + spacing) + contentOffset;
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect item = new UiRect(x + (CARD_WIDTH - ICON_SIZE) / 2, y + 26, ICON_SIZE, ICON_SIZE);
      cards.add(new Card(visible.get(i), card, item));
    }

    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    int leftCenter = leftX + leftWidth / 2;
    int pageTextX = leftCenter - pageTextWidth / 2;
    int controlsY = height - 40 + contentOffset;
    UiRect previous = new UiRect(pageTextX - PAGE_BUTTON_WIDTH - 12, controlsY,
        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
    UiRect page = new UiRect(pageTextX, height - 35 + contentOffset, pageTextWidth,
        Math.max(1, metrics.lineHeight()));
    UiRect next = new UiRect(pageTextX + pageTextWidth + 12, controlsY,
        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);

    UiRect search = new UiRect(leftX + panel, 20 - searchOffset,
        Math.min(SEARCH_WIDTH, Math.max(1, leftWidth - panel * 2)), SEARCH_HEIGHT);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2,
        search.width() + 8, search.height() + 4);
    UiRect message = new UiRect(leftX + Math.max(panel, (leftWidth - 180) / 2),
        GRID_START_Y + contentOffset + 50,
        Math.min(180, Math.max(1, leftWidth - panel * 2)), 24);

    int purchaseInnerX = purchasePanel.x() + 12;
    int purchaseInnerWidth = Math.max(1, purchasePanel.width() - 24);
    UiRect purchaseTitle = new UiRect(purchaseInnerX, purchasePanel.y() + 10,
        purchaseInnerWidth, 16);
    UiRect purchaseItem = new UiRect(
        purchasePanel.x() + Math.max(0, (purchasePanel.width() - PURCHASE_ICON_SIZE) / 2),
        purchasePanel.y() + 34, PURCHASE_ICON_SIZE, PURCHASE_ICON_SIZE);
    UiRect purchaseName = new UiRect(purchaseInnerX, purchasePanel.y() + 70,
        purchaseInnerWidth, 16);
    UiRect purchaseUnitPrice = new UiRect(purchaseInnerX, purchasePanel.y() + 91,
        purchaseInnerWidth, 14);
    UiRect purchaseBalance = new UiRect(purchaseInnerX, purchasePanel.y() + 108,
        purchaseInnerWidth, 14);
    UiRect purchaseCapacity = new UiRect(purchaseInnerX, purchasePanel.y() + 125,
        purchaseInnerWidth, 14);
    UiRect purchaseQuantity = new UiRect(purchaseInnerX, purchasePanel.y() + 148,
        purchaseInnerWidth, 20);
    UiRect purchaseTotal = new UiRect(purchaseInnerX, purchasePanel.y() + 176,
        purchaseInnerWidth, 16);
    UiRect purchaseMessage = new UiRect(purchaseInnerX, purchasePanel.y() + 198,
        purchaseInnerWidth, 30);
    UiRect purchaseConfirm = new UiRect(purchaseInnerX, purchasePanel.bottom() - 34,
        purchaseInnerWidth, 22);

    int lineHeight = Math.max(1, metrics.lineHeight());
    UiVersionInfoLayout.Result versionInfo = UiVersionInfoLayout.calculate(metrics,
        "screen.shop.title", List.of(), panel, height - panel + contentOffset, 120);
    UiRect title = versionInfo.card();
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90),
        height - panel - lineHeight + contentOffset, 90, lineHeight);

    return new Layout(scale, title, versionInfo.contentScale(), esc, search, searchBackground,
        catalogArea, purchasePanel, List.copyOf(cards), previous, page, next, message,
        purchaseTitle, purchaseItem, purchaseName, purchaseUnitPrice, purchaseBalance,
        purchaseCapacity, purchaseQuantity, purchaseTotal, purchaseMessage, purchaseConfirm,
        pageSize, columns, rows, metrics, Math.max(0f, Math.min(1f, animationProgress)));
  }

  public record Layout(
      UiScale scale,
      UiRect title,
      float versionInfoScale,
      UiRect esc,
      UiRect search,
      UiRect searchBackground,
      UiRect catalogArea,
      UiRect purchasePanel,
      List<Card> cards,
      UiRect previousButton,
      UiRect pageText,
      UiRect nextButton,
      UiRect message,
      UiRect purchaseTitle,
      UiRect purchaseItem,
      UiRect purchaseName,
      UiRect purchaseUnitPrice,
      UiRect purchaseBalance,
      UiRect purchaseCapacity,
      UiRect purchaseQuantity,
      UiRect purchaseTotal,
      UiRect purchaseMessage,
      UiRect purchaseConfirm,
      int pageSize,
      int columns,
      int rows,
      UiTextMetrics metrics,
      float animationProgress) {
    public Layout {
      cards = List.copyOf(cards);
    }
  }

  public record Card(ShopRow row, UiRect card, UiRect itemIcon) {}
}
