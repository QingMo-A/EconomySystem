package com.mo.economy_system.screen.storybook_system;

import com.mo.economy_system.core.storybook_system.StoryBookEntryViewData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.storybook_system.Packet_UpdateStoryBookOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Screen_StoryBookCatalog extends Screen {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final int BOOK_COLOR = 0xFF6B4B2D;
    private static final int BOOK_DARK = 0xFF51361F;
    private static final int PAGE_COLOR = 0xFFF0DFC1;
    private static final int PAGE_SHADOW = 0x30A0784A;
    private static final int PAGE_BORDER = 0x80755634;
    private static final int CARD_PIN = 0xFFC44B36;
    private static final int CARD_TITLE = 0xFF51341D;
    private static final int CARD_TEXT = 0xFF6D4A2A;
    private static final int CARD_BG_A = 0xFFEEDAAE;
    private static final int CARD_BG_B = 0xFFE7D0A1;
    private static final int CARD_BG_HOVER = 0xFFF4E5C1;
    private static final int CHAPTER_BG = 0xFFE8D5AF;
    private static final int CHAPTER_BG_HOVER = 0xFFF2E3C4;
    private static final int CHAPTER_ACCENT = 0xFF8A5B31;
    private static final int BUTTON_TEXT = 0xFF7A5634;
    private static final int BUTTON_TEXT_HOVER = 0xFF9A6A3B;
    private static final int BUTTON_TEXT_DISABLED = 0x997B6752;
    private static final int OVERLAY = 0x7A140D07;

    private static final int CARD_SIDE_PADDING = 14;
    private static final int CARD_HEIGHT = 88;
    private static final int CARD_GAP_Y = 16;
    private static final int ROWS_PER_PAGE = 2;
    private static final int COLS_PER_SPREAD = 2;
    private static final int ITEMS_PER_PAGE = ROWS_PER_PAGE * COLS_PER_SPREAD;
    private static final int NAV_BUTTON_WIDTH = 54;
    private static final int NAV_BUTTON_HEIGHT = 18;
    private static final int BACK_BUTTON_WIDTH = 52;
    private static final int BACK_BUTTON_HEIGHT = 18;

    private final List<StoryCard> allCards = new ArrayList<>();
    private final List<StoryCard> visibleCards = new ArrayList<>();
    private final List<ChapterCard> chapterCards = new ArrayList<>();

    private int bookX;
    private int bookY;
    private int bookWidth;
    private int bookHeight;
    private int leftPageX;
    private int rightPageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;
    private int cardWidth;

    private Integer selectedChapterId;
    private int currentPage;

    private StoryCard draggingCard;
    private double dragOffsetX;
    private double dragOffsetY;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private boolean dirtyOrder;

    public Screen_StoryBookCatalog(List<StoryBookEntryViewData> entries) {
        super(Component.literal("随记本"));
        for (StoryBookEntryViewData entry : entries) {
            allCards.add(new StoryCard(entry));
        }
        rebuildChapterCards();
    }

    @Override
    protected void init() {
        layoutBook();
        refreshVisibleState();
    }

    private void layoutBook() {
        bookWidth = Math.min(760, this.width - 70);
        bookHeight = Math.min(430, this.height - 60);
        bookX = (this.width - bookWidth) / 2;
        bookY = (this.height - bookHeight) / 2;
        pageY = bookY + 24;
        pageHeight = bookHeight - 48;
        pageWidth = (bookWidth - 62) / 2;
        leftPageX = bookX + 20;
        rightPageX = bookX + bookWidth - pageWidth - 20;
        cardWidth = pageWidth - CARD_SIDE_PADDING * 2;
    }

    private void rebuildChapterCards() {
        Map<Integer, ChapterStats> statsByChapter = new LinkedHashMap<>();
        for (StoryCard card : allCards) {
            ChapterStats stats = statsByChapter.computeIfAbsent(card.entry.getChapterId(), key -> new ChapterStats());
            stats.total++;
            if (card.entry.isRead()) {
                stats.read++;
            }
        }

        chapterCards.clear();
        statsByChapter.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> chapterCards.add(new ChapterCard(entry.getKey(), entry.getValue().total, entry.getValue().read)));
    }

    private void refreshVisibleState() {
        if (selectedChapterId == null) {
            layoutChapterCards();
        } else {
            layoutStoryCardsForSelectedChapter();
        }
    }

    private void layoutChapterCards() {
        visibleCards.clear();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, chapterCards.size());
        for (int i = 0; i < chapterCards.size(); i++) {
            ChapterCard chapterCard = chapterCards.get(i);
            chapterCard.visible = i >= start && i < end;
            if (!chapterCard.visible) {
                continue;
            }
            int pageIndex = i - start;
            int column = pageIndex / ROWS_PER_PAGE;
            int row = pageIndex % ROWS_PER_PAGE;
            chapterCard.x = getColumnX(column);
            chapterCard.y = getRowY(row);
        }
    }

    private void layoutStoryCardsForSelectedChapter() {
        visibleCards.clear();
        List<StoryCard> chapterEntries = getCardsForSelectedChapter();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, chapterEntries.size());
        for (int i = start; i < end; i++) {
            StoryCard card = chapterEntries.get(i);
            int pageIndex = i - start;
            int column = pageIndex / ROWS_PER_PAGE;
            int row = pageIndex % ROWS_PER_PAGE;
            int x = getColumnX(column);
            int y = getRowY(row);
            card.targetX = x;
            card.targetY = y;
            if (card != draggingCard) {
                card.x = x;
                card.y = y;
            }
            visibleCards.add(card);
        }
    }

    private int getColumnX(int column) {
        return (column == 0 ? leftPageX : rightPageX) + CARD_SIDE_PADDING;
    }

    private int getRowY(int row) {
        return pageY + 18 + row * (CARD_HEIGHT + CARD_GAP_Y);
    }

    private List<StoryCard> getCardsForSelectedChapter() {
        List<StoryCard> chapterCards = new ArrayList<>();
        for (StoryCard card : allCards) {
            if (selectedChapterId != null && card.entry.getChapterId() == selectedChapterId) {
                chapterCards.add(card);
            }
        }
        return chapterCards;
    }

    private int getCurrentPageCount() {
        int itemCount = selectedChapterId == null ? chapterCards.size() : getCardsForSelectedChapter().size();
        return Math.max(1, (int) Math.ceil(itemCount / (double) ITEMS_PER_PAGE));
    }

    private void clampCurrentPage() {
        currentPage = Math.max(0, Math.min(currentPage, getCurrentPageCount() - 1));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, OVERLAY);
        layoutBook();
        clampCurrentPage();
        refreshVisibleState();
        renderBook(guiGraphics);
        renderHeader(guiGraphics);

        if (selectedChapterId == null) {
            renderChapterView(guiGraphics, mouseX, mouseY);
        } else {
            renderChapterContentView(guiGraphics, mouseX, mouseY);
        }

        renderNavigation(guiGraphics, mouseX, mouseY);
        renderFooter(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderBook(GuiGraphics guiGraphics) {
        guiGraphics.fill(RenderType.gui(), bookX, bookY, bookX + bookWidth, bookY + bookHeight, BOOK_DARK);
        guiGraphics.fill(RenderType.gui(), bookX + 6, bookY + 6, bookX + bookWidth - 6, bookY + bookHeight - 6, BOOK_COLOR);

        drawPage(guiGraphics, leftPageX, pageY, pageWidth, pageHeight);
        drawPage(guiGraphics, rightPageX, pageY, pageWidth, pageHeight);

        guiGraphics.fill(RenderType.gui(), bookX + bookWidth / 2 - 12, bookY + 16, bookX + bookWidth / 2 + 12, bookY + bookHeight - 16, BOOK_DARK);
        guiGraphics.fill(RenderType.gui(), bookX + bookWidth / 2 - 4, bookY + 28, bookX + bookWidth / 2 + 4, bookY + bookHeight - 28, 0x40FFFFFF);
    }

    private void drawPage(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(RenderType.gui(), x + 4, y + 6, x + width + 4, y + height + 6, PAGE_SHADOW);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, PAGE_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, PAGE_BORDER);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, PAGE_BORDER);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, PAGE_BORDER);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, PAGE_BORDER);
    }

    private void renderHeader(GuiGraphics guiGraphics) {
        Component title = Component.literal("随记本");
        Component subtitle = selectedChapterId == null
                ? Component.literal("先翻看章节，再进入该章节的片段")
                : Component.literal(getChapterLabel(selectedChapterId) + " · 点击卡片阅读，拖动卡片交换顺序");
        guiGraphics.drawCenteredString(MC.font, title, this.width / 2, bookY - 18, 0xFFF4E7CF);
        guiGraphics.drawCenteredString(MC.font, subtitle, this.width / 2, bookY - 4, 0xFFD6C1A0);
    }

    private void renderChapterView(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (ChapterCard chapterCard : chapterCards) {
            if (!chapterCard.visible) {
                continue;
            }
            boolean hovered = chapterCard.isMouseOver(mouseX, mouseY, cardWidth);
            renderChapterCard(guiGraphics, chapterCard, hovered);
        }
    }

    private void renderChapterContentView(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        StoryCard hoveredCard = null;
        for (StoryCard card : visibleCards) {
            if (card != draggingCard && card.isMouseOver(mouseX, mouseY, cardWidth)) {
                hoveredCard = card;
            }
            if (card != draggingCard) {
                renderStoryCard(guiGraphics, card, card == hoveredCard);
            }
        }

        if (draggingCard != null) {
            renderStoryCard(guiGraphics, draggingCard, true);
        }
    }

    private void renderChapterCard(GuiGraphics guiGraphics, ChapterCard chapterCard, boolean hovered) {
        int x = chapterCard.x;
        int y = chapterCard.y;
        int bgColor = hovered ? CHAPTER_BG_HOVER : CHAPTER_BG;

        guiGraphics.fill(RenderType.gui(), x + 3, y + 5, x + cardWidth + 3, y + CARD_HEIGHT + 5, 0x28160D07);
        guiGraphics.fill(RenderType.gui(), x, y, x + cardWidth, y + CARD_HEIGHT, bgColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 8, y + CARD_HEIGHT, CHAPTER_ACCENT);
        guiGraphics.fill(RenderType.gui(), x, y, x + cardWidth, y + 1, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x, y + CARD_HEIGHT - 1, x + cardWidth, y + CARD_HEIGHT, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + CARD_HEIGHT, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x + cardWidth - 1, y, x + cardWidth, y + CARD_HEIGHT, 0x60825D34);

        guiGraphics.drawString(MC.font, getChapterLabel(chapterCard.chapterId), x + 16, y + 14, CARD_TITLE, false);
        guiGraphics.drawString(MC.font, "已收录片段: " + chapterCard.total, x + 16, y + 33, CARD_TEXT, false);
        guiGraphics.drawString(MC.font, "已阅读: " + chapterCard.read + " / " + chapterCard.total, x + 16, y + 46, CARD_TEXT, false);
        guiGraphics.drawString(MC.font, "点击查看本章节内容", x + 16, y + 67, 0xFF86562C, false);
    }

    private void renderStoryCard(GuiGraphics guiGraphics, StoryCard card, boolean hovered) {
        int x = card.x;
        int y = card.y;
        int bgColor = hovered ? CARD_BG_HOVER : ((card.entry.getFragmentId() & 1) == 0 ? CARD_BG_A : CARD_BG_B);

        guiGraphics.fill(RenderType.gui(), x + 3, y + 5, x + cardWidth + 3, y + CARD_HEIGHT + 5, 0x28160D07);
        guiGraphics.fill(RenderType.gui(), x, y, x + cardWidth, y + CARD_HEIGHT, bgColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + cardWidth, y + 1, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x, y + CARD_HEIGHT - 1, x + cardWidth, y + CARD_HEIGHT, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + CARD_HEIGHT, 0x60825D34);
        guiGraphics.fill(RenderType.gui(), x + cardWidth - 1, y, x + cardWidth, y + CARD_HEIGHT, 0x60825D34);

        guiGraphics.fill(RenderType.gui(), x + cardWidth / 2 - 5, y - 4, x + cardWidth / 2 + 5, y + 6, CARD_PIN);
        guiGraphics.fill(RenderType.gui(), x + cardWidth / 2 - 2, y - 1, x + cardWidth / 2 + 2, y + 3, 0x7AFFFFFF);

        guiGraphics.drawString(MC.font, card.entry.getTitle(), x + 10, y + 12, CARD_TITLE, false);
        guiGraphics.drawString(MC.font, "记录者: " + card.entry.getAuthorName(), x + 10, y + 29, CARD_TEXT, false);
        guiGraphics.drawString(MC.font, "时间: " + card.entry.getTime(), x + 10, y + 42, CARD_TEXT, false);
        guiGraphics.drawString(MC.font, "阶段 " + card.entry.getStageId() + " · 片段 " + card.entry.getFragmentId(), x + 10, y + 55, CARD_TEXT, false);
        guiGraphics.drawString(MC.font, card.entry.isRead() ? "已阅读" : "未阅读", x + 10, y + 68, card.entry.isRead() ? 0xFF6C5A43 : 0xFFA5462C, false);
    }

    private void renderNavigation(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int footerY = bookY + bookHeight - 26;
        int leftButtonX = leftPageX + CARD_SIDE_PADDING;
        int rightButtonX = rightPageX + pageWidth - CARD_SIDE_PADDING - NAV_BUTTON_WIDTH;

        boolean prevHovered = isInside(mouseX, mouseY, leftButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT);
        boolean nextHovered = isInside(mouseX, mouseY, rightButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT);
        boolean canPrev = currentPage > 0;
        boolean canNext = currentPage < getCurrentPageCount() - 1;

        renderTextButton(guiGraphics, leftButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT, "< 上页", prevHovered && canPrev, canPrev);
        renderTextButton(guiGraphics, rightButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT, "下页 >", nextHovered && canNext, canNext);

        String pageText = (currentPage + 1) + " / " + getCurrentPageCount();
        guiGraphics.drawCenteredString(MC.font, pageText, this.width / 2, footerY + 5, 0xFF7A5634);

        if (selectedChapterId != null) {
            int backX = leftPageX + CARD_SIDE_PADDING;
            int backY = bookY + 6;
            boolean hovered = isInside(mouseX, mouseY, backX, backY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT);
            renderTextButton(guiGraphics, backX, backY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT, "返回", hovered, true);
        }
    }

    private void renderTextButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String text, boolean hovered, boolean enabled) {
        int textColor = !enabled ? BUTTON_TEXT_DISABLED : hovered ? BUTTON_TEXT_HOVER : BUTTON_TEXT;
        int textY = y + (height - MC.font.lineHeight) / 2;
        guiGraphics.drawCenteredString(MC.font, text, x + width / 2, textY, textColor);

        if (enabled && hovered) {
            int lineWidth = MC.font.width(text);
            int lineX = x + (width - lineWidth) / 2;
            int lineY = textY + MC.font.lineHeight + 1;
            guiGraphics.fill(RenderType.gui(), lineX, lineY, lineX + lineWidth, lineY + 1, 0x887A5634);
        }
    }

    private void renderFooter(GuiGraphics guiGraphics) {
        Component leftHint = selectedChapterId == null
                ? Component.literal("点击章节进入内容")
                : Component.literal("拖动卡片到另一张卡片的位置，可交换顺序");
        Component rightHint = Component.literal("ESC 关闭");
        int footerY = bookY + bookHeight + 10;

        guiGraphics.drawString(MC.font, leftHint, bookX, footerY, 0xFFD6C1A0, false);
        guiGraphics.drawString(MC.font, rightHint, bookX + bookWidth - MC.font.width(rightHint), footerY, 0xFFD6C1A0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectedChapterId != null && isInside(mouseX, mouseY, leftPageX + CARD_SIDE_PADDING, bookY + 6, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT)) {
            selectedChapterId = null;
            currentPage = 0;
            draggingCard = null;
            refreshVisibleState();
            return true;
        }

        if (handlePageButtonClick(mouseX, mouseY)) {
            return true;
        }

        if (selectedChapterId == null) {
            for (ChapterCard chapterCard : chapterCards) {
                if (chapterCard.visible && chapterCard.isMouseOver(mouseX, mouseY, cardWidth)) {
                    selectedChapterId = chapterCard.chapterId;
                    currentPage = 0;
                    refreshVisibleState();
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        for (int i = visibleCards.size() - 1; i >= 0; i--) {
            StoryCard card = visibleCards.get(i);
            if (card.isMouseOver(mouseX, mouseY, cardWidth)) {
                draggingCard = card;
                dragOffsetX = mouseX - card.x;
                dragOffsetY = mouseY - card.y;
                dragStartMouseX = mouseX;
                dragStartMouseY = mouseY;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePageButtonClick(double mouseX, double mouseY) {
        int footerY = bookY + bookHeight - 26;
        int leftButtonX = leftPageX + CARD_SIDE_PADDING;
        int rightButtonX = rightPageX + pageWidth - CARD_SIDE_PADDING - NAV_BUTTON_WIDTH;

        if (isInside(mouseX, mouseY, leftButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT) && currentPage > 0) {
            currentPage--;
            draggingCard = null;
            refreshVisibleState();
            return true;
        }

        if (isInside(mouseX, mouseY, rightButtonX, footerY, NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT) && currentPage < getCurrentPageCount() - 1) {
            currentPage++;
            draggingCard = null;
            refreshVisibleState();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingCard != null) {
            draggingCard.x = (int) Math.round(mouseX - dragOffsetX);
            draggingCard.y = (int) Math.round(mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingCard != null) {
            StoryCard releasedCard = draggingCard;
            draggingCard = null;

            double moved = Math.hypot(mouseX - dragStartMouseX, mouseY - dragStartMouseY);
            if (moved < 6.0D) {
                this.minecraft.setScreen(new Screen_StoryFragment(
                        releasedCard.entry.getFragmentId(),
                        releasedCard.entry.getStageId(),
                        releasedCard.entry.getChapterId(),
                        releasedCard.entry.getTitle(),
                        releasedCard.entry.getContent(),
                        releasedCard.entry.getTime(),
                        releasedCard.entry.getAuthorName()
                ));
                refreshVisibleState();
                return true;
            }

            StoryCard swapTarget = findSwapTarget(releasedCard, mouseX, mouseY);
            if (swapTarget != null && swapTarget != releasedCard) {
                swapCardsInGlobalOrder(releasedCard, swapTarget);
                dirtyOrder = true;
                sendOrderUpdate();
            } else {
                releasedCard.x = releasedCard.targetX;
                releasedCard.y = releasedCard.targetY;
            }

            refreshVisibleState();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private StoryCard findSwapTarget(StoryCard draggedCard, double mouseX, double mouseY) {
        int centerX = draggedCard.x + cardWidth / 2;
        int centerY = draggedCard.y + CARD_HEIGHT / 2;

        for (StoryCard target : visibleCards) {
            if (target == draggedCard) {
                continue;
            }

            boolean mouseInside = target.isMouseOver(mouseX, mouseY, cardWidth);
            boolean centerInside = centerX >= target.targetX && centerX <= target.targetX + cardWidth
                    && centerY >= target.targetY && centerY <= target.targetY + CARD_HEIGHT;
            if (mouseInside || centerInside) {
                return target;
            }
        }

        return null;
    }

    private void swapCardsInGlobalOrder(StoryCard first, StoryCard second) {
        int index1 = allCards.indexOf(first);
        int index2 = allCards.indexOf(second);
        if (index1 < 0 || index2 < 0) {
            return;
        }
        allCards.set(index1, second);
        allCards.set(index2, first);
    }

    private void sendOrderUpdate() {
        if (!dirtyOrder) {
            return;
        }
        List<Integer> orderedIds = new ArrayList<>();
        for (StoryCard card : allCards) {
            orderedIds.add(card.entry.getFragmentId());
        }
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_UpdateStoryBookOrder(orderedIds));
        dirtyOrder = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedChapterId != null) {
                selectedChapterId = null;
                currentPage = 0;
                draggingCard = null;
                refreshVisibleState();
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String getChapterLabel(int chapterId) {
        return chapterId <= 0 ? "序章" : "第 " + chapterId + " 章";
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static class ChapterStats {
        private int total;
        private int read;
    }

    private static class ChapterCard {
        private final int chapterId;
        private final int total;
        private final int read;
        private int x;
        private int y;
        private boolean visible;

        private ChapterCard(int chapterId, int total, int read) {
            this.chapterId = chapterId;
            this.total = total;
            this.read = read;
        }

        private boolean isMouseOver(double mouseX, double mouseY, int width) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        }
    }

    private static class StoryCard {
        private final StoryBookEntryViewData entry;
        private int x;
        private int y;
        private int targetX;
        private int targetY;

        private StoryCard(StoryBookEntryViewData entry) {
            this.entry = entry;
        }

        private boolean isMouseOver(double mouseX, double mouseY, int width) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CARD_HEIGHT;
        }
    }
}
