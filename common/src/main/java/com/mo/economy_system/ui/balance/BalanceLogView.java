package com.mo.economy_system.ui.balance;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.text.UiNumbers;
import java.util.List;

/** Semantic balance-log table renderer shared by both targets. */
public final class BalanceLogView {
  private BalanceLogView() {}

  public static void render(EconomyUiRenderer renderer, BalanceLogState state,
                            BalanceLogLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.panel(), EconomyUiTheme.BALANCE_CARD, false);
    renderer.translatedText("screen.balance_log.title", List.of(), layout.title().x(),
        layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.balance_log.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    for (BalanceLogLayout.Tab tab : layout.tabs()) {
      boolean selected = tab.category().equals(state.category());
      renderer.button(tab.rect(), selected ? EconomyUiTheme.BALANCE_BUTTON : EconomyUiTheme.DISABLED_BUTTON,
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
      renderer.fill(row.rect(), i % 2 == 0 ? 0x301A2633 : 0x201A2633);
      renderer.text(UiNumbers.formatTimestamp(entry.timeMillis()), row.rect().x() + 6,
          row.rect().y() + 4, EconomyUiTheme.TEXT_SECONDARY);
      // Legacy Screen_BalanceLog places the category at x+82 (time starts at x+6).
      renderer.text(entry.category(), row.rect().x() + 82, row.rect().y() + 4,
          EconomyUiTheme.SHOP_ACCENT);
      String delta = (entry.delta() >= 0 ? "+" : "") + entry.delta();
      renderer.text(delta, row.rect().x() + 132, row.rect().y() + 4,
          entry.delta() >= 0 ? EconomyUiTheme.TEXT_SUCCESS : EconomyUiTheme.TEXT_ERROR);
      renderer.text(entry.beforeBalance() + " -> " + entry.afterBalance(), row.rect().x() + 188,
          row.rect().y() + 4, EconomyUiTheme.TEXT_PRIMARY);
      renderer.textInRect(entry.reason(), new UiRect(row.rect().x() + 320, row.rect().y() + 2,
          Math.max(80, row.rect().width() - 330), 18), EconomyUiTheme.TEXT_SECONDARY,
          UiTextAlignment.LEFT);
    }
    renderer.translatedButton(layout.previousButton(), state.hasPreviousPage()
            ? EconomyUiTheme.BALANCE_BUTTON : EconomyUiTheme.BALANCE_BUTTON_DISABLED,
        "screen.balance_log.previous", List.of(), layout.previousButton().contains(mouseX, mouseY),
        state.hasPreviousPage());
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages() + "  \u5171 "
        + state.total() + " \u6761",
        layout.pageText(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedButton(layout.nextButton(), state.hasNextPage()
            ? EconomyUiTheme.BALANCE_BUTTON : EconomyUiTheme.BALANCE_BUTTON_DISABLED,
        "screen.balance_log.next", List.of(), layout.nextButton().contains(mouseX, mouseY),
        state.hasNextPage());
  }
}
