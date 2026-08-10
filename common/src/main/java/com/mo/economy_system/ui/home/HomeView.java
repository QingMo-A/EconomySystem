package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.text.UiTextSpan;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.List;

/** Semantic Home dashboard view shared by both Minecraft targets. */
public final class HomeView {
  private static final int TITLE_COLOR = 0xFFFFFFFF;
  private static final int SEPARATOR_COLOR = 0xFF4A5568;
  private static final int MUTED_WHITE = 0xB0FFFFFF;
  private static final int BALANCE_UNIT_COLOR = 0xB0FFFFFF;
  private static final int RANK_GOLD = 0xFFFFD700;
  private static final int RANK_SILVER = 0xFFC0C0C0;
  private static final int RANK_BRONZE = 0xFFCD7F32;
  private static final int RANK_OTHER = 0xFF888888;

  private HomeView() {}

  public static void render(EconomyUiRenderer renderer, HomeState state,
                            HomeLayout.Layout layout, int mouseX, int mouseY) {
    drawNavigation(renderer, state, layout, mouseX, mouseY);
    drawBalanceCard(renderer, state, layout, mouseX, mouseY);
    drawTradeCard(renderer, state, layout, mouseX, mouseY);
    drawLeaderboard(renderer, state, layout, mouseX, mouseY);
    drawFooter(renderer, layout);
  }

  private static void drawNavigation(EconomyUiRenderer renderer, HomeState state,
                                     HomeLayout.Layout layout, int mouseX, int mouseY) {
    for (HomeLayout.NavButton nav : layout.navButtons()) {
      EconomyUiMenu.Entry entry = state.entries().stream()
          .filter(value -> value.route() == nav.route()).findFirst().orElse(null);
      if (entry == null) continue;
      // Navigation remains usable while data cards are loading or in an error state.
      renderer.translatedIconButton(nav.rect(), navStyle(nav.route()), navIcon(nav.route()),
          entry.labelKey(), List.of(), nav.rect().contains(mouseX, mouseY), true);
    }
  }

  private static void drawBalanceCard(EconomyUiRenderer renderer, HomeState state,
                                      HomeLayout.Layout layout, int mouseX, int mouseY) {
    UiRect card = layout.balanceCard();
    boolean hovered = card.contains(mouseX, mouseY);
    renderer.card(card, EconomyUiTheme.HOME_BALANCE_CARD, hovered);
    int titleY = card.y() + 8;
    renderer.icon(UiIcon.BALANCE, new UiRect(card.x() + 8, titleY - 1,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_SIZE));
    renderer.translatedText("screen.home.balance", List.of(), card.x() + 21, titleY, TITLE_COLOR);

    String rankText = hovered ? "日志 >>" : (state.playerRank() > 0 ? "#" + state.playerRank() : "--");
    int rankX = card.right() - 8 - layout.metrics().width(rankText);
    renderer.text(rankText, rankX, titleY, hovered ? RANK_GOLD : 0xFFAAFFAA);

    int separatorY = titleY + layout.metrics().lineHeight() + 3;
    renderer.fill(new UiRect(card.x() + 3, separatorY, Math.max(0, card.width() - 4), 1),
        SEPARATOR_COLOR);
    int balanceY = separatorY + 6;
    String balanceText = UiNumbers.formatInteger(state.balance());
    renderer.textInRect(balanceText,
        new UiRect(card.x() + 8, balanceY,
            Math.max(1, card.width() - 16), layout.metrics().lineHeight()),
        EconomyUiTheme.HOME_BALANCE_ACCENT, UiTextAlignment.CENTER);
    renderer.textInRect("梦鱼币",
        new UiRect(card.x() + 8, balanceY + layout.metrics().lineHeight() + 2,
            Math.max(1, card.width() - 16), layout.metrics().lineHeight()),
        BALANCE_UNIT_COLOR, UiTextAlignment.CENTER);
  }

  private static void drawTradeCard(EconomyUiRenderer renderer, HomeState state,
                                    HomeLayout.Layout layout, int mouseX, int mouseY) {
    UiRect card = layout.tradeCard();
    boolean hovered = card.contains(mouseX, mouseY);
    renderer.card(card, EconomyUiTheme.HOME_TRADE_CARD, hovered);
    int titleY = card.y() + 8;
    renderer.icon(UiIcon.TRADE, new UiRect(card.x() + 8, titleY - 1,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_SIZE));
    renderer.translatedText("screen.home.trade", List.of(), card.x() + 21, titleY, TITLE_COLOR);
    if (hovered) {
      String hint = "点击查看 >>";
      renderer.text(hint, card.right() - 8 - layout.metrics().width(hint), titleY, 0xFF4FC3F7);
    }

    int separatorY = titleY + layout.metrics().lineHeight() + 3;
    renderer.fill(new UiRect(card.x() + 3, separatorY, Math.max(0, card.width() - 4), 1),
        SEPARATOR_COLOR);
    int statsY = separatorY + 6;
    int colWidth = Math.max(1, (card.width() - 16) / 2);
    int leftX = card.x() + 8;
    int rightX = leftX + colWidth;
    renderer.text("卖单", leftX, statsY, MUTED_WHITE);
    String sell = UiNumbers.formatInteger(state.sellOrders());
    renderer.text(sell, leftX + colWidth - layout.metrics().width(sell), statsY,
        0xFFFFAA00);
    renderer.text("求购", rightX, statsY, MUTED_WHITE);
    String buy = UiNumbers.formatInteger(state.demandOrders());
    renderer.text(buy, card.right() - 8 - layout.metrics().width(buy), statsY, 0xFF00FFFF);
  }

