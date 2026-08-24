package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

public final class BuffManageLayout {
    public static final int BACKGROUND_COLOR = 0xB0000000;
    public static final int MIN_CARD_WIDTH = 280;
    public static final int CARD_HEIGHT = 98;
    public static final int LIST_START_Y = 60;
    public static final int ACTION_WIDTH = 92;
    public static final int ACTION_HEIGHT = 22;
    private static final int MAX_COLUMNS = 2;
    private static final int FOOTER_RESERVED = 48;
    private BuffManageLayout() {}
    public static Layout calculate(int physicalWidth, int physicalHeight, BuffManageState state) {
        UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
        int width = scale.virtualWidth(), height = scale.virtualHeight();
        int panel = EconomyUiTheme.PANEL_PADDING;
        int listWidth = Math.max(1, width - panel * 2);
        int cardHeight = CARD_HEIGHT, spacing = EconomyUiTheme.CARD_SPACING;
        int columns = Math.min(MAX_COLUMNS,
                Math.max(1, (listWidth + spacing) / (MIN_CARD_WIDTH + spacing)));
        int cardWidth = Math.max(1, (listWidth - spacing * (columns - 1)) / columns);
        int availableHeight = Math.max(1, height - LIST_START_Y - FOOTER_RESERVED);
        int rows = Math.max(1, (availableHeight + spacing) / (cardHeight + spacing));
        int pageSize = columns * rows;
        List<Card> cards = new ArrayList<>();
        List<BuffRow> visible = state.visibleBuffs();
        for (int i = 0; i < visible.size() && i < pageSize; i++) {
            int col = i % columns, row = i / columns;
            int x = panel + col * (cardWidth + spacing), y = LIST_START_Y + row * (cardHeight + spacing);
            UiRect card = new UiRect(x, y, Math.min(cardWidth, width - x - panel), cardHeight);
            int textX = card.x() + Math.min(52, card.width());
            int rightPad = 12;
            int levelWidth = Math.min(72, Math.max(1, card.width() / 3));
            int actionWidth = Math.min(ACTION_WIDTH, Math.max(1, card.width() - 24));
            int actionX = Math.max(card.x() + 12, card.right() - rightPad - actionWidth);
            int detailWidth = Math.max(1, actionX - textX - 8);
            cards.add(new Card(visible.get(i), card,
                    new UiRect(actionX, card.bottom() - 10 - ACTION_HEIGHT,
                            actionWidth, ACTION_HEIGHT),
                    new UiRect(card.x() + 12, card.y() + 34, Math.min(28, card.width()), Math.min(28, card.height())),
                    new UiRect(textX, card.y() + 9,
                            Math.max(1, card.width() - (textX - card.x()) - levelWidth - rightPad - 8), 14),
                    new UiRect(card.right() - rightPad - levelWidth, card.y() + 9,
                            levelWidth, 14),
                    new UiRect(textX, card.y() + 27,
                            Math.max(1, card.right() - rightPad - textX), 2),
                    new UiRect(textX, card.y() + 34,
                            Math.max(1, card.right() - rightPad - textX), 14),
                    new UiRect(textX, card.y() + 53, detailWidth, 14),
                    new UiRect(textX, card.y() + 72, detailWidth, 14)));
        }
        int pageY = height - 40;
        UiRect pageText = new UiRect(Math.max(panel, width / 2 - 28), pageY, 56, 22);
        UiRect previous = new UiRect(Math.max(panel, pageText.x() - 62), pageY, 50, 22);
        UiRect next = new UiRect(Math.min(width - panel - 50, pageText.right() + 12), pageY, 50, 22);
        UiRect retry = new UiRect(panel + Math.max(0, (listWidth - 96) / 2),
                LIST_START_Y + Math.max(0, (availableHeight - 22) / 2),
                Math.min(96, listWidth), 22);
        UiRect footerTitle = new UiRect(panel, height - 31,
                Math.max(0, previous.x() - panel - 8), 19);
        UiRect escHint = new UiRect(Math.max(panel, width - panel - 180), height - 20, 180, 12);
        return new Layout(scale, new UiRect(panel, 20, Math.min(200, listWidth), 20),
                new UiRect(panel, 40, listWidth, 14), List.copyOf(cards), previous, next,
                pageText, retry, footerTitle, escHint, pageSize);
    }
    public record Layout(UiScale scale, UiRect search, UiRect header, List<Card> cards,
                         UiRect previousButton, UiRect nextButton, UiRect pageText,
                         UiRect retryButton, UiRect footerTitle, UiRect escHint, int pageSize) {
        public Layout { cards = List.copyOf(cards); }
    }
    public record Card(BuffRow buff, UiRect card, UiRect actionButton, UiRect icon,
                       UiRect name, UiRect level, UiRect levelTrack, UiRect status, UiRect cost,
                       UiRect availability) {}
}
