package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiVersionInfoLayout;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate layout for the territory card grid. */
public final class TerritoryListLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int CARD_WIDTH = 200;
  public static final int CARD_HEIGHT = 120;
  private static final int GRID_START_Y = 55;
  private static final int FOOTER_HEIGHT = 28;
  private static final int SEARCH_WIDTH = 200;

  private TerritoryListLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryListState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE);
  }

  /** Calculates geometry using the target's native font metrics. */
  public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryListState state,
                                 UiTextMetrics metrics) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int spacing = EconomyUiTheme.CARD_SPACING;
    int columns = Math.max(1, (width - panel * 2 + spacing) / (CARD_WIDTH + spacing));
    int availableHeight = Math.max(CARD_HEIGHT, height - GRID_START_Y - FOOTER_HEIGHT - panel);
    int rows = Math.max(1, (availableHeight + spacing) / (CARD_HEIGHT + spacing));
    int pageSize = Math.max(1, rows * columns);
    List<Card> cards = new ArrayList<>();
    List<TerritoryListRow> visible = state.visibleRows();
    for (int index = 0; index < visible.size() && index < pageSize; index++) {
      int column = index % columns;
      int row = index / columns;
      int x = panel + column * (CARD_WIDTH + spacing);
      int y = GRID_START_Y + row * (CARD_HEIGHT + spacing);
      UiRect card = new UiRect(x, y, CARD_WIDTH, CARD_HEIGHT);
      int buttonY = y + CARD_HEIGHT - 22;
      int innerWidth = CARD_WIDTH - 16;
      int buttonSpacing = 4;
      int single = Math.max(1, (innerWidth - buttonSpacing) / 2);
      UiRect teleport = new UiRect(x + 8, buttonY, visible.get(index).owned() ? single : innerWidth, 18);
      UiRect manage = visible.get(index).owned()
          ? new UiRect(x + 8 + single + buttonSpacing, buttonY, single, 18)
          : new UiRect(0, 0, 0, 0);
      cards.add(new Card(visible.get(index), card, teleport, manage));
    }
    int controlsY = Math.max(GRID_START_Y, height - panel - 24);
    UiRect previous = new UiRect(Math.max(panel, width / 2 - 112), controlsY, 50, 22);
    UiRect page = new UiRect(Math.max(panel, width / 2 - 28), controlsY, 56, 22);
    UiRect next = new UiRect(Math.min(Math.max(panel, width - panel - 50), width / 2 + 62), controlsY, 50, 22);
    UiRect retry = new UiRect(Math.max(panel, (width - 120) / 2),
        GRID_START_Y + Math.max(0, (height - GRID_START_Y - FOOTER_HEIGHT - 28) / 2),
        Math.min(120, Math.max(1, width - panel * 2)), 24);
    // List, shop and delivery screens all anchor their footer title cards to the same bottom
    // baseline.  Derive the card height from target font metrics instead of the old hard-coded
    // 31px offset (which left this title visibly above the other pages).
    UiVersionInfoLayout.Result versionInfo = UiVersionInfoLayout.calculate(metrics,
        "screen.territory.title", List.of(), panel, height - panel, 240);
    UiRect title = versionInfo.card();
    int lineHeight = Math.max(1, metrics.lineHeight());
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), height - panel - lineHeight,
        90, lineHeight);
    UiRect search = new UiRect(panel, 20, Math.min(SEARCH_WIDTH, Math.max(1, width - panel * 2)), 20);
    UiRect searchBackground = new UiRect(search.x() - 4, search.y() - 2,
        search.width() + 8, search.height() + 4);
    return new Layout(scale, title, esc, search, searchBackground, List.copyOf(cards),
        previous, page, next, retry, pageSize, columns, rows);
  }

  public record Layout(UiScale scale, UiRect title, UiRect esc, UiRect search,
                       UiRect searchBackground, List<Card> cards, UiRect previousButton,
                       UiRect pageText, UiRect nextButton, UiRect retryButton, int pageSize,
                       int columns, int rows) {
    public Layout {
      cards = List.copyOf(cards);
    }
  }

  public record Card(TerritoryListRow row, UiRect card, UiRect teleportButton,
                     UiRect manageButton) {}
}
