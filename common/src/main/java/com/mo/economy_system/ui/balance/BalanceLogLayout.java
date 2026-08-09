package com.mo.economy_system.ui.balance;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure virtual-coordinate layout for the balance-log page. */
public final class BalanceLogLayout {
  private static final int TAB_HEIGHT = 22;
  private static final int ROW_HEIGHT = 24;

  private BalanceLogLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, BalanceLogState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    UiRect panelRect = new UiRect(panel, panel, Math.max(1, width - panel * 2), Math.max(1, height - panel * 2));
    UiRect title = new UiRect(panel + 10, panel + 8, Math.max(1, width / 2), 14);
    UiRect esc = new UiRect(Math.max(panel, width - panel - 90), panel + 8, 90, 14);
    int tabX = panel + 10, tabY = panel + 28;
    int tabWidth = Math.max(52, (width - panel * 2 - 20) / BalanceLogState.CATEGORIES.size());
    List<Tab> tabs = new ArrayList<>();
    for (int i = 0; i < BalanceLogState.CATEGORIES.size(); i++) {
      tabs.add(new Tab(BalanceLogState.CATEGORIES.get(i),
          new UiRect(tabX + i * tabWidth, tabY, tabWidth - 4, TAB_HEIGHT)));
    }
    int rowsY = panel + 58;
    int controlsY = Math.max(rowsY, height - panel - 30);
    int rowHeight = Math.max(1, controlsY - rowsY - 6);
    int visibleRows = Math.max(1, rowHeight / ROW_HEIGHT);
    List<Row> rows = new ArrayList<>();
    List<BalanceLogRow> visible = state.visibleEntries();
    int rowWidth = Math.max(1, width - panel * 2 - 20);
    for (int i = 0; i < visible.size() && i < visibleRows; i++) {
      rows.add(new Row(visible.get(i), new UiRect(panel + 10, rowsY + i * ROW_HEIGHT,
          rowWidth, ROW_HEIGHT - 2)));
    }
    int buttonWidth = 58, buttonHeight = 20;
    UiRect previous = new UiRect(panel + 10, controlsY, buttonWidth, buttonHeight);
    UiRect next = new UiRect(width - panel - 10 - buttonWidth, controlsY, buttonWidth, buttonHeight);
    UiRect page = new UiRect(width / 2 - 90, controlsY, 180, buttonHeight);
    UiRect message = new UiRect(Math.max(panel + 10, (width - 200) / 2), rowsY + 28,
        Math.min(200, Math.max(1, width - panel * 2 - 20)), 24);
    return new Layout(scale, panelRect, title, esc, List.copyOf(tabs), List.copyOf(rows),
        previous, page, next, message, visibleRows);
  }

  public record Layout(UiScale scale, UiRect panel, UiRect title, UiRect esc, List<Tab> tabs,
                       List<Row> rows, UiRect previousButton, UiRect pageText, UiRect nextButton,
                       UiRect message, int visibleRows) {
    public Layout { tabs = List.copyOf(tabs); rows = List.copyOf(rows); }
  }

  public record Tab(String category, UiRect rect) {}
  public record Row(BalanceLogRow row, UiRect rect) {}
}
