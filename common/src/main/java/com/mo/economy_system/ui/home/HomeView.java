package com.mo.economy_system.ui.home;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.List;

/** Semantic home dashboard view shared by both Minecraft targets. */
public final class HomeView {
  private HomeView() {}

  public static void render(EconomyUiRenderer renderer, HomeState state,
                            HomeLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        0x400A0A14);
    for (HomeLayout.NavButton nav : layout.navButtons()) {
      EconomyUiMenu.Entry entry = state.entries().stream()
          .filter(value -> value.route() == nav.route()).findFirst().orElse(null);
      if (entry == null) continue;
      renderer.translatedButton(nav.rect(), navStyle(nav.route()), entry.labelKey(), List.of(),
          nav.rect().contains(mouseX, mouseY), state.screenState() != ScreenState.ERROR);
    }

    renderer.card(layout.balanceCard(), EconomyUiTheme.HOME_CARD,
        layout.balanceCard().contains(mouseX, mouseY));
    renderer.icon(UiIcon.BALANCE, new UiRect(layout.balanceCard().x() + 10,
        layout.balanceCard().y() + 10, 14, 14));
    renderer.translatedTextInRect("screen.home.balance", List.of(),
        new UiRect(layout.balanceCard().x() + 30, layout.balanceCard().y() + 8,
            Math.max(1, layout.balanceCard().width() - 40), 16),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.textInRect(Integer.toString(state.balance()),
        new UiRect(layout.balanceCard().x() + 10, layout.balanceCard().y() + 32,
            Math.max(1, layout.balanceCard().width() - 20), 24),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);

    renderer.card(layout.tradeCard(), EconomyUiTheme.HOME_CARD,
        layout.tradeCard().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.home.trade", List.of(),
        new UiRect(layout.tradeCard().x() + 10, layout.tradeCard().y() + 8,
            Math.max(1, layout.tradeCard().width() - 20), 16),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.home.sell_orders", List.of(Integer.toString(state.sellOrders())),
        new UiRect(layout.tradeCard().x() + 10, layout.tradeCard().y() + 31,
            Math.max(1, layout.tradeCard().width() - 20), 14),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.home.demand_orders", List.of(Integer.toString(state.demandOrders())),
        new UiRect(layout.tradeCard().x() + 10, layout.tradeCard().y() + 48,
            Math.max(1, layout.tradeCard().width() - 20), 14),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);

    renderer.card(layout.leaderboardCard(), EconomyUiTheme.HOME_LEADERBOARD_CARD,
        layout.leaderboardCard().contains(mouseX, mouseY));
    renderer.icon(UiIcon.LEADERBOARD, new UiRect(layout.leaderboardCard().x() + 10,
        layout.leaderboardCard().y() + 9, 12, 12));
    renderer.translatedTextInRect("screen.home.leaderboard", List.of(),
        new UiRect(layout.leaderboardCard().x() + 28, layout.leaderboardCard().y() + 8,
            Math.max(1, layout.leaderboardCard().width() - 40), 16),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.home.loading", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
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
        renderer.textInRect("#" + row.rank() + " " + row.account().playerName(), row.rect(),
            self ? EconomyUiTheme.SHOP_ACCENT : EconomyUiTheme.TEXT_PRIMARY,
            UiTextAlignment.LEFT);
        renderer.textInRect(Integer.toString(row.account().balance()), row.rect(),
            self ? EconomyUiTheme.SHOP_ACCENT : EconomyUiTheme.TEXT_SECONDARY,
            UiTextAlignment.RIGHT);
      }
    }
    if (state.totalPages() > 1) {
      renderer.button(layout.previousButton(), EconomyUiTheme.TERRITORY_BUTTON, "<",
          layout.previousButton().contains(mouseX, mouseY), state.leaderboardOffset() > 0);
      renderer.textInRect((state.leaderboardOffset() / state.leaderboardPageSize() + 1)
              + " / " + state.totalPages(), layout.pageText(), EconomyUiTheme.TEXT_PRIMARY,
          UiTextAlignment.CENTER);
      renderer.button(layout.nextButton(), EconomyUiTheme.TERRITORY_BUTTON, ">",
          layout.nextButton().contains(mouseX, mouseY),
          state.leaderboardOffset() + state.leaderboardPageSize() < state.accounts().size());
    }
    renderer.translatedTextInRect("screen.home.version", List.of(), layout.footer(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
  }

  private static UiButtonStyle navStyle(EconomyUiRoute route) {
    return switch (route) {
      case SHOP -> EconomyUiTheme.HOME_SHOP_BUTTON;
      case MARKET -> EconomyUiTheme.HOME_MARKET_BUTTON;
      case DELIVERY_BOX -> EconomyUiTheme.HOME_DELIVERY_BUTTON;
      case TERRITORY -> EconomyUiTheme.HOME_TERRITORY_BUTTON;
      case ABOUT, BALANCE_LOG -> EconomyUiTheme.HOME_ABOUT_BUTTON;
      case HOME -> EconomyUiTheme.TERRITORY_BUTTON;
    };
  }
}
