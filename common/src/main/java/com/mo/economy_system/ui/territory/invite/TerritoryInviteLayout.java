package com.mo.economy_system.ui.territory.invite;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Shared invite directory geometry in the 640x360 virtual coordinate space. */
public final class TerritoryInviteLayout {
  private static final int PANEL_WIDTH = 420;
  private static final int PANEL_PADDING = 12;
  private static final int ROW_HEIGHT = 24;
  private TerritoryInviteLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryInviteState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, width - PANEL_PADDING * 2));
    int left = Math.max(PANEL_PADDING, (width - panelWidth) / 2);
    UiRect title = new UiRect(left, 12, panelWidth, 18);
    UiRect subtitle = new UiRect(left, 32, panelWidth, 16);
    UiRect search = new UiRect(left + PANEL_PADDING, 52,
        Math.max(1, panelWidth - PANEL_PADDING * 2), 20);
    int listY = 78;
    int footer = 36;
    int listHeight = Math.max(1, height - listY - footer - PANEL_PADDING);
    int pageSize = Math.max(1, listHeight / ROW_HEIGHT);
    int rowWidth = Math.max(1, panelWidth - PANEL_PADDING * 2);
    List<Row> rows = new ArrayList<>();
    List<com.mo.economy_system.common.network.PlayerSummary> visible = state.visiblePlayers();
    for (int i = 0; i < visible.size() && i < pageSize; i++) {
      int y = listY + i * ROW_HEIGHT;
      UiRect row = new UiRect(left + PANEL_PADDING, y, rowWidth, ROW_HEIGHT - 2);
      rows.add(new Row(visible.get(i), row,
          new UiRect(Math.max(row.x(), row.right() - 72), y + 1, Math.min(72, row.width()), 20)));
    }
    int navY = height - footer;
    UiRect previous = new UiRect(left + PANEL_PADDING, navY, 58, 20);
    UiRect page = new UiRect(left + Math.max(0, (panelWidth - 64) / 2), navY, 64, 20);
    UiRect next = new UiRect(Math.max(left + PANEL_PADDING, left + panelWidth - PANEL_PADDING - 58), navY, 58, 20);
    UiRect back = new UiRect(Math.max(0, width - PANEL_PADDING - 58), height - PANEL_PADDING - 20, 58, 20);
    UiRect retry = new UiRect(left + Math.max(0, (panelWidth - 100) / 2),
        listY + Math.max(0, (listHeight - 22) / 2), Math.min(100, panelWidth), 22);
    return new Layout(scale, title, subtitle, search, new UiRect(left, listY, panelWidth, listHeight),
        List.copyOf(rows), previous, page, next, back, retry, pageSize);
  }

  public record Layout(UiScale scale, UiRect title, UiRect subtitle, UiRect search, UiRect rows,
      List<Row> playerRows, UiRect previousButton, UiRect pageText, UiRect nextButton,
      UiRect backButton, UiRect retryButton, int pageSize) {
    public Layout { playerRows = List.copyOf(playerRows); }
  }

  public record Row(com.mo.economy_system.common.network.PlayerSummary player, UiRect row,
      UiRect inviteButton) {}
}
