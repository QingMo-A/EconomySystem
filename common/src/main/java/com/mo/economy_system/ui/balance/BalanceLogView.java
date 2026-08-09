package com.mo.economy_system.ui.balance;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic balance-log table renderer shared by both targets. */
public final class BalanceLogView {
  private BalanceLogView() {}

  public static void render(EconomyUiRenderer renderer, BalanceLogState state,
                            BalanceLogLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.card(layout.panel(), EconomyUiTheme.MARKET_CARD, false);
    renderer.icon(UiIcon.BALANCE, new UiRect(layout.title().x(), layout.title().y(), 12, 12));
    renderer.translatedText("screen.balance_log.title", List.of(), layout.title().x() + 16,
        layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.balance_log.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    for (BalanceLogLayout.Tab tab : layout.tabs()) {
      boolean selected = tab.category().equals(state.category());
      renderer.button(tab.rect(), selected ? EconomyUiTheme.MARKET_BUTTON : EconomyUiTheme.DISABLED_BUTTON,
          tab.category(), tab.rect().contains(mouseX, mouseY), true);
    }
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.balance_log.loading", List.of(), layout.message(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.balance_log.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.MARKET_BUTTON, "screen.balance_log.retry",
          List.of(), layout.message().contains(mouseX, mouseY), state.can(BalanceLogAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.balance_log.empty", List.of(), layout.message(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    }
    for (int i = 0; i < layout.rows().size(); i++) {
      BalanceLogLayout.Row row = layout.rows().get(i);
      var entry = row.row().entry();
      renderer.card(row.rect(), i % 2 == 0 ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.DELIVERY_CARD,
          false);
      renderer.text(Long.toString(entry.timeMillis()), row.rect().x() + 6,
          row.rect().y() + 4, EconomyUiTheme.TEXT_SECONDARY);
      renderer.text(entry.category(), row.rect().x() + 84, row.rect().y() + 4,
          EconomyUiTheme.SHOP_ACCENT);
      String delta = (entry.delta() >= 0 ? "+" : "") + entry.delta();
      renderer.text(delta, row.rect().x() + 132, row.rect().y() + 4,
          entry.delta() >= 0 ? EconomyUiTheme.TEXT_SUCCESS : EconomyUiTheme.TEXT_ERROR);
      renderer.text(entry.beforeBalance() + " -> " + entry.afterBalance(), row.rect().x() + 188,
          row.rect().y() + 4, EconomyUiTheme.TEXT_PRIMARY);
      renderer.textInRect(entry.reason(), new UiRect(row.rect().x() + 320, row.rect().y() + 2,
          Math.max(1, row.rect().width() - 326), 18), EconomyUiTheme.TEXT_SECONDARY,
          UiTextAlignment.LEFT);
    }
    renderer.translatedButton(layout.previousButton(), EconomyUiTheme.MARKET_BUTTON,
        "screen.balance_log.previous", List.of(), layout.previousButton().contains(mouseX, mouseY),
        state.hasPreviousPage());
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages() + "  " + state.total(),
        layout.pageText(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedButton(layout.nextButton(), EconomyUiTheme.MARKET_BUTTON,
        "screen.balance_log.next", List.of(), layout.nextButton().contains(mouseX, mouseY),
        state.hasNextPage());
  }
}
