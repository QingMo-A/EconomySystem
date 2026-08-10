package com.mo.economy_system.ui.territory.invite;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic invite directory view shared by Forge and NeoForge. */
public final class TerritoryInviteView {
  private TerritoryInviteView() {}

  public static void render(EconomyUiRenderer renderer, TerritoryInviteState state,
      TerritoryInviteLayout.Layout layout, int mouseX, int mouseY, long tick) {
    renderer.card(layout.rows(), EconomyUiTheme.TERRITORY_CARD, false);
    UiRect searchFrame = new UiRect(layout.search().x() - 4, layout.search().y() - 2,
        layout.search().width() + 8, layout.search().height() + 4);
    renderer.inputFrame(searchFrame, EconomyUiTheme.TERRITORY_SEARCH_FRAME,
        layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.invite.title", List.of(), layout.title(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedTextInRect("screen.invite.territory", List.of(state.territoryName()), layout.subtitle(),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.CENTER);
    renderer.icon(UiIcon.MEMBER, new UiRect(layout.rows().x() + 6, layout.rows().y() + 6, 12, 12));
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.invite.loading", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.invite.sync_failed" : state.errorKey(),
          List.of(), layout.retryButton(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.invite.retry", List.of(), layout.retryButton().contains(mouseX, mouseY), true);
    } else if (state.eligiblePlayers().isEmpty()) {
      renderer.translatedTextInRect("screen.invite.empty", List.of(), layout.retryButton(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    } else {
      for (TerritoryInviteLayout.Row row : layout.playerRows()) {
        renderer.playerHead(row.player().playerId(), row.player().playerName(),
            new UiRect(row.row().x() + 4, row.row().y() + 4, 16, 16));
        renderer.textInRect(row.player().playerName(),
            new UiRect(row.row().x() + 26, row.row().y(),
                Math.max(1, row.inviteButton().x() - row.row().x() - 30), row.row().height()),
            EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
        boolean enabled = state.canInvite(row.player().playerId()) && tick >= state.cooldownUntilTick();
        renderer.translatedButton(row.inviteButton(), EconomyUiTheme.TERRITORY_BUTTON,
            "button.invite.invite", List.of(), row.inviteButton().contains(mouseX, mouseY), enabled);
      }
    }
    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled
          ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
          "", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x() + 19,
          layout.previousButton().y() + 4, 12, 12));
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled
          ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
          "", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
      renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x() + 19,
          layout.nextButton().y() + 4, 12, 12));
    }
    renderer.translatedButton(layout.backButton(), EconomyUiTheme.HOME_ABOUT_BUTTON,
        "button.invite.back", List.of(), layout.backButton().contains(mouseX, mouseY), true);
  }
}
