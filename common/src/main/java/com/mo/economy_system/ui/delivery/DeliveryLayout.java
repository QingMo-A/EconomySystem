package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate layout for the delivery-box card grid. */
public final class DeliveryLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int CARD_WIDTH = 200;
  public static final int CARD_HEIGHT = 70;
  private static final int GRID_START_Y = 55;
  private static final int FOOTER_HEIGHT = 40;

  private DeliveryLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, DeliveryState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, DeliveryState state,
                                 UiTextMetrics metrics) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING, spacing = EconomyUiTheme.CARD_SPACING;
    int columns = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int rows = 2;
    int pageSize = Math.max(1, columns * rows);
    List<Card> cards = new ArrayList<>();
    List<DeliveryRow> visible = state.visibleRows();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int col = i % columns, row = i / columns;
      int x = panel + col * (CARD_WIDTH + spacing);
      int y = GRID_START_Y + row * (CARD_HEIGHT + spacing);
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      UiRect item = new UiRect(x + 8, y + (CARD_HEIGHT - 32) / 2, 32, 32);
      UiRect claim = new UiRect(x + CARD_WIDTH - 8 - 60, y + CARD_HEIGHT - 8 - 18, 60, 18);
      cards.add(new Card(visible.get(i), card, item, claim));
    }
    int controlsY = Math.max(GRID_START_Y, height - FOOTER_HEIGHT);
    String pageTextValue = (state.page() + 1) + " / " + state.totalPages();
    int pageTextWidth = Math.max(1, metrics.width(pageTextValue));
    int pageTextX = width / 2 - pageTextWidth / 2;
    UiRect previous = new UiRect(Math.max(panel, pageTextX - 50 - 12), controlsY, 50, 24);
    int lineHeight = Math.max(1, metrics.lineHeight());
    // Legacy Screen_DeliveryBox draws the page label at virtualHeight - 35,
    // while the adjacent arrow buttons remain anchored at virtualHeight - 40.
    UiRect page = new UiRect(Math.max(panel, pageTextX), height - 35, pageTextWidth, lineHeight);
    UiRect next = new UiRect(Math.min(Math.max(panel, width - panel - 50), page.x() + page.width() + 12), controlsY, 50, 24);
    UiRect search = new UiRect(panel, 20, Math.min(200, Math.max(1, width - panel * 2)), 20);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2,
        search.width() + 8, search.height() + 4);
    UiRect message = new UiRect(Math.max(panel, (width - 180) / 2), Math.max(GRID_START_Y, height / 2 - 12),
        Math.min(180, Math.max(1, width - panel * 2)), 24);
    int titleHeight = lineHeight + 10;
    UiRect title = new UiRect(panel, Math.max(panel, height - panel - titleHeight), 140, titleHeight);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight, 90, lineHeight);
    return new Layout(scale, title, esc, search, searchBackground, List.copyOf(cards), previous,
        page, next, message, pageSize, columns, rows, metrics);
  }

  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search,
                       UiRect searchBackground, List<Card> cards, UiRect previousButton,
                       UiRect pageText, UiRect nextButton, UiRect message,
                       int pageSize, int columns, int rows, UiTextMetrics metrics) {
    public Layout { cards = List.copyOf(cards); }
  }

  public record Card(DeliveryRow row, UiRect card, UiRect itemIcon, UiRect claimButton) {}
}
