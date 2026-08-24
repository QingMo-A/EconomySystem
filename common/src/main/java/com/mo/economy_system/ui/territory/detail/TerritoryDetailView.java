package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import java.util.List;

/** Semantic view for the unified territory management center. */
public final class TerritoryDetailView {
  private TerritoryDetailView() {}

  public static void render(EconomyUiRenderer renderer, TerritoryDetailState state,
                            TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.TERRITORY, "screen.territory.detail.title", List.of(),
        layout.title().x() + 8, layout.title().y() + 5, 1.0f,
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.territory.detail.territory",
        List.of(state.territory().summary().name()), layout.subtitle(),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);

    renderNavigation(renderer, state, layout, mouseX, mouseY);
    renderer.card(layout.content(), EconomyUiTheme.TERRITORY_CARD, false);

    switch (state.view()) {
      case MAIN -> renderMain(renderer, state, layout, mouseX, mouseY);
      case ACCESS -> renderAccess(renderer, state, layout, mouseX, mouseY);
      case RULES -> renderRules(renderer, state, layout, mouseX, mouseY);
      case SETTINGS -> renderSettings(renderer, state, layout, mouseX, mouseY);
      case TRANSFER -> renderTransfer(renderer, state, layout, mouseX, mouseY);
    }

    renderLifecycle(renderer, state, layout, mouseX, mouseY);
    if (state.searchVisible() && state.totalPages() > 1) {
      renderPagination(renderer, state, layout, mouseX, mouseY);
    }
  }

  private static void renderNavigation(EconomyUiRenderer renderer, TerritoryDetailState state,
                                       TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.navigationPanel(), EconomyUiTheme.TERRITORY_CARD, false);
    for (TerritoryDetailLayout.NavigationButton nav : layout.navigationButtons()) {
      UiButtonStyle style = nav.selected()
          ? EconomyUiTheme.TERRITORY_PRIMARY_BUTTON
          : nav.action() == TerritoryDetailAction.BUFFS
              ? EconomyUiTheme.TERRITORY_BUFF_BUTTON
              : EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON;
      renderer.translatedButton(nav.rect(), style, navigationKey(nav.action()), List.of(),
          nav.rect().contains(mouseX, mouseY), state.can(nav.action()));
    }
    renderer.translatedButton(layout.backButton(), EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON,
        "gui.back", List.of(), layout.backButton().contains(mouseX, mouseY), true);
  }

  private static void renderMain(EconomyUiRenderer renderer, TerritoryDetailState state,
                                 TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    UiRect content = layout.content();
    renderer.translatedTextInRect("screen.territory.detail.overview.title", List.of(),
        new UiRect(content.x() + 12, content.y() + 10, content.width() - 24, 16),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);

    UiRect summary = new UiRect(content.x() + 8, content.y() + 32,
        Math.max(1, content.width() / 2 - 12), 116);
    UiRect rules = new UiRect(summary.right() + 8, summary.y(),
        Math.max(1, content.right() - summary.right() - 16), 116);
    renderer.card(summary, EconomyUiTheme.TERRITORY_CARD, false);
    renderer.card(rules, EconomyUiTheme.TERRITORY_CARD, false);

    var territory = state.territory();
    var summaryData = territory.summary();
    Position p1 = summaryData.pos1();
    Position p2 = summaryData.pos2();
    int sizeX = Math.abs(p1.x() - p2.x()) + 1;
    int sizeZ = Math.abs(p1.z() - p2.z()) + 1;

    renderer.translatedTextInRect("screen.territory.detail.overview.status", List.of(),
        new UiRect(summary.x() + 10, summary.y() + 8, summary.width() - 20, 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.territory.detail.owner", List.of(summaryData.ownerName()),
        new UiRect(summary.x() + 10, summary.y() + 27, summary.width() - 20, 14),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.territory.detail.member_count",
        List.of(Integer.toString(territory.authorizedMembers().size())),
        new UiRect(summary.x() + 10, summary.y() + 45, summary.width() - 20, 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.territory.detail.dimension",
        List.of(summaryData.dimensionId()),
        new UiRect(summary.x() + 10, summary.y() + 63, summary.width() - 20, 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.territory.detail.range",
        List.of(Integer.toString(sizeX), Integer.toString(sizeZ)),
        new UiRect(summary.x() + 10, summary.y() + 81, summary.width() - 20, 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);

    renderer.translatedTextInRect("screen.territory.detail.overview.permissions", List.of(),
        new UiRect(rules.x() + 10, rules.y() + 8, rules.width() - 20, 14),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    RuleAction[] highlighted = {RuleAction.BREAK_BLOCK, RuleAction.OPEN_CONTAINER, RuleAction.USE_ITEM};
    for (int index = 0; index < highlighted.length; index++) {
      RuleAction action = highlighted[index];
      Rule rule = territory.rules().stream().filter(value -> value.action() == action).findFirst().orElse(null);
      int y = rules.y() + 29 + index * 24;
      renderer.translatedTextInRect("message.territory.rule." + action.id(), List.of(),
          new UiRect(rules.x() + 10, y, Math.max(1, rules.width() - 108), 14),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      if (rule != null) {
        renderer.translatedTextInRect("message.territory.rule.level." + rule.level().id(), List.of(),
            new UiRect(rules.right() - 96, y, 86, 14), ruleLevelColor(rule.level()), UiTextAlignment.RIGHT);
      }
    }

    for (TerritoryDetailLayout.QuickAction action : layout.quickActions()) {
      renderer.translatedButton(action.rect(), quickStyle(action.action()), actionKey(action.action()), List.of(),
          action.rect().contains(mouseX, mouseY), state.can(action.action()));
    }
  }

  private static void renderAccess(EconomyUiRenderer renderer, TerritoryDetailState state,
                                   TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderSearchFrame(renderer, layout.search(), mouseX, mouseY);
    renderer.translatedButton(layout.inviteButton(), EconomyUiTheme.TERRITORY_PRIMARY_BUTTON,
        "button.territory.members.invite", List.of(), layout.inviteButton().contains(mouseX, mouseY),
        state.can(TerritoryDetailAction.INVITE));
    if (state.accessRows().isEmpty()) {
      message(renderer, "screen.territory.detail.access.empty", layout.rows(), EconomyUiTheme.TEXT_MUTED);
      return;
    }
    for (TerritoryDetailLayout.AccessCard value : layout.accessCards()) {
      TerritoryAccessRow row = value.row();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD, value.card().contains(mouseX, mouseY));
      renderer.playerHead(row.playerId(), row.playerName(), value.head());
      renderer.textInRect(row.playerName(), value.name(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.territory.detail.access.member", List.of(), value.status(),
          EconomyUiTheme.TEXT_SUCCESS, UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), EconomyUiTheme.TERRITORY_DANGER_BUTTON,
          "button.territory.access.remove", List.of(), value.actionButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.TOGGLE_ACCESS));
    }
  }

  private static void renderRules(EconomyUiRenderer renderer, TerritoryDetailState state,
                                  TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderSearchFrame(renderer, layout.search(), mouseX, mouseY);
    if (state.ruleRows().isEmpty()) {
      message(renderer, "screen.territory.detail.rules.empty", layout.rows(), EconomyUiTheme.TEXT_MUTED);
      return;
    }
    for (TerritoryDetailLayout.RuleCard value : layout.ruleCards()) {
      TerritoryRuleRow row = value.row();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD, value.card().contains(mouseX, mouseY));
      renderer.translatedTextInRect("message.territory.rule." + row.action().id(), List.of(),
          value.name(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.territory.detail.rule.description", List.of(),
          value.description(), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), ruleLevelStyle(row.level(), true),
          "message.territory.rule.level." + row.level().id(), List.of(),
          value.actionButton().contains(mouseX, mouseY), state.can(TerritoryDetailAction.CYCLE_RULE));
    }
  }

  private static void renderSettings(EconomyUiRenderer renderer, TerritoryDetailState state,
                                     TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    UiRect content = layout.content();
    renderer.translatedTextInRect("screen.territory.detail.settings.title", List.of(),
        layout.settingsTitle(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    for (TerritoryDetailLayout.SettingAction setting : layout.settingsActions()) {
      boolean dangerous = setting.action() == TerritoryDetailAction.TRANSFER
          || setting.action() == TerritoryDetailAction.DELETE;
      renderer.card(setting.row(), EconomyUiTheme.TERRITORY_CARD,
          setting.row().contains(mouseX, mouseY));
      String base = "screen.territory.detail.settings." + settingId(setting.action());
      renderer.translatedTextInRect(base + ".title", List.of(),
          new UiRect(setting.row().x() + 10, setting.row().y() + 7,
              Math.max(1, setting.button().x() - setting.row().x() - 18), 14),
          dangerous ? EconomyUiTheme.TEXT_ERROR : EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      List<String> args = setting.action() == TerritoryDetailAction.COPY_ID
          ? List.of(state.territory().summary().territoryId().toString()) : List.of();
      renderer.translatedTextInRect(base + ".description", args,
          new UiRect(setting.row().x() + 10, setting.row().y() + 23,
              Math.max(1, setting.button().x() - setting.row().x() - 18), 14),
          dangerous ? EconomyUiTheme.TEXT_ERROR : EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(setting.button(), dangerous
              ? EconomyUiTheme.TERRITORY_DANGER_BUTTON
              : setting.action() == TerritoryDetailAction.RESIZE
                  ? EconomyUiTheme.TERRITORY_PRIMARY_BUTTON
                  : EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON,
          actionKey(setting.action()), List.of(), setting.button().contains(mouseX, mouseY),
          state.can(setting.action()));
    }
  }

  private static void renderTransfer(EconomyUiRenderer renderer, TerritoryDetailState state,
                                     TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    renderSearchFrame(renderer, layout.search(), mouseX, mouseY);
    if (state.transferRows().isEmpty()) {
      message(renderer, "screen.territory.detail.transfer.empty", layout.rows(), EconomyUiTheme.TEXT_MUTED);
      return;
    }
    for (TerritoryDetailLayout.TransferCard value : layout.transferCards()) {
      PlayerSummary player = value.player();
      renderer.card(value.card(), EconomyUiTheme.TERRITORY_CARD, value.card().contains(mouseX, mouseY));
      renderer.playerHead(player.playerId(), player.playerName(), value.head());
      renderer.textInRect(player.playerName(), value.name(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.textInRect(player.playerId().toString(), value.description(),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(value.actionButton(), EconomyUiTheme.TERRITORY_DANGER_BUTTON,
          "button.territory.transfer", List.of(), value.actionButton().contains(mouseX, mouseY),
          state.can(TerritoryDetailAction.TRANSFER_OWNERSHIP));
    }
  }

  private static void renderLifecycle(EconomyUiRenderer renderer, TerritoryDetailState state,
                                      TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.territory.detail.syncing", List.of(),
          new UiRect(layout.content().right() - 120, layout.content().bottom() - 18, 108, 12),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.RIGHT);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(keyOr(state.errorKey(), "screen.territory.detail.sync_failed"),
          List.of(), new UiRect(layout.content().x() + 8, layout.content().bottom() - 20,
              Math.max(1, layout.content().width() - 112), 14),
          EconomyUiTheme.TEXT_ERROR, UiTextAlignment.LEFT);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.TERRITORY_BUTTON,
          "screen.territory.detail.retry", List.of(), layout.retryButton().contains(mouseX, mouseY), true);
    }
  }

  private static void renderPagination(EconomyUiRenderer renderer, TerritoryDetailState state,
                                       TerritoryDetailLayout.Layout layout, int mouseX, int mouseY) {
    boolean previousEnabled = state.scroll() > 0;
    renderer.button(layout.previousButton(), previousEnabled
            ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
        "", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
    renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x()
        + (layout.previousButton().width() - 12) / 2,
        layout.previousButton().y() + 4, 12, 12));
    renderer.textInRect((state.scroll() + 1) + " / " + state.totalPages(), layout.pageText(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    boolean nextEnabled = state.scroll() + 1 < state.totalPages();
    renderer.button(layout.nextButton(), nextEnabled
            ? EconomyUiTheme.TERRITORY_PAGE_BUTTON : EconomyUiTheme.TERRITORY_PAGE_BUTTON_DISABLED,
        "", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x()
        + (layout.nextButton().width() - 12) / 2,
        layout.nextButton().y() + 4, 12, 12));
  }

  private static void message(EconomyUiRenderer renderer, String key, UiRect rect, int color) {
    renderer.translatedTextInRect(key, List.of(), rect, color, UiTextAlignment.CENTER);
  }

  private static void renderSearchFrame(EconomyUiRenderer renderer, UiRect search,
                                        int mouseX, int mouseY) {
    UiRect frame = new UiRect(search.x() - 4, search.y() - 2, search.width() + 8, search.height() + 4);
    renderer.inputFrame(frame, EconomyUiTheme.TERRITORY_SEARCH_FRAME, search.contains(mouseX, mouseY));
  }

  private static String keyOr(String key, String fallback) {
    return key == null || key.isBlank() ? fallback : key;
  }

  private static String navigationKey(TerritoryDetailAction action) {
    return switch (action) {
      case OVERVIEW -> "button.territory.nav.overview";
      case ACCESS -> "button.territory.nav.members";
      case RULES -> "button.territory.nav.permissions";
      case BUFFS -> "button.territory.nav.buffs";
      case SETTINGS -> "button.territory.nav.settings";
      default -> "gui.back";
    };
  }

  private static String actionKey(TerritoryDetailAction action) {
    return switch (action) {
      case RESIZE -> "message.territory_management.resize_territory";
      case INVITE -> "message.territory_management.invite_player";
      case COPY_ID -> "message.territory_management.copy_id";
      case TRANSFER -> "message.territory_management.transfer_ownership";
      case DELETE -> "message.territory_management.delete_territory";
      default -> "gui.back";
    };
  }

  private static String settingId(TerritoryDetailAction action) {
    return switch (action) {
      case COPY_ID -> "copy_id";
      case RESIZE -> "resize";
      case TRANSFER -> "transfer";
      case DELETE -> "delete";
      default -> "unknown";
    };
  }

  private static UiButtonStyle quickStyle(TerritoryDetailAction action) {
    return action == TerritoryDetailAction.INVITE
        ? EconomyUiTheme.TERRITORY_PRIMARY_BUTTON : EconomyUiTheme.TERRITORY_NEUTRAL_BUTTON;
  }

  private static UiButtonStyle ruleLevelStyle(RuleLevel level, boolean selected) {
    if (!selected) return EconomyUiTheme.TERRITORY_RULE_UNSELECTED_BUTTON;
    return switch (level) {
      case OWNER_ONLY -> EconomyUiTheme.TERRITORY_RULE_OWNER_BUTTON;
      case MEMBERS -> EconomyUiTheme.TERRITORY_RULE_MEMBER_BUTTON;
      case EVERYONE -> EconomyUiTheme.TERRITORY_RULE_EVERYONE_BUTTON;
    };
  }

  private static int ruleLevelColor(RuleLevel level) {
    return switch (level) {
      case OWNER_ONLY -> EconomyUiTheme.TEXT_LOCKED;
      case MEMBERS -> EconomyUiTheme.TERRITORY_ACCENT;
      case EVERYONE -> EconomyUiTheme.TEXT_SUCCESS;
    };
  }
}
