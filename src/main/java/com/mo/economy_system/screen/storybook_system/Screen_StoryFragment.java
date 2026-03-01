package com.mo.economy_system.screen.storybook_system;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class Screen_StoryFragment extends Screen {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_MARGIN = 26;
    private static final int CONTENT_PADDING = 22;
    private static final int TEAR_DEPTH = 10;
    private static final float TITLE_SCALE = 1.62f;
    private static final float META_SCALE = 1.02f;
    private static final float SCROLL_SPEED = 14f;

    private static final int COLOR_OVERLAY = 0xC0150F09;
    private static final int COLOR_PAPER = 0xFFF1E3BE;
    private static final int COLOR_PAPER_SHADOW = 0x6A5A3D1E;
    private static final int COLOR_BORDER = 0xB08E6B3D;
    private static final int COLOR_TITLE = 0xFF6A4321;
    private static final int COLOR_META = 0xFF7B4F29;
    private static final int COLOR_TEXT = 0xFF2A1F14;
    private static final int COLOR_SEPARATOR = 0x907C5A33;
    private static final int COLOR_SCROLL_BG = 0x30FFFFFF;
    private static final int COLOR_SCROLL_BAR = 0xB07A5A33;

    private final int fragmentId;
    private final int stageId;
    private final int chapterId;
    private final String title;
    private final String content;
    private final String time;
    private final String authorName;
    private final String[] paragraphs;

    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private long openTime;
    private float fadeProgress;
    private float scrollOffset;
    private float maxScrollOffset;

    public Screen_StoryFragment(int fragmentId, int stageId, int chapterId, String title, String content, String time, String authorName) {
        super(Component.literal("残页"));
        this.fragmentId = fragmentId;
        this.stageId = stageId;
        this.chapterId = chapterId;
        this.title = title == null ? "未命名残页" : title;
        this.content = content == null ? "" : content;
        this.time = time == null ? "" : time;
        this.authorName = authorName == null ? "未知记录者" : authorName;
        this.paragraphs = this.content.split("\\n");
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        calculateVirtualSize();
        calculateLayout();
        calculateMaxScroll();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = Math.max(1, (int) (this.width / uiScale));
        virtualHeight = Math.max(1, (int) (this.height / uiScale));
    }

    private void calculateLayout() {
        panelX = PANEL_MARGIN;
        panelY = PANEL_MARGIN - 4;
        panelWidth = virtualWidth - PANEL_MARGIN * 2;
        panelHeight = virtualHeight - PANEL_MARGIN * 2 + 8;
        contentX = panelX + CONTENT_PADDING;
        contentY = panelY + CONTENT_PADDING + 4;
        contentWidth = panelWidth - CONTENT_PADDING * 2 - 10;
        contentHeight = panelHeight - CONTENT_PADDING * 2 - 8;
    }

    private void calculateMaxScroll() {
        int totalHeight = 0;
        totalHeight += (int) (MC.font.lineHeight * TITLE_SCALE) + 12;
        totalHeight += (int) (MC.font.lineHeight * META_SCALE) * 4 + 20;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                totalHeight += 7;
                continue;
            }
            int lineCount = MC.font.getSplitter().splitLines(paragraph, contentWidth, Style.EMPTY).size();
            totalHeight += lineCount * (MC.font.lineHeight + 1) + 5;
        }

        maxScrollOffset = Math.max(0, totalHeight - contentHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        calculateVirtualSize();
        calculateLayout();
        updateAnimation();

        guiGraphics.fill(0, 0, this.width, this.height, withAlpha(COLOR_OVERLAY, fadeProgress));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        renderPaperShadow(guiGraphics);
        renderTornPaper(guiGraphics, fadeProgress);
        renderContent(guiGraphics, fadeProgress);

        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateAnimation() {
        long elapsed = System.currentTimeMillis() - openTime;
        fadeProgress = Math.min(1.0f, elapsed / 220f);
    }

    private void renderPaperShadow(GuiGraphics guiGraphics) {
        int shadowOffsetX = 6;
        int shadowOffsetY = 8;
        for (int i = 0; i < panelHeight; i += 2) {
            int insetLeft = getTearInset(i, 0) / 2;
            int insetRight = getTearInset(i, 1) / 2;
            guiGraphics.fill(RenderType.gui(),
                    panelX + shadowOffsetX + insetLeft,
                    panelY + shadowOffsetY + i,
                    panelX + shadowOffsetX + panelWidth - insetRight,
                    panelY + shadowOffsetY + Math.min(panelHeight, i + 2),
                    withAlpha(COLOR_PAPER_SHADOW, fadeProgress * 0.85f));
        }
    }

    private void renderTornPaper(GuiGraphics guiGraphics, float alpha) {
        int paperColor = withAlpha(COLOR_PAPER, alpha);
        int borderColor = withAlpha(COLOR_BORDER, alpha);

        for (int i = 0; i < panelHeight; i += 2) {
            int leftInset = getTearInset(i, 0);
            int rightInset = getTearInset(i, 1);
            int topY = panelY + i;
            int bottomY = panelY + Math.min(panelHeight, i + 2);
            int left = panelX + leftInset;
            int right = panelX + panelWidth - rightInset;

            guiGraphics.fill(RenderType.gui(), left, topY, right, bottomY, paperColor);

            if (i < panelHeight - 2) {
                guiGraphics.fill(RenderType.gui(), left, topY, Math.min(left + 1, right), bottomY, borderColor);
                guiGraphics.fill(RenderType.gui(), Math.max(right - 1, left), topY, right, bottomY, borderColor);
            }
        }

        guiGraphics.fill(RenderType.gui(), panelX + 18, panelY + 22, panelX + panelWidth - 42, panelY + 30, 0x0FFFFFFF);
        guiGraphics.fill(RenderType.gui(), panelX + 26, panelY + panelHeight - 38, panelX + panelWidth - 18, panelY + panelHeight - 26, 0x10A17D52);
    }

    private void renderContent(GuiGraphics guiGraphics, float alpha) {
        int scissorX1 = (int) ((panelX + 8) * uiScale);
        int scissorY1 = (int) ((panelY + 6) * uiScale);
        int scissorX2 = (int) ((panelX + panelWidth - 8) * uiScale);
        int scissorY2 = (int) ((panelY + panelHeight - 6) * uiScale);
        guiGraphics.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);

        int currentY = contentY - (int) scrollOffset;
        renderTitle(guiGraphics, currentY, alpha);
        currentY += (int) (MC.font.lineHeight * TITLE_SCALE) + 10;
        renderMeta(guiGraphics, currentY, alpha);
        currentY += (int) (MC.font.lineHeight * META_SCALE) * 4 + 12;
        guiGraphics.fill(RenderType.gui(), contentX, currentY, contentX + contentWidth, currentY + 1, withAlpha(COLOR_SEPARATOR, alpha));
        currentY += 9;
        renderBody(guiGraphics, currentY, alpha);

        guiGraphics.disableScissor();
        renderFooter(guiGraphics, alpha);
        if (maxScrollOffset > 0) {
            renderScrollBar(guiGraphics, alpha);
        }
    }

    private void renderTitle(GuiGraphics guiGraphics, int y, float alpha) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TITLE_SCALE, TITLE_SCALE, 1f);
        guiGraphics.drawString(
                MC.font,
                title,
                (int) (contentX / TITLE_SCALE),
                (int) (y / TITLE_SCALE),
                withAlpha(COLOR_TITLE, alpha),
                false
        );
        guiGraphics.pose().popPose();
    }

    private void renderMeta(GuiGraphics guiGraphics, int y, float alpha) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(META_SCALE, META_SCALE, 1f);

        int drawX = (int) (contentX / META_SCALE);
        int drawY = (int) (y / META_SCALE);
        int lineGap = 10;

        guiGraphics.drawString(MC.font, "时间: " + (time.isEmpty() ? "未知" : time), drawX, drawY, withAlpha(COLOR_META, alpha), false);
        guiGraphics.drawString(MC.font, "记录者: " + authorName, drawX, drawY + lineGap, withAlpha(COLOR_META, alpha), false);
        guiGraphics.drawString(MC.font, "阶段: " + stageId, drawX, drawY + lineGap * 2, withAlpha(COLOR_META, alpha), false);
        guiGraphics.drawString(MC.font, "章节: " + chapterId + "   片段ID: " + fragmentId, drawX, drawY + lineGap * 3, withAlpha(COLOR_META, alpha), false);

        guiGraphics.pose().popPose();
    }

    private void renderBody(GuiGraphics guiGraphics, int y, float alpha) {
        int currentY = y;
        int textColor = withAlpha(COLOR_TEXT, alpha);
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                currentY += 7;
                continue;
            }

            var wrappedLines = MC.font.getSplitter().splitLines(paragraph, contentWidth, Style.EMPTY);
            for (var line : wrappedLines) {
                guiGraphics.drawString(MC.font, line.getString(), contentX, currentY, textColor, false);
                currentY += MC.font.lineHeight + 1;
            }
            currentY += 5;
        }
    }

    private void renderFooter(GuiGraphics guiGraphics, float alpha) {
        String text = "滚轮翻页  ·  ESC 关闭";
        guiGraphics.drawString(
                MC.font,
                text,
                panelX + panelWidth - MC.font.width(text) - 22,
                panelY + panelHeight - 20,
                withAlpha(COLOR_META, alpha),
                false
        );
    }

    private void renderScrollBar(GuiGraphics guiGraphics, float alpha) {
        int barX = panelX + panelWidth - 15;
        int barY = contentY + 2;
        int barHeight = contentHeight - 8;
        guiGraphics.fill(RenderType.gui(), barX, barY, barX + 3, barY + barHeight, withAlpha(COLOR_SCROLL_BG, alpha));

        float scrollPercent = scrollOffset / maxScrollOffset;
        int sliderHeight = Math.max(18, (int) (barHeight * (barHeight / (barHeight + maxScrollOffset))));
        int sliderY = barY + (int) ((barHeight - sliderHeight) * scrollPercent);
        guiGraphics.fill(RenderType.gui(), barX, sliderY, barX + 3, sliderY + sliderHeight, withAlpha(COLOR_SCROLL_BAR, alpha));
    }

    private int getTearInset(int row, int side) {
        double base = Math.sin((row + side * 23) * 0.14) * 4.2;
        double detail = Math.cos((row + side * 41) * 0.37) * 2.4;
        int inset = (int) Math.round(Math.abs(base + detail)) + (row % 24 == 0 ? TEAR_DEPTH / 2 : 0);
        return Mth.clamp(inset, 2, TEAR_DEPTH);
    }

    private int withAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScrollOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        double virtualMouseX = mouseX / uiScale;
        double virtualMouseY = mouseY / uiScale;
        if (virtualMouseX >= panelX && virtualMouseX <= panelX + panelWidth
                && virtualMouseY >= panelY && virtualMouseY <= panelY + panelHeight) {
            scrollOffset = Mth.clamp(scrollOffset - (float) delta * SCROLL_SPEED, 0, maxScrollOffset);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
