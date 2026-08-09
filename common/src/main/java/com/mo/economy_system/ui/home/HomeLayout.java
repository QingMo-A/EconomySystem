package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure geometry for the shared home dashboard. */
public final class HomeLayout {
  private static final int NAV_HEIGHT = 28;
  private static final int TOP_HEIGHT = 70;
  private static final int FOOTER_HEIGHT = 22;
  private HomeLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, HomeState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int leftWidth = Math.max(140, Math.min(220, Math.round(width * 0.25f)));
    int rightX = leftWidth + panel;
    int rightWidth = Math.max(1, width - rightX - panel);
    List<NavButton> nav = new ArrayList<>();
    int navWidth = Math.max(1, leftWidth - panel * 2);
    for (int index = 0; index < state.entries().size(); index++) {
      int y = panel + index * (NAV_HEIGHT + EconomyUiTheme.CARD_SPACING);
      nav.add(new NavButton(state.entries().get(index).route(),
          new UiRect(panel, y, navWidth, NAV_HEIGHT)));
    }
    int half = Math.max(1, (rightWidth - EconomyUiTheme.CARD_SPACING) / 2);
    UiRect balance = new UiRect(rightX, panel, half, TOP_HEIGHT);
    UiRect trade = new UiRect(rightX + half + EconomyUiTheme.CARD_SPACING, panel,
        Math.max(1, rightWidth - half - EconomyUiTheme.CARD_SPACING), TOP_HEIGHT);
    int leaderboardY = panel + TOP_HEIGHT + EconomyUiTheme.CARD_SPACING;
    int leaderboardHeight = Math.max(1, height - leaderboardY - panel - FOOTER_HEIGHT);
    UiRect leaderboard = new UiRect(rightX, leaderboardY, rightWidth, leaderboardHeight);
    int rowHeight = 18;
    int rowStart = leaderboardY + 38;
    int pageSize = Math.max(1, (leaderboardHeight - 48) / rowHeight);
    List<LeaderboardRow> rows = new ArrayList<>();
    List<AccountBalance> visible = state.visibleAccounts();
    int startRank = state.leaderboardOffset() + 1;
    for (int index = 0; index < visible.size() && index < pageSize; index++) {
      rows.add(new LeaderboardRow(visible.get(index), startRank + index,
          new UiRect(rightX + 10, rowStart + index * rowHeight,
              Math.max(1, rightWidth - 20), rowHeight)));
    }
    UiRect previous = new UiRect(rightX + Math.max(0, rightWidth / 2 - 76), height - panel - 20,
        58, 20);
    UiRect page = new UiRect(rightX + Math.max(0, rightWidth / 2 - 12), height - panel - 20, 24, 20);
    UiRect next = new UiRect(Math.min(width - panel - 58, rightX + rightWidth / 2 + 18),
        height - panel - 20, 58, 20);
    UiRect footer = new UiRect(panel, height - panel - FOOTER_HEIGHT,
        Math.max(1, leftWidth - panel * 2), FOOTER_HEIGHT);
    UiRect retry = new UiRect(rightX + Math.max(0, (rightWidth - 96) / 2),
        leaderboardY + Math.max(0, (leaderboardHeight - 22) / 2), Math.min(96, rightWidth), 22);
    return new Layout(scale, List.copyOf(nav), balance, trade, leaderboard, List.copyOf(rows),
        previous, page, next, footer, retry, pageSize);
  }

  public record Layout(UiScale scale, List<NavButton> navButtons, UiRect balanceCard,
                       UiRect tradeCard, UiRect leaderboardCard, List<LeaderboardRow> rows,
                       UiRect previousButton, UiRect pageText, UiRect nextButton,
                       UiRect footer, UiRect retryButton, int pageSize) {
    public Layout { navButtons = List.copyOf(navButtons); rows = List.copyOf(rows); }
  }
  public record NavButton(EconomyUiRoute route, UiRect rect) {}
  public record LeaderboardRow(AccountBalance account, int rank, UiRect rect) {}
}
