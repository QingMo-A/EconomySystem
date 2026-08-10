package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;

public final class BuffManageLayout {
    public static final int BACKGROUND_COLOR = 0xB0000000;
    public static final int CARD_WIDTH = 200;
    public static final int CARD_HEIGHT = 88;
    public static final int LIST_START_Y = 55;
    public static final int ACTION_WIDTH = 70;
    public static final int ACTION_HEIGHT = 18;
    private BuffManageLayout() {}
    public static Layout calculate(int physicalWidth, int physicalHeight, BuffManageState state) {
        UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
        int width = scale.virtualWidth(), height = scale.virtualHeight();
        int panel = EconomyUiTheme.PANEL_PADDING;
        int listWidth = Math.max(1, width - panel * 2);
        int cardWidth = CARD_WIDTH, cardHeight = CARD_HEIGHT, spacing = EconomyUiTheme.CARD_SPACING;
        int columns = Math.max(1, (listWidth + spacing) / (cardWidth + spacing));
        int rows = Math.max(1, (height - LIST_START_Y - 45 + spacing) / (cardHeight + spacing));
        int pageSize = columns * rows;
        List<Card> cards = new ArrayList<>();
        List<BuffRow> visible = state.visibleBuffs();
        for (int i = 0; i < visible.size() && i < pageSize; i++) {
            int col = i % columns, row = i / columns;
            int x = panel + col * (cardWidth + spacing), y = LIST_START_Y + row * (cardHeight + spacing);
            UiRect card = new UiRect(x, y, Math.min(cardWidth, width - x - panel), cardHeight);
            int textX = card.x() + Math.min(48, card.width());
            int textWidth = Math.max(1, card.width() - (textX - card.x()) - 8);
            int actionX = Math.max(card.x(), card.right() - 8 - Math.min(ACTION_WIDTH, card.width()));
            cards.add(new Card(visible.get(i), card,
                    new UiRect(actionX, card.bottom() - 8 - Math.min(ACTION_HEIGHT, card.height()),
                            Math.min(ACTION_WIDTH, card.width()), Math.min(ACTION_HEIGHT, card.height())),
                    new UiRect(card.x() + 8, card.y() + 28, Math.min(32, card.width()), Math.min(32, card.height())),
                    new UiRect(textX, card.y() + 5, Math.max(1, textWidth - 62), 16),
                    new UiRect(Math.max(textX, card.right() - 68), card.y() + 5,
                            Math.min(60, textWidth), 16),
                    new UiRect(textX, card.y() + 23, textWidth, 14),
                    new UiRect(textX, card.y() + 39, textWidth, 14),
                    new UiRect(textX, card.y() + 54, Math.max(1, actionX - textX - 4), 14)));
        }
        int pageY = height - 40;
        UiRect pageText = new UiRect(Math.max(panel, width / 2 - 28), pageY, 56, 22);
        UiRect previous = new UiRect(Math.max(panel, pageText.x() - 62), pageY, 50, 22);
        UiRect next = new UiRect(Math.min(width - panel - 50, pageText.right() + 12), pageY, 50, 22);
        UiRect retry = new UiRect(panel + Math.max(0, (listWidth - 96) / 2),
                LIST_START_Y + Math.max(0, (height - LIST_START_Y - 45 - 22) / 2),
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
                       UiRect name, UiRect level, UiRect status, UiRect cost,
                       UiRect availability) {}
}
