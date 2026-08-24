package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure, loader-neutral geometry for the unified territory management center. */
public final class TerritoryDetailLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int ROW_HEIGHT = 40;
  public static final int ACTION_HEIGHT = 22;
  private static final int TITLE_Y = 14;
  private static final int BODY_Y = 48;
  private static final int NAV_WIDTH = 112;
  private static final int NAV_GAP = 8;
  private static final int FOOTER_HEIGHT = 34;

  private TerritoryDetailLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryDetailState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int contentX = panel + NAV_WIDTH + NAV_GAP;
    int contentWidth = Math.max(1, width - contentX - panel);
    int footerY = Math.max(BODY_Y + 40, height - FOOTER_HEIGHT);

    UiRect title = new UiRect(panel, TITLE_Y, Math.max(1, width - panel * 2), 14);
    UiRect subtitle = new UiRect(panel, TITLE_Y + 16, Math.max(1, width - panel * 2), 12);
    UiRect navigationPanel = new UiRect(panel, BODY_Y, NAV_WIDTH,
        Math.max(1, footerY - BODY_Y));
    List<NavigationButton> navigationButtons = navigationButtons(navigationPanel, state.view());
    UiRect back = new UiRect(navigationPanel.x() + 8,
        Math.max(navigationPanel.y() + 8, navigationPanel.bottom() - 28),
        Math.max(1, navigationPanel.width() - 16), 20);

    UiRect content = new UiRect(contentX, BODY_Y, contentWidth,
        Math.max(1, footerY - BODY_Y));
    UiRect search = new UiRect(content.x() + 8, content.y() + 8,
        Math.min(160, Math.max(1, content.width() - 16)), 20);
    UiRect invite = new UiRect(Math.max(content.x() + 8, content.right() - 110),
        content.y() + 8, 102, 20);

    int rowsY = state.searchVisible() ? content.y() + 38 : content.y() + 10;
    int fullRowsHeight = Math.max(1, content.bottom() - rowsY);
    int fullPageSize = Math.max(1, (fullRowsHeight + EconomyUiTheme.CARD_SPACING)
        / (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING));
    boolean needsPager = state.searchVisible() && state.rowCount() > fullPageSize;
    int rowsBottom = needsPager ? content.bottom() - 30 : content.bottom();
    UiRect rows = new UiRect(content.x(), rowsY, content.width(),
        Math.max(1, rowsBottom - rowsY));
    int pageSize = Math.max(1, (rows.height() + EconomyUiTheme.CARD_SPACING)
        / (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING));

    List<QuickAction> quickActions = state.view() == TerritoryDetailViewKind.MAIN
        ? quickActions(content) : List.of();
    List<SettingAction> settingsActions = state.view() == TerritoryDetailViewKind.SETTINGS
        ? settingsActions(content) : List.of();
    List<AccessCard> accessCards = new ArrayList<>();
    List<RuleCard> ruleCards = new ArrayList<>();
    List<TransferCard> transferCards = new ArrayList<>();
    switch (state.view()) {
      case ACCESS -> addAccessCards(state.visibleAccessRows(), accessCards, rows);
      case RULES -> addRuleCards(state.visibleRuleRows(), ruleCards, rows);
      case TRANSFER -> addTransferCards(state.visibleTransferRows(), transferCards, rows);
      case MAIN, SETTINGS -> { }
    }

    int pagerY = content.bottom() - 25;
    int pagerGroupWidth = 158;
    int pagerX = Math.max(content.x() + 8,
        content.x() + (content.width() - pagerGroupWidth) / 2);
    UiRect previous = new UiRect(pagerX, pagerY, 44, 20);
    UiRect pageText = new UiRect(pagerX + 52, pagerY, 54, 20);
    UiRect next = new UiRect(pagerX + 114, pagerY, 44, 20);
    UiRect retry = new UiRect(content.x() + Math.max(0, (content.width() - 96) / 2),
        rows.y() + Math.max(0, (rows.height() - 22) / 2), Math.min(96, content.width()), 22);

    return new Layout(scale, title, subtitle, navigationPanel, List.copyOf(navigationButtons), content,
        search, invite, rows, List.copyOf(quickActions), List.copyOf(settingsActions),
        List.of(), List.copyOf(accessCards), List.copyOf(ruleCards),
        List.copyOf(transferCards), previous, pageText, next, retry, back, pageSize);
  }

  private static List<NavigationButton> navigationButtons(UiRect panel, TerritoryDetailViewKind view) {
    TerritoryDetailAction[] actions = {
        TerritoryDetailAction.OVERVIEW,
        TerritoryDetailAction.ACCESS,
        TerritoryDetailAction.RULES,
        TerritoryDetailAction.BUFFS,
        TerritoryDetailAction.SETTINGS
    };
    List<NavigationButton> result = new ArrayList<>();
    for (int index = 0; index < actions.length; index++) {
      TerritoryDetailAction action = actions[index];
      boolean selected = switch (action) {
        case OVERVIEW -> view == TerritoryDetailViewKind.MAIN;
        case ACCESS -> view == TerritoryDetailViewKind.ACCESS;
        case RULES -> view == TerritoryDetailViewKind.RULES;
        case SETTINGS -> view == TerritoryDetailViewKind.SETTINGS || view == TerritoryDetailViewKind.TRANSFER;
        default -> false;
      };
      result.add(new NavigationButton(action,
          new UiRect(panel.x() + 8, panel.y() + 10 + index * 30,
              Math.max(1, panel.width() - 16), 22), selected));
    }
    return result;
  }

  private static List<QuickAction> quickActions(UiRect content) {
    int gap = EconomyUiTheme.CARD_SPACING;
    int width = Math.min(126, Math.max(80, (content.width() - 24 - gap) / 2));
    int y = content.bottom() - 34;
    return List.of(
        new QuickAction(TerritoryDetailAction.RESIZE,
            new UiRect(content.x() + 12, y, width, ACTION_HEIGHT)),
        new QuickAction(TerritoryDetailAction.INVITE,
            new UiRect(content.x() + 12 + width + gap, y, width, ACTION_HEIGHT)));
  }

  private static List<SettingAction> settingsActions(UiRect content) {
    TerritoryDetailAction[] actions = {
        TerritoryDetailAction.COPY_ID,
        TerritoryDetailAction.RESIZE,
        TerritoryDetailAction.TRANSFER,
        TerritoryDetailAction.DELETE
    };
    List<SettingAction> result = new ArrayList<>();
    int startY = content.y() + 14;
    for (int index = 0; index < actions.length; index++) {
      UiRect row = new UiRect(content.x() + 8, startY + index * 54,
          Math.max(1, content.width() - 16), 46);
      UiRect button = new UiRect(Math.max(row.x(), row.right() - 112), row.y() + 12, 104, 22);
      result.add(new SettingAction(actions[index], row, button));
    }
    return result;
  }

  private static void addAccessCards(List<TerritoryAccessRow> values, List<AccessCard> output,
                                     UiRect rows) {
    for (int index = 0; index < values.size(); index++) {
      int y = rows.y() + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(rows.x(), y, rows.width(), ROW_HEIGHT);
      UiRect action = new UiRect(Math.max(card.x(), card.right() - 102), card.y() + 11, 94, 20);
      output.add(new AccessCard(values.get(index), card,
          new UiRect(card.x() + 8, card.y() + 8, 26, 26),
          new UiRect(card.x() + 42, card.y() + 5, Math.max(1, action.x() - card.x() - 50), 16),
          new UiRect(card.x() + 42, card.y() + 21, Math.max(1, action.x() - card.x() - 50), 14),
          action));
    }
  }

  private static void addRuleCards(List<TerritoryRuleRow> values, List<RuleCard> output,
                                   UiRect rows) {
    for (int index = 0; index < values.size(); index++) {
      int y = rows.y() + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(rows.x(), y, rows.width(), ROW_HEIGHT);
      UiRect action = new UiRect(Math.max(card.x() + 150, card.right() - 150),
          card.y() + 11, 142, 20);
      output.add(new RuleCard(values.get(index), card,
          new UiRect(card.x() + 10, card.y() + 6, Math.max(1, action.x() - card.x() - 18), 14),
          new UiRect(card.x() + 10, card.y() + 22, Math.max(1, action.x() - card.x() - 18), 14),
          action));
    }
  }

  private static void addTransferCards(List<PlayerSummary> values, List<TransferCard> output,
                                       UiRect rows) {
    for (int index = 0; index < values.size(); index++) {
      int y = rows.y() + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(rows.x(), y, rows.width(), ROW_HEIGHT);
      UiRect action = new UiRect(Math.max(card.x(), card.right() - 102), card.y() + 11, 94, 20);
      output.add(new TransferCard(values.get(index), card,
          new UiRect(card.x() + 8, card.y() + 8, 26, 26),
          new UiRect(card.x() + 42, card.y() + 7, Math.max(1, action.x() - card.x() - 50), 14),
          new UiRect(card.x() + 42, card.y() + 22, Math.max(1, action.x() - card.x() - 50), 12),
          action));
    }
  }

  public record Layout(
      UiScale scale,
      UiRect title,
      UiRect subtitle,
      UiRect navigationPanel,
      List<NavigationButton> navigationButtons,
      UiRect content,
      UiRect search,
      UiRect inviteButton,
      UiRect rows,
      List<QuickAction> quickActions,
      List<SettingAction> settingsActions,
      List<PresetButton> presetButtons,
      List<AccessCard> accessCards,
      List<RuleCard> ruleCards,
      List<TransferCard> transferCards,
      UiRect previousButton,
      UiRect pageText,
      UiRect nextButton,
      UiRect retryButton,
      UiRect backButton,
      int pageSize) {
    public Layout {
      navigationButtons = List.copyOf(navigationButtons);
      quickActions = List.copyOf(quickActions);
      settingsActions = List.copyOf(settingsActions);
      presetButtons = List.copyOf(presetButtons);
      accessCards = List.copyOf(accessCards);
      ruleCards = List.copyOf(ruleCards);
      transferCards = List.copyOf(transferCards);
      if (pageSize < 1) throw new IllegalArgumentException("pageSize");
    }
  }

  public record NavigationButton(TerritoryDetailAction action, UiRect rect, boolean selected) {}
  public record QuickAction(TerritoryDetailAction action, UiRect rect) {}
  public record SettingAction(TerritoryDetailAction action, UiRect row, UiRect button) {}
  public record PresetButton(TerritoryRulePreset preset, UiRect rect) {}
  public record AccessCard(TerritoryAccessRow row, UiRect card, UiRect head, UiRect name,
                           UiRect status, UiRect actionButton) {}
  public record RuleCard(TerritoryRuleRow row, UiRect card, UiRect name, UiRect description,
                         UiRect actionButton) {}
  public record TransferCard(PlayerSummary player, UiRect card, UiRect head, UiRect name,
                             UiRect description, UiRect actionButton) {}
}
