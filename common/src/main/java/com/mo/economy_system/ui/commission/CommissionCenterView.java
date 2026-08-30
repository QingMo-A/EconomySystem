package com.mo.economy_system.ui.commission;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

public final class CommissionCenterView {
  private CommissionCenterView() {}

  public static void render(EconomyUiRenderer renderer, CommissionCenterState state,
      CommissionCenterLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.list(), EconomyUiTheme.HOME_LEADERBOARD_CARD, false);
    renderer.translatedText("screen.commissions.mine", List.of(), layout.list().x() + 10, layout.list().y() + 10, 0xFFFFFFFF);
    renderer.card(layout.detail(), EconomyUiTheme.HOME_BALANCE_CARD, false);
    renderer.translatedButton(layout.publicTab(), EconomyUiTheme.TERRITORY_BUTTON,
        "screen.commissions.public.tab", List.of(), layout.publicTab().contains(mouseX, mouseY), true);
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.commissions.loading", List.of(), layout.retry(), 0xFFFFFFFF, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.retry(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retry(), EconomyUiTheme.TERRITORY_BUTTON, "screen.commissions.retry", List.of(), layout.retry().contains(mouseX, mouseY), true);
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.commissions.empty", List.of(), layout.retry(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    } else {
      for (CommissionCenterLayout.Card card : layout.cards()) drawCard(renderer, card, mouseX, mouseY);
      CommissionInstance selected = state.selected();
      if (selected != null) {
        renderer.text(selected.requesterName(), layout.detail().x() + 12, layout.detail().y() + 14, 0xFFFFFFFF);
        renderer.text(selected.targetSnapshot() + " × " + selected.requiredAmount(), layout.detail().x() + 12, layout.detail().y() + 34, 0xFFDDDDDD);
        renderer.text("进度 " + selected.progress() + "/" + selected.requiredAmount(), layout.detail().x() + 12, layout.detail().y() + 54, 0xFFAAAAAA);
        renderer.text("奖励 " + UiNumbers.formatInteger(selected.rewardSnapshot().amount()), layout.detail().x() + 12, layout.detail().y() + 74, EconomyUiTheme.HOME_BALANCE_ACCENT);
        renderer.translatedButton(layout.submit(), EconomyUiTheme.TERRITORY_BUTTON, "screen.commissions.submit_one", List.of(), layout.submit().contains(mouseX, mouseY), !selected.status().terminal() && selected.type() == com.mo.economy_system.common.commission.CommissionType.ITEM_DELIVERY);
      }
    }
    renderer.translatedButton(layout.back(), EconomyUiTheme.TERRITORY_BUTTON, "button.common.back", List.of(), layout.back().contains(mouseX, mouseY), true);
  }

  private static void drawCard(EconomyUiRenderer renderer, CommissionCenterLayout.Card card,
      int mouseX, int mouseY) {
    com.mo.economy_system.ui.theme.UiCardStyle style = card.selected()
        ? EconomyUiTheme.HOME_BALANCE_CARD : EconomyUiTheme.HOME_LEADERBOARD_CARD;
    renderer.card(card.rect(), style, card.rect().contains(mouseX, mouseY));
    CommissionInstance c = card.commission();
    renderer.text(c.requesterName(), card.rect().x() + 8, card.rect().y() + 5, 0xFFFFFFFF);
    renderer.text(c.targetSnapshot() + " " + c.progress() + "/" + c.requiredAmount(), card.rect().x() + 8, card.rect().y() + 21, 0xFFCCCCCC);
  }
}
