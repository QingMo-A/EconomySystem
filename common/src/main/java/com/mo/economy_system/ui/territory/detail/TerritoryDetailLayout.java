package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure, loader-neutral geometry for territory administration detail views. */
public final class TerritoryDetailLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  public static final int ROW_HEIGHT = 42;
  public static final int ACTION_HEIGHT = 22;
  private static final int TITLE_Y = 18;
  private static final int SEARCH_Y = 45;
  private static final int LIST_Y = 76;
  private static final int FOOTER_HEIGHT = 36;
  private static final int ACTION_WIDTH = 94;

  private TerritoryDetailLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryDetailState state) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int panel = EconomyUiTheme.PANEL_PADDING;
    int contentWidth = Math.max(1, width - panel * 2);
    int contentHeight = Math.max(1, height - LIST_Y - FOOTER_HEIGHT);
    int pageSize = Math.max(1, (contentHeight + EconomyUiTheme.CARD_SPACING)
        / (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING));
    UiRect title = new UiRect(panel, TITLE_Y, contentWidth, 14);
    UiRect subtitle = new UiRect(panel, TITLE_Y + 16, contentWidth, 12);
    UiRect search = new UiRect(panel, SEARCH_Y, Math.min(220, contentWidth), 20);
    UiRect rows = new UiRect(panel, LIST_Y, contentWidth, contentHeight);
    UiRect previous = new UiRect(Math.max(panel, width / 2 - 76), height - 28, 58, 20);
    UiRect pageText = new UiRect(Math.max(panel, width / 2 - 12), height - 28, 24, 20);
    UiRect next = new UiRect(Math.min(width - panel - 58, width / 2 + 18), height - 28, 58, 20);
    UiRect back = new UiRect(panel, height - 28, 72, 20);
    UiRect retry = new UiRect(panel + Math.max(0, (contentWidth - 96) / 2),
        LIST_Y + Math.max(0, (contentHeight - 22) / 2), Math.min(96, contentWidth), 22);

    List<MainAction> mainActions = mainActions(width, height, panel, contentWidth);
    List<AccessCard> accessCards = new ArrayList<>();
    List<RuleCard> ruleCards = new ArrayList<>();
    List<TransferCard> transferCards = new ArrayList<>();
    switch (state.view()) {
      case ACCESS -> addAccessCards(state.visibleAccessRows(), accessCards, panel, contentWidth);
      case RULES -> addRuleCards(state.visibleRuleRows(), ruleCards, panel, contentWidth);
      case TRANSFER -> addTransferCards(state.visibleTransferRows(), transferCards, panel, contentWidth);
      case MAIN -> { }
    }
    return new Layout(scale, title, subtitle, search, rows, List.copyOf(mainActions),
        List.copyOf(accessCards), List.copyOf(ruleCards), List.copyOf(transferCards),
        previous, pageText, next, retry, back, pageSize);
  }

  private static List<MainAction> mainActions(int width, int height, int panel, int contentWidth) {
    TerritoryDetailAction[] actions = {
        TerritoryDetailAction.RESIZE,
        TerritoryDetailAction.BUFFS,
        TerritoryDetailAction.ACCESS,
        TerritoryDetailAction.RULES,
        TerritoryDetailAction.TRANSFER
    };
    int buttonWidth = Math.min(240, Math.max(120, contentWidth));
    int x = Math.max(panel, (width - buttonWidth) / 2);
    int startY = Math.max(LIST_Y, (height - (actions.length * ACTION_HEIGHT
        + (actions.length - 1) * EconomyUiTheme.CARD_SPACING)) / 2);
    List<MainAction> result = new ArrayList<>();
    for (int index = 0; index < actions.length; index++) {
      result.add(new MainAction(actions[index],
          new UiRect(x, startY + index * (ACTION_HEIGHT + EconomyUiTheme.CARD_SPACING),
              buttonWidth, ACTION_HEIGHT)));
    }
    return result;
  }

  private static void addAccessCards(List<TerritoryAccessRow> values, List<AccessCard> output,
                                     int panel, int width) {
    for (int index = 0; index < values.size(); index++) {
      int y = LIST_Y + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(panel, y, width, ROW_HEIGHT);
      UiRect action = actionRect(card);
      output.add(new AccessCard(values.get(index), card,
          new UiRect(card.x() + 8, card.y() + 8, 26, 26),
          new UiRect(card.x() + 42, card.y() + 5, Math.max(1, action.x() - card.x() - 50), 16),
          new UiRect(card.x() + 42, card.y() + 21, Math.max(1, action.x() - card.x() - 50), 14),
          action));
    }
  }

  private static void addRuleCards(List<TerritoryRuleRow> values, List<RuleCard> output,
                                    int panel, int width) {
    for (int index = 0; index < values.size(); index++) {
      int y = LIST_Y + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(panel, y, width, ROW_HEIGHT);
      UiRect action = actionRect(card);
      output.add(new RuleCard(values.get(index), card,
          new UiRect(card.x() + 10, card.y() + 6, Math.max(1, action.x() - card.x() - 18), 14),
          new UiRect(card.x() + 10, card.y() + 22, Math.max(1, action.x() - card.x() - 18), 14),
          action));
    }
  }

  private static void addTransferCards(List<PlayerSummary> values, List<TransferCard> output,
                                        int panel, int width) {
    for (int index = 0; index < values.size(); index++) {
      int y = LIST_Y + index * (ROW_HEIGHT + EconomyUiTheme.CARD_SPACING);
      UiRect card = new UiRect(panel, y, width, ROW_HEIGHT);
      UiRect action = actionRect(card);
      output.add(new TransferCard(values.get(index), card,
          new UiRect(card.x() + 8, card.y() + 8, 26, 26),
          new UiRect(card.x() + 42, card.y() + 7, Math.max(1, action.x() - card.x() - 50), 14),
          new UiRect(card.x() + 42, card.y() + 22, Math.max(1, action.x() - card.x() - 50), 12),
          action));
    }
  }

  private static UiRect actionRect(UiRect card) {
    int width = Math.min(ACTION_WIDTH, Math.max(1, card.width() / 3));
    return new UiRect(Math.max(card.x(), card.right() - width - 8), card.y() + 11, width, 20);
  }

  public record Layout(
      UiScale scale,
      UiRect title,
      UiRect subtitle,
      UiRect search,
      UiRect rows,
      List<MainAction> mainActions,
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
      mainActions = List.copyOf(mainActions);
      accessCards = List.copyOf(accessCards);
      ruleCards = List.copyOf(ruleCards);
      transferCards = List.copyOf(transferCards);
      if (pageSize < 1) throw new IllegalArgumentException("pageSize");
    }
  }

  public record MainAction(TerritoryDetailAction action, UiRect rect) {}
  public record AccessCard(TerritoryAccessRow row, UiRect card, UiRect head, UiRect name,
                           UiRect status, UiRect actionButton) {}
  public record RuleCard(TerritoryRuleRow row, UiRect card, UiRect name, UiRect description,
                         UiRect actionButton) {}
  public record TransferCard(PlayerSummary player, UiRect card, UiRect head, UiRect name,
                             UiRect description, UiRect actionButton) {}
}
