package com.mo.economy_system.ui.territory;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiText;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

/** Pure Territory Manage geometry shared by Forge and NeoForge renderers. */
public final class TerritoryManageLayout {
    public static final int PLAYER_CARD_HEIGHT = 48;
    public static final int PLAYER_ICON_SIZE = 32;
    public static final int ACTION_BUTTON_HEIGHT = 22;
    public static final int ACTION_BUTTON_SPACING = 6;
    public static final int PAGE_HINT_HEIGHT = 45;
    public static final int ACTION_PANEL_WIDTH = 180;
    public static final int LIST_START_Y = 55;
    public static final int KICK_BUTTON_WIDTH = 66;
    public static final int PAGE_BUTTON_WIDTH = 50;
    public static final int PAGE_BUTTON_HEIGHT = 24;
    public static final int FOOTER_MAX_CONTENT_WIDTH = 240;
    public static final int FOOTER_ICON_SIZE = 10;
    public static final int FOOTER_ICON_ADVANCE = 14;

    private TerritoryManageLayout() {
    }

    public static Layout calculate(int physicalWidth, int physicalHeight,
                                   TerritoryManageState state) {
        return calculate(physicalWidth, physicalHeight, state, UiTextMetrics.APPROXIMATE);
    }

    public static Layout calculate(int physicalWidth, int physicalHeight,
                                   TerritoryManageState state, UiTextMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics cannot be null");
        }
        UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
                EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
        int width = scale.virtualWidth();
        int height = scale.virtualHeight();
        int panel = EconomyUiTheme.PANEL_PADDING;

        // This is intentionally the same min-left-width/right-panel calculation as the legacy
        // 1.21.1 screen.  Do not replace it with an approximate proportional split.
        int minLeftWidth = 240;
        int rightWidth = ACTION_PANEL_WIDTH;
        int leftWidth = width - panel * 3 - rightWidth;
        if (leftWidth < minLeftWidth) {
            rightWidth = Math.max(140, width - panel * 3 - minLeftWidth);
        }
        rightWidth = Math.max(140, rightWidth);
        leftWidth = Math.max(0, width - panel * 3 - rightWidth);

        int listHeight = Math.max(0, height - LIST_START_Y - PAGE_HINT_HEIGHT);
        int rowCount = Math.max(1, (listHeight + EconomyUiTheme.CARD_SPACING)
                / (PLAYER_CARD_HEIGHT + EconomyUiTheme.CARD_SPACING));
        int pageSize = rowCount;
        List<MemberRow> filtered = state.filteredMembers();
        int start = Math.min(state.page() * pageSize, filtered.size());
        int end = Math.min(filtered.size(), start + pageSize);
        List<MemberCard> cards = new ArrayList<>();
        int cardWidth = leftWidth;
        // A viewport narrower than the reference card cannot expose a useful kick hitbox. Keep
        // the panel valid and let the empty/scroll state communicate that no card is drawable.
        if (cardWidth >= KICK_BUTTON_WIDTH + 16) {
            for (int i = start; i < end; i++) {
                int row = i - start;
                int cardY = LIST_START_Y + row * (PLAYER_CARD_HEIGHT + EconomyUiTheme.CARD_SPACING);
                UiRect card = new UiRect(panel, cardY, cardWidth, PLAYER_CARD_HEIGHT);
                UiRect kick = new UiRect(card.right() - KICK_BUTTON_WIDTH - 8,
                        cardY + (PLAYER_CARD_HEIGHT - ACTION_BUTTON_HEIGHT) / 2,
                        KICK_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT);
                cards.add(new MemberCard(filtered.get(i), card, kick));
            }
        }

        int actionX = panel + leftWidth + panel;
        int actionY = LIST_START_Y;
        TerritoryManageAction[] actions = {
                TerritoryManageAction.COPY_ID,
                TerritoryManageAction.MODIFY_MODE,
                TerritoryManageAction.INVITE,
                TerritoryManageAction.BUFFS,
                TerritoryManageAction.ACCESS,
                TerritoryManageAction.PERMISSIONS,
                TerritoryManageAction.TRANSFER,
                TerritoryManageAction.DELETE
        };
        int headerHeight = metrics.lineHeight() + 14;
        int contentHeight = actions.length * ACTION_BUTTON_HEIGHT
                + (actions.length - 1) * ACTION_BUTTON_SPACING;
        int actionHeight = Math.max(120, headerHeight + contentHeight + 8);
        UiRect actionPanel = new UiRect(actionX, actionY, rightWidth, actionHeight);
        List<ActionButton> actionButtons = new ArrayList<>();
        int actionButtonY = actionY + headerHeight;
        for (TerritoryManageAction action : actions) {
            actionButtons.add(new ActionButton(action,
                    new UiRect(actionX + 6, actionButtonY,
                            Math.max(0, rightWidth - 12), ACTION_BUTTON_HEIGHT)));
            actionButtonY += ACTION_BUTTON_HEIGHT + ACTION_BUTTON_SPACING;
        }