  private static void drawLeaderboard(EconomyUiRenderer renderer, HomeState state,
                                      HomeLayout.Layout layout, int mouseX, int mouseY) {
    UiRect card = layout.leaderboardCard();
    // The leaderboard is a static reference card; rows do not change its hover chrome.
    renderer.card(card, EconomyUiTheme.HOME_LEADERBOARD_CARD, false);
    renderer.icon(UiIcon.LEADERBOARD, new UiRect(card.x() + 10, card.y() + 9,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_SIZE));
    renderer.translatedTextInRect("screen.home.leaderboard", List.of(),
        new UiRect(card.x() + 24, card.y() + 10,
            Math.max(1, card.width() - 34), layout.metrics().lineHeight()),
        TITLE_COLOR, UiTextAlignment.LEFT);
    renderer.fill(new UiRect(card.x() + 3, card.y() + 28,
        Math.max(0, card.width() - 4), 1), SEPARATOR_COLOR);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.home.loading", List.of(), layout.retryButton(),
          TITLE_COLOR, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.home.sync_failed" : state.errorKey(),
          List.of(), layout.retryButton(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.home.retry", List.of(), layout.retryButton().contains(mouseX, mouseY), true);
    } else if (state.accounts().isEmpty()) {
      renderer.translatedTextInRect("screen.home.leaderboard.empty", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    } else {
      for (HomeLayout.LeaderboardRow row : layout.rows()) {
        boolean self = state.isSelf(row.account());
        int rankColor = self ? RANK_GOLD : rankColor(row.rank());
        // Keep the legacy entry as one draw call.  This preserves the reference space width and
        // avoids approximating a separator between independently positioned rank/name strings.
        String entryText = "#" + row.rank() + " " + row.account().playerName();
        renderer.text(entryText, row.rect().x(), row.rect().y(), rankColor);
        String balance = UiNumbers.formatInteger(row.account().balance());
        renderer.text(balance, card.right() - 10 - layout.metrics().width(balance),
            row.rect().y(), self ? RANK_GOLD : MUTED_WHITE);
      }
    }
  }

  private static int rankColor(int rank) {
    return switch (rank) {
      case 1 -> RANK_GOLD;
      case 2 -> RANK_SILVER;
      case 3 -> RANK_BRONZE;
      default -> RANK_OTHER;
    };
  }

  private static void drawFooter(EconomyUiRenderer renderer, HomeLayout.Layout layout) {
    String economy = "Economy";
    String system = "System";
    UiRect card = layout.footer();
    renderer.card(card, EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconStyledText(UiIcon.HOME,
        List.of(new UiTextSpan(economy, 0xFF55FFFF), new UiTextSpan(system, 0xFFFF55FF)),
        card.x() + 8, card.y() + 5, layout.footerScale(), EconomyUiRenderer.ICON_SIZE,
        EconomyUiRenderer.ICON_ADVANCE);
    renderer.fill(new UiRect(card.x() + 8, card.bottom() - 3,
        Math.max(0, card.width() - 16), 1), 0x30FFFFFF);
  }

  private static UiButtonStyle navStyle(EconomyUiRoute route) {
    return switch (route) {
      case SHOP -> EconomyUiTheme.HOME_NAV_SHOP_STYLE;
      case MARKET -> EconomyUiTheme.HOME_NAV_MARKET_STYLE;
      case DELIVERY_BOX -> EconomyUiTheme.HOME_NAV_DELIVERY_STYLE;
      case TERRITORY -> EconomyUiTheme.HOME_NAV_TERRITORY_STYLE;
      case ABOUT, BALANCE_LOG -> EconomyUiTheme.HOME_NAV_ABOUT_STYLE;
      case HOME -> EconomyUiTheme.HOME_NAV_TERRITORY_STYLE;
    };
  }

  private static UiIcon navIcon(EconomyUiRoute route) {
    return switch (route) {
      case SHOP -> UiIcon.SHOP;
      case MARKET -> UiIcon.MARKET;
      case DELIVERY_BOX -> UiIcon.DELIVERY;
      case TERRITORY -> UiIcon.TERRITORY;
      case ABOUT, BALANCE_LOG -> UiIcon.ABOUT;
      case HOME -> UiIcon.HOME;
    };
  }
}
