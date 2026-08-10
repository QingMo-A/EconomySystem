package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure, loader-neutral geometry for the shared Home dashboard. */
public final class HomeLayout {
  public static final int BASE_WIDTH = EconomyUiTheme.BASE_WIDTH;
  public static final int BASE_HEIGHT = EconomyUiTheme.BASE_HEIGHT;
  public static final float LEFT_PANEL_PERCENT = 0.25f;
  public static final int CARD_SPACING = EconomyUiTheme.CARD_SPACING;
  public static final int PANEL_PADDING = EconomyUiTheme.PANEL_PADDING;
  public static final int NAV_CARD_HEIGHT = 28;
  public static final int TOP_ROW_HEIGHT = 70;
  public static final int PANEL_ANIMATION_OFFSET = HomeOpenAnimation.PANEL_OFFSET;
  public static final int LEADERBOARD_VISIBLE_ROWS = 10;
  public static final int BACKGROUND_COLOR = 0x400A0A14;

  private HomeLayout() {}

  /** Uses the deterministic common metrics and the settled opening state. */
  public static Layout calculate(int physicalWidth, int physicalHeight, HomeState state) {
    return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  /** Calculates geometry using target-adapted text metrics and an eased animation progress. */
  public static Layout calculate(int physicalWidth, int physicalHeight, HomeState state,
                                 UiTextMetrics metrics, float animationProgress) {
    if (state == null) throw new IllegalArgumentException("state");
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, BASE_WIDTH, BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int leftWidth = (int) (width * LEFT_PANEL_PERCENT);
    // The reference geometry has no normal-viewport clamp. A minimum only prevents negative
    // rectangles in an unusually narrow viewport and never changes the 640x360 layout.
    leftWidth = Math.max(1, leftWidth);
    int rightX = leftWidth + PANEL_PADDING;
    int rightWidth = Math.max(1, width - rightX - PANEL_PADDING);
    int leftOffset = HomeOpenAnimation.leftOffset(animationProgress);
    int rightOffset = HomeOpenAnimation.rightOffset(animationProgress);

    List<NavButton> nav = new ArrayList<>();
    int navWidth = Math.max(1, leftWidth - PANEL_PADDING * 2);
    for (int index = 0; index < state.entries().size(); index++) {
      int y = PANEL_PADDING + index * (NAV_CARD_HEIGHT + CARD_SPACING);
      nav.add(new NavButton(state.entries().get(index).route(),
          new UiRect(PANEL_PADDING + leftOffset, y, navWidth, NAV_CARD_HEIGHT)));
    }

    int half = Math.max(1, (rightWidth - CARD_SPACING) / 2);
    UiRect balance = new UiRect(rightX + rightOffset, PANEL_PADDING, half, TOP_ROW_HEIGHT);
    UiRect trade = new UiRect(rightX + half + CARD_SPACING + rightOffset, PANEL_PADDING,
        Math.max(1, rightWidth - half - CARD_SPACING), TOP_ROW_HEIGHT);
    int leaderboardY = PANEL_PADDING + TOP_ROW_HEIGHT + CARD_SPACING;
    // Footer lives entirely in the left panel; it must not consume leaderboard height.
    int leaderboardHeight = Math.max(1, height - leaderboardY - PANEL_PADDING);
    UiRect leaderboard = new UiRect(rightX + rightOffset, leaderboardY, rightWidth,
        leaderboardHeight);

    int rowSpacing = Math.max(1, metrics.lineHeight() + 4);
    int titleBarHeight = 28;
    int leaderboardPadding = 10;
    int rowStart = leaderboardY + titleBarHeight + leaderboardPadding;
    int pageSize = Math.max(1, (leaderboardHeight - titleBarHeight - leaderboardPadding * 2)
        / rowSpacing);
    List<LeaderboardRow> rows = new ArrayList<>();
    List<AccountBalance> visible = state.visibleAccounts();
    int startRank = state.leaderboardOffset() + 1;
    int rowCount = Math.min(visible.size(), pageSize);
    for (int index = 0; index < rowCount; index++) {
      rows.add(new LeaderboardRow(visible.get(index), startRank + index,
          new UiRect(rightX + rightOffset + PANEL_PADDING - 2,
              rowStart + index * rowSpacing, Math.max(1, rightWidth - 20), rowSpacing)));
    }

    int footerHeight = Math.max(1, metrics.lineHeight() + 10);
    float footerScale = footerScale(metrics, leftWidth);
    int footerWidth = footerWidth(metrics, footerScale);
    // The layout owns the final version-card rectangle.  Keep the legacy content-width
    // calculation (including the 16px horizontal card padding) instead of handing HomeView a
    // left-panel placeholder that it would have to clamp a second time.
    UiRect footer = new UiRect(PANEL_PADDING + leftOffset,
        height - PANEL_PADDING - footerHeight, footerWidth, footerHeight);
    UiRect retry = new UiRect(rightX + rightOffset + Math.max(0, (rightWidth - 96) / 2),
        leaderboardY + Math.max(0, (leaderboardHeight - 22) / 2), Math.min(96, rightWidth), 22);
    return new Layout(scale, metrics, List.copyOf(nav), balance, trade, leaderboard,
        List.copyOf(rows), footer, retry, pageSize, footerScale,
        leftWidth, rightX + rightOffset, rightWidth);
  }

  private static float footerScale(UiTextMetrics metrics, int maxWidth) {
    int contentWidth = footerContentWidth(metrics);
    return Math.min(1.0f, maxWidth <= 0 ? 1.0f : (float) maxWidth / contentWidth);
  }

  private static int footerWidth(UiTextMetrics metrics, float scale) {
    // Match CardRenderer.drawVersionInfo exactly: truncate rather than round, and add card
    // padding after scaling the icon + text content.
    return Math.max(1, (int) (footerContentWidth(metrics) * scale) + 16);
  }

  private static int footerContentWidth(UiTextMetrics metrics) {
    return 14 + metrics.width("EconomySystem");
  }

  public record Layout(UiScale scale, UiTextMetrics metrics, List<NavButton> navButtons,
                       UiRect balanceCard, UiRect tradeCard, UiRect leaderboardCard,
                       List<LeaderboardRow> rows, UiRect footer, UiRect retryButton, int pageSize,
                       float footerScale, int leftPanelWidth, int rightPanelStartX,
                       int rightPanelWidth) {
    public Layout {
      navButtons = List.copyOf(navButtons);
      rows = List.copyOf(rows);
    }
  }
  public record NavButton(EconomyUiRoute route, UiRect rect) {}
  public record LeaderboardRow(AccountBalance account, int rank, UiRect rect) {}
}
