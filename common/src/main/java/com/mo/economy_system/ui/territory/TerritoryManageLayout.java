package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure layout calculation shared by both target renderers. */
public final class TerritoryManageLayout {
    public static final int PLAYER_CARD_HEIGHT = 48;
    public static final int ACTION_BUTTON_HEIGHT = 22;
    public static final int ACTION_BUTTON_SPACING = 6;
    public static final int PAGE_HINT_HEIGHT = 45;
    private static final int ACTION_PANEL_WIDTH = 180;
    private static final int LIST_START_Y = 55;
    private static final int SEARCH_WIDTH = 200;

    private TerritoryManageLayout() {
    }

    public static Layout calculate(int physicalWidth, int physicalHeight, TerritoryManageState state) {
        UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
                EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
        int width = scale.virtualWidth();
        int height = scale.virtualHeight();
        int panel = EconomyUiTheme.PANEL_PADDING;
        int rightWidth = Math.max(140, Math.min(ACTION_PANEL_WIDTH,
                width - panel * 3 - 240));
        int listWidth = Math.max(0, width - panel * 3 - rightWidth);
        int listHeight = Math.max(0, height - LIST_START_Y - PAGE_HINT_HEIGHT);
        int rowCount = Math.max(0, (listHeight + EconomyUiTheme.CARD_SPACING)
                / (PLAYER_CARD_HEIGHT + EconomyUiTheme.CARD_SPACING));
        int pageSize = Math.max(1, rowCount);
        List<MemberCard> cards = new ArrayList<>();
        List<MemberRow> visible = state.visibleMembers();
        int cardWidth = Math.max(0, listWidth);
        for (int i = 0; i < visible.size() && i < pageSize; i++) {
            int y = LIST_START_Y + i * (PLAYER_CARD_HEIGHT + EconomyUiTheme.CARD_SPACING);
            cards.add(new MemberCard(visible.get(i),
                    new UiRect(panel, y, cardWidth, PLAYER_CARD_HEIGHT),
                    new UiRect(Math.max(panel, panel + cardWidth - 72), y + 14, 66, 20)));
        }
        int actionX = panel + listWidth + panel;
        int actionY = LIST_START_Y;
        TerritoryManageAction[] actions = {TerritoryManageAction.COPY_ID,
                TerritoryManageAction.MODIFY_MODE, TerritoryManageAction.INVITE,
                TerritoryManageAction.BUFFS, TerritoryManageAction.ACCESS,
                TerritoryManageAction.PERMISSIONS, TerritoryManageAction.TRANSFER,
                TerritoryManageAction.DELETE};
        List<ActionButton> actionButtons = new ArrayList<>();
        int actionHeight = Math.max(120, 34 + actions.length * ACTION_BUTTON_HEIGHT
                + (actions.length - 1) * ACTION_BUTTON_SPACING);
        UiRect actionPanel = new UiRect(actionX, actionY, rightWidth, actionHeight);
        int actionButtonY = actionY + 34;
        for (TerritoryManageAction action : actions) {
            actionButtons.add(new ActionButton(action,
                    new UiRect(actionX + 6, actionButtonY, Math.max(0, rightWidth - 12), ACTION_BUTTON_HEIGHT)));
            actionButtonY += ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;
        }
        UiRect previous = new UiRect(panel, Math.max(LIST_START_Y, height - PAGE_HINT_HEIGHT + 8), 72, 20);
        UiRect next = new UiRect(Math.max(panel, panel + listWidth - 72), previous.y(), 72, 20);
        UiRect back = new UiRect(Math.max(panel, width - panel - 72), height - panel - 20, 72, 20);
        UiRect retry = new UiRect(panel + Math.max(0, (listWidth - 96) / 2),
                LIST_START_Y + Math.max(0, (listHeight - 22) / 2), Math.min(96, listWidth), 22);
        UiRect search = new UiRect(panel, panel, Math.min(SEARCH_WIDTH, listWidth), 20);
        UiRect memberHeader = new UiRect(panel, LIST_START_Y - 14, listWidth, 12);
        UiRect pageText = new UiRect(panel + Math.max(0, (listWidth - 64) / 2), previous.y(),
                Math.min(64, listWidth), previous.height());
        return new Layout(scale,
                new UiRect(panel, height - panel - 12, Math.max(0, width - panel * 2 - 84), 12),
                search, memberHeader, new UiRect(panel, LIST_START_Y, listWidth, listHeight), actionPanel,
                List.copyOf(cards), List.copyOf(actionButtons), previous, next, pageText, retry, back, pageSize);
    }

    public record Layout(UiScale scale, UiRect title, UiRect search, UiRect memberHeader,
                         UiRect memberPanel, UiRect actionPanel,
                         List<MemberCard> cards, List<ActionButton> actionButtons,
                         UiRect previousButton, UiRect nextButton, UiRect pageText,
                         UiRect retryButton, UiRect backButton, int pageSize) {
        public Layout {
            cards = List.copyOf(cards);
            actionButtons = List.copyOf(actionButtons);
        }
    }

    public record MemberCard(MemberRow member, UiRect card, UiRect kickButton) {
    }

    public record ActionButton(TerritoryManageAction action, UiRect rect) {
    }
}
