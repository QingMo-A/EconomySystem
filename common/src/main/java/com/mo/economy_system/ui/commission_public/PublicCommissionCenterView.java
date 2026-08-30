package com.mo.economy_system.ui.commission_public;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.commission.PublicCommissionStatus;
import com.mo.economy_system.ui.component.UiPanel;
import com.mo.economy_system.ui.component.UiSection;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic renderer for the common public commission page. */
public final class PublicCommissionCenterView {
  private PublicCommissionCenterView() {}

  /** Follows the renderer-first convention used by the other common screen views. */
  public static void render(EconomyUiRenderer renderer, PublicCommissionCenterState state,
                            PublicCommissionCenterLayout.Layout layout, int mouseX, int mouseY) {
    render(state, layout, renderer, mouseX, mouseY);
  }

  public static void render(PublicCommissionCenterState state,
                            PublicCommissionCenterLayout.Layout layout,
                            EconomyUiRenderer renderer, int mouseX, int mouseY) {
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.LEADERBOARD, "screen.commissions.public.title", List.of(),
        layout.title().x() + 8, layout.title().y() + 5, 1.0f,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.commissions.public.esc", List.of(), layout.esc(),
        EconomyUiTheme.Text.MUTED, UiTextAlignment.RIGHT);
    UiPanel.render(renderer, layout.list(), false);
    UiPanel.render(renderer, layout.detail(), false);
    renderer.translatedTextInRect("screen.commissions.public.list", List.of(), layout.listHeader(),
        EconomyUiTheme.Text.PRIMARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.commissions.public.detail", List.of(), layout.detailHeader(),
        EconomyUiTheme.Text.PRIMARY, UiTextAlignment.LEFT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.commissions.public.loading", List.of(), layout.emptyOrLoading(),
          EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.emptyOrLoading(),
          EconomyUiTheme.Text.ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retry(), EconomyUiTheme.MARKET_FORM_BUTTON,
          "screen.commissions.public.retry", List.of(), layout.retry().contains(mouseX, mouseY), true);
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.commissions.public.empty", List.of(), layout.emptyOrLoading(),
          EconomyUiTheme.Text.MUTED, UiTextAlignment.CENTER);
    }

    for (PublicCommissionCenterLayout.Card card : layout.cards()) drawCard(renderer, card, mouseX, mouseY);
    PublicCommission selected = state.selected();
    if (selected != null && state.screenState() != ScreenState.ERROR) {
      renderer.textInRect(selected.name(), layout.detailHeader(), EconomyUiTheme.Text.PRIMARY,
          UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.commissions.public.target",
          List.of(selected.requesterName(), selected.targetSnapshot()), layout.target(),
          EconomyUiTheme.Text.SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.commissions.public.progress",
          List.of(Integer.toString(selected.targetAmount() - selected.remainingAmount()),
              Integer.toString(selected.targetAmount()), Integer.toString(selected.remainingAmount())),
          layout.progress(), EconomyUiTheme.Text.PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.commissions.public.reward",
          List.of(UiNumbers.formatInteger(selected.unitReward()),
              UiNumbers.formatInteger(selected.remainingBudget())), layout.reward(),
          EconomyUiTheme.HOME_BALANCE_ACCENT, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.commissions.public.expires",
          List.of(UiNumbers.formatDurationMillis(selected.expiresAt() - state.serverNowMillis())), layout.expiration(),
          selected.status() == PublicCommissionStatus.AVAILABLE
              ? EconomyUiTheme.Text.SECONDARY : EconomyUiTheme.Text.ERROR, UiTextAlignment.LEFT);
      if (!state.actionMessage().isBlank()) {
        renderer.textInRect(state.actionMessage(), layout.message(),
            state.lastSubmitStatus() == com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus.REJECTED
                ? EconomyUiTheme.Text.ERROR : EconomyUiTheme.Text.SUCCESS, UiTextAlignment.CENTER);
      }
      renderAmountFrame(renderer, layout.amountInput(), false,
          layout.amountInput().contains(mouseX, mouseY));
      renderer.translatedButton(layout.submit(), EconomyUiTheme.MARKET_FORM_BUTTON,
          "screen.commissions.public.submit", List.of(), layout.submit().contains(mouseX, mouseY),
          selected.status() == PublicCommissionStatus.AVAILABLE && !state.submitInFlight());
    }
    renderer.translatedButton(layout.back(), EconomyUiTheme.NEUTRAL_FORM_BUTTON,
        "button.common.back", List.of(), layout.back().contains(mouseX, mouseY), true);
  }

  public static void renderAmountFrame(EconomyUiRenderer renderer, UiRect nativeWidgetRect,
                                       boolean focused, boolean hovered) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.MARKET_SEARCH_FRAME,
        focused, hovered);
  }

  private static void drawCard(EconomyUiRenderer renderer, PublicCommissionCenterLayout.Card card,
                               int mouseX, int mouseY) {
    PublicCommission commission = card.commission();
    boolean hovered = card.rect().contains(mouseX, mouseY);
    UiSection.render(renderer, card.rect(), EconomyUiTheme.LEADERBOARD_ACCENT, hovered);
    if (card.selected()) UiSection.selectionGlow(renderer, card.rect(), EconomyUiTheme.LEADERBOARD_ACCENT);
    renderer.textInRect(commission.name(), new UiRect(card.rect().x() + 8, card.rect().y() + 5,
        Math.max(1, card.rect().width() - 16), 12), EconomyUiTheme.Text.PRIMARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.commissions.public.card.progress",
        List.of(Integer.toString(commission.targetAmount() - commission.remainingAmount()),
            Integer.toString(commission.targetAmount())),
        new UiRect(card.rect().x() + 8, card.rect().y() + 22,
            Math.max(1, card.rect().width() - 16), 12), EconomyUiTheme.Text.SECONDARY,
        UiTextAlignment.LEFT);
  }
}
