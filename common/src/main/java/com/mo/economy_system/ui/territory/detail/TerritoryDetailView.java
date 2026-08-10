package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.List;

/** Semantic view for territory main, access, rules, and transfer screens. */
public final class TerritoryDetailView {
  private TerritoryDetailView() {}

  public static void render(EconomyUiRenderer renderer, TerritoryDetailState state,
                            TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()),
        TerritoryDetailLayout.BACKGROUND_COLOR);
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconText(UiIcon.TERRITORY, "Territory", layout.title().x() + 8,
        layout.title().y() + 5, 1.0f, EconomyUiRenderer.ICON_SIZE,
        EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.territory.detail.territory",
        List.of(state.territory().summary().name()), layout.subtitle(),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);

    if (state.view() == TerritoryDetailViewKind.MAIN) {
      renderMain(renderer, state, layout, mouseX, mouseY);
    } else {
      renderNested(renderer, state, layout, mouseX, mouseY);
    }

    if (state.view() != TerritoryDetailViewKind.MAIN && state.totalPages() > 1) {
      renderer.button(layout.previousButton(), EconomyUiTheme.TERRITORY_BUTTON, "<",
          layout.previousButton().contains(mouseX, mouseY), state.scroll() > 0);
      renderer.textInRect((state.scroll() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      renderer.button(layout.nextButton(), EconomyUiTheme.TERRITORY_BUTTON, ">",
          layout.nextButton().contains(mouseX, mouseY), state.scroll() + 1 < state.totalPages());
    }
    renderer.translatedButton(layout.backButton(), EconomyUiTheme.TERRITORY_BUTTON, "gui.back",
        List.of(), layout.backButton().contains(mouseX, mouseY), state.can(TerritoryDetailAction.BACK));
  }

  private static void renderMain(EconomyUiRenderer renderer, TerritoryDetailState state,
                                 TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    if (state.screenState() == ScreenState.LOADING) {
      message(renderer, "screen.territory.detail.loading", layout.rows(), EconomyUiTheme.TEXT_PRIMARY);
      return;
    }
    if (state.screenState() == ScreenState.ERROR) {
      message(renderer, keyOr(state.errorKey(), "screen.territory.detail.sync_failed"),
          layout.rows(), EconomyUiTheme.TEXT_ERROR);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.territory.detail.retry", List.of(),
          layout.retryButton().contains(mouseX, mouseY), state.can(TerritoryDetailAction.RETRY));
      return;
    }
    renderer.card(layout.rows(), EconomyUiTheme.TERRITORY_CARD, false);
    renderer.translatedTextInRect("screen.territory.detail.owner",
        List.of(state.territory().summary().ownerName()),
        new UiRect(layout.rows().x() + 12, layout.rows().y() + 12,
            Math.max(1, layout.rows().width() - 24), 16),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.territory.detail.member_count",
        List.of(Integer.toString(state.territory().authorizedMembers().size())),
        new UiRect(layout.rows().x() + 12, layout.rows().y() + 31,
            Math.max(1, layout.rows().width() - 24), 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    for (TerritoryDetailLayout.MainAction action : layout.mainActions()) {
      renderer.translatedButton(action.rect(), style(action.action()), actionKey(action.action()),
          List.of(), action.rect().contains(mouseX, mouseY), state.can(action.action()));
    }
  }

  private static void renderNested(EconomyUiRenderer renderer, TerritoryDetailState state,
                                   TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderSearchFrame(renderer, layout.search());
    if (state.screenState() == ScreenState.LOADING) {
      message(renderer, "screen.territory.detail.loading", layout.rows(), EconomyUiTheme.TEXT_PRIMARY);
      return;
    }
    if (state.screenState() == ScreenState.ERROR) {
      message(renderer, keyOr(state.errorKey(), "screen.territory.detail.sync_failed"),
          layout.rows(), EconomyUiTheme.TEXT_ERROR);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.territory.detail.retry", List.of(), layout.retryButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.RETRY));
      return;
    }
    if (state.rowCount() == 0) {
      message(renderer, emptyKey(state.view()), layout.rows(), EconomyUiTheme.TEXT_MUTED);
      return;
    }
    switch (state.view()) {
      case ACCESS -> renderAccess(renderer, state, layout, mouseX, mouseY);
      case RULES -> renderRules(renderer, state, layout, mouseX, mouseY);
      case TRANSFER -> renderTransfer(renderer, state, layout, mouseX, mouseY);
      case MAIN -> { }
    }
  }

  private static void renderAccess(EconomyUiRenderer renderer, TerritoryDetailState state,
                                   TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    for (TerritoryDetailLayout.AccessCard value : layout.accessCards()) {
      TerritoryAccessRow row = value.row();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD,
          value.card().contains(mouseX, mouseY));
      renderer.playerHead(row.playerId(), row.playerName(), value.head());
      renderer.textInRect(row.playerName(), value.name(), EconomyUiTheme.TEXT_PRIMARY,
          UiTextAlignment.LEFT);
      renderer.translatedTextInRect(row.allowed()
              ? "screen.territory.detail.access.allowed"
              : "screen.territory.detail.access.denied", List.of(), value.status(),
          row.allowed() ? EconomyUiTheme.TEXT_SUCCESS : EconomyUiTheme.TEXT_LOCKED,
          UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), row.allowed()
              ? EconomyUiTheme.TERRITORY_DANGER_BUTTON : EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON,
          row.allowed() ? "button.territory.access.remove" : "button.territory.access.add",
          List.of(), value.actionButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.TOGGLE_ACCESS));
    }
  }

  private static void renderRules(EconomyUiRenderer renderer, TerritoryDetailState state,
                                  TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    for (TerritoryDetailLayout.RuleCard value : layout.ruleCards()) {
      TerritoryRuleRow row = value.row();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD,
          value.card().contains(mouseX, mouseY));
      renderer.translatedTextInRect("message.territory.rule." + row.action().id(), List.of(),
          value.name(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.territory.detail.rule.description", List.of(),
          value.description(), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON,
          "message.territory.rule.level." + row.level().id(), List.of(),
          value.actionButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.CYCLE_RULE));
    }
  }

  private static void renderTransfer(EconomyUiRenderer renderer, TerritoryDetailState state,
                                     TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    for (TerritoryDetailLayout.TransferCard value : layout.transferCards()) {
      PlayerSummary player = value.player();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD,
          value.card().contains(mouseX, mouseY));
      renderer.playerHead(player.playerId(), player.playerName(), value.head());
      renderer.textInRect(player.playerName(), value.name(), EconomyUiTheme.TEXT_PRIMARY,
          UiTextAlignment.LEFT);
      renderer.textInRect(player.playerId().toString(), value.description(),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), EconomyUiTheme.TERRITORY_DANGER_BUTTON,
          "button.territory.transfer", List.of(), value.actionButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.TRANSFER_OWNERSHIP));
    }
  }

  private static void message(EconomyUiRenderer renderer, String key, UiRect rect, int color) {
    renderer.translatedTextInRect(key, List.of(), rect, color, UiTextAlignment.CENTER);
  }

  private static void renderSearchFrame(EconomyUiRenderer renderer, UiRect search) {
    UiRect frame = new UiRect(search.x() - 4, search.y() - 2,
        search.width() + 8, search.height() + 4);
    renderer.fill(frame, 0xE04A5568);
    renderer.fill(new UiRect(frame.x(), frame.y(), frame.width(), 1), EconomyUiTheme.TERRITORY_ACCENT);
    renderer.fill(new UiRect(frame.x(), frame.bottom() - 1, frame.width(), 1), EconomyUiTheme.TERRITORY_ACCENT);
    renderer.fill(new UiRect(frame.x(), frame.y(), 1, frame.height()), EconomyUiTheme.TERRITORY_ACCENT);
    renderer.fill(new UiRect(frame.right() - 1, frame.y(), 1, frame.height()), EconomyUiTheme.TERRITORY_ACCENT);
  }

  private static String titleKey(TerritoryDetailViewKind view) {
    return switch (view) {
      case MAIN -> "screen.territory.detail.title";
      case ACCESS -> "screen.territory.detail.access.title";
      case RULES -> "screen.territory.detail.rules.title";
      case TRANSFER -> "screen.territory.detail.transfer.title";
    };
  }

  private static String emptyKey(TerritoryDetailViewKind view) {
    return switch (view) {
      case ACCESS -> "screen.territory.detail.access.empty";
      case RULES -> "screen.territory.detail.rules.empty";
      case TRANSFER -> "screen.territory.detail.transfer.empty";
      case MAIN -> "screen.territory.detail.empty";
    };
  }

  private static String keyOr(String key, String fallback) {
    return key == null || key.isBlank() ? fallback : key;
  }

  private static String actionKey(TerritoryDetailAction action) {
    return switch (action) {
      case RESIZE -> "message.territory_management.resize_territory";
      case BUFFS -> "message.territory_management.buff";
      case ACCESS -> "message.territory_management.access";
      case RULES -> "message.territory_management.permissions";
      case TRANSFER -> "message.territory_management.transfer_ownership";
      default -> "gui.back";
    };
  }

  private static UiButtonStyle style(TerritoryDetailAction action) {
    return switch (action) {
      case BUFFS -> EconomyUiTheme.TERRITORY_WARN_BUTTON;
      case TRANSFER -> EconomyUiTheme.TERRITORY_DANGER_BUTTON;
      case ACCESS, RULES -> EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON;
      default -> EconomyUiTheme.TERRITORY_PRIMARY_BUTTON;
    };
  }
}