        String pageText = (state.page() + 1) + " / " + state.totalPages();
        int pageTextWidth = Math.max(1, metrics.width(pageText));
        int pageTextX = Math.max(0, width / 2 - pageTextWidth / 2);
        int pageY = Math.max(0, height - 40);
        UiRect previous = new UiRect(Math.max(0, pageTextX - PAGE_BUTTON_WIDTH - 12),
                pageY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
        UiRect next = new UiRect(Math.min(Math.max(0, width - PAGE_BUTTON_WIDTH),
                pageTextX + pageTextWidth + 12), pageY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT);
        UiRect pageRect = new UiRect(pageTextX, Math.max(0, height - 35), pageTextWidth,
                Math.max(1, metrics.lineHeight()));

        String footerText = "领地管理 · " + state.territoryName();
        int footerContentWidth = metrics.width(footerText) + FOOTER_ICON_ADVANCE;
        float footerScale = Math.min(1.0f,
                (float) FOOTER_MAX_CONTENT_WIDTH / Math.max(1, footerContentWidth));
        int footerWidth = Math.max(1, (int) (footerContentWidth * footerScale) + 16);
        int footerHeight = Math.max(1, metrics.lineHeight() + 10);
        UiRect footer = new UiRect(panel,
                Math.max(0, height - panel - footerHeight), footerWidth, footerHeight);
        String esc = "按 ESC 返回";
        int escWidth = Math.max(1, metrics.width(esc));
        UiRect escHint = new UiRect(Math.max(0, width - panel - escWidth),
                Math.max(0, height - panel - metrics.lineHeight()), escWidth,
                Math.max(1, metrics.lineHeight()));

        UiRect memberHeader = new UiRect(panel,
                Math.max(0, LIST_START_Y - metrics.lineHeight() - 3), leftWidth,
                Math.max(1, metrics.lineHeight()));
        UiRect memberPanel = new UiRect(panel, LIST_START_Y, leftWidth, listHeight);
        UiRect retry = new UiRect(panel + Math.max(0, (leftWidth - 96) / 2),
                LIST_START_Y + Math.max(0, (listHeight - ACTION_BUTTON_HEIGHT) / 2),
                Math.min(96, leftWidth), ACTION_BUTTON_HEIGHT);
        return new Layout(scale, metrics, memberHeader, memberPanel,
                actionPanel, List.copyOf(cards), List.copyOf(actionButtons), previous, next,
                pageRect, retry, footer, footerText, footerScale, escHint, pageSize);
    }

    public record Layout(UiScale scale, UiTextMetrics metrics,
                         UiRect memberHeader, UiRect memberPanel, UiRect actionPanel,
                         List<MemberCard> cards, List<ActionButton> actionButtons,
                         UiRect previousButton, UiRect nextButton, UiRect pageText,
                         UiRect retryButton, UiRect footer, String footerText,
                         float footerContentScale, UiRect escHint, int pageSize) {
        public Layout {
            cards = List.copyOf(cards);
            actionButtons = List.copyOf(actionButtons);
        }

        public String truncatedTerritoryName(String territoryName) {
            return UiText.truncate(metrics, territoryName, Math.max(1, actionPanel.width() - 16));
        }
    }

    public record MemberCard(MemberRow member, UiRect card, UiRect kickButton) {
        public UiRect nameRect(UiTextMetrics metrics) {
            return new UiRect(card.x() + 48, card.y() + 7,
                    Math.max(0, kickButton.x() - (card.x() + 48) - 4), metrics.lineHeight());
        }

        public UiRect uuidRect(UiTextMetrics metrics) {
            return new UiRect(card.x() + 48,
                    card.y() + 7 + metrics.lineHeight() + 2,
                    Math.max(0, kickButton.x() - (card.x() + 48) - 4), metrics.lineHeight());
        }
    }

    public record ActionButton(TerritoryManageAction action, UiRect rect) {
    }
}
