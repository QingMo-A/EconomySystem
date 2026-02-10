package com.mo.economy_system.core.clue_system;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * 线索查看界面 - 荒野大镖客2风格
 * 黑底白字的手写风格界面，使用虚拟坐标系统
 */
public class Screen_Clue extends Screen {

    // ==================== 虚拟基准尺寸 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;

    // ==================== 常量定义 ====================
    private static final String STAGE_PREFIX = "阶段: ";
    private static final String TIME_PREFIX = "时间: ";
    private static final String AUTHOR_PREFIX = "记录者: ";
    private static final String PRESS_ESC = "按 ESC 退出";

    // ==================== 颜色定义 ====================
    private static final int COLOR_PAPER = 0xFF0A0A0A;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFCCCCCC;
    private static final int COLOR_TEXT_DIM = 0xFF888888;
    private static final int COLOR_ACCENT = 0xFF888888;
    private static final int COLOR_SEPARATOR = 0xFFCC3333;  // 红色分隔线
    private static final int COLOR_BORDER = 0xFF4A4A4A;     // 淡灰色描边

    // ==================== 虚拟坐标系统变量 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 布局参数（虚拟坐标） ====================
    private static final int PAPER_MARGIN = 30;
    private static final int PAPER_BORDER_WIDTH = 1;
    private static final int CONTENT_PADDING = 20;  // 内容到纸张边缘的距离
    private int paperX, paperY, paperWidth, paperHeight;
    private int contentX, contentY, contentWidth, contentHeight;

    // ==================== 排版参数 ====================
    private static final float TITLE_SCALE = 1.6f;      // 标题缩放
    private static final float INFO_SCALE = 0.9f;       // 信息缩放
    private static final int TITLE_BOTTOM_MARGIN = 12;  // 标题下方间距
    private static final int INFO_LINE_SPACING = 10;    // 信息行间距
    private static final int SEPARATOR_MARGIN = 12;     // 分隔线上下间距
    private static final int CONTENT_LINE_SPACING = 0;   // 内容行间距（0表示使用默认行高）
    private static final int CONTENT_PARAGRAPH_SPACING = 6;  // 段落间距

    // ==================== 数据 ====================
    private final String title;
    private final String content;
    private final String time;
    private final String author;
    private final int stage;
    private final String[] contentLines;

    // ==================== 动画状态 ====================
    private long openTime;
    private float fadeProgress = 0f;

    // ==================== 滚动状态 ====================
    private float scrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private static final float SCROLL_SPEED = 10f;

    private static final Minecraft mc = Minecraft.getInstance();

    public Screen_Clue(String title, String content, String time, String author, int stage) {
        super(Component.literal("线索"));
        this.title = title;
        this.content = content;
        this.time = time;
        this.author = author;
        this.stage = stage;
        this.contentLines = content.split("\n");
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        calculateVirtualSize();
        calculateLayout();
        calculateMaxScroll();
    }

    /**
     * 计算虚拟尺寸和缩放比例
     */
    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    /**
     * 计算布局参数（虚拟坐标）
     */
    private void calculateLayout() {
        paperWidth = virtualWidth - PAPER_MARGIN * 2;
        paperHeight = virtualHeight - PAPER_MARGIN * 2;
        paperX = PAPER_MARGIN;
        paperY = PAPER_MARGIN;

        contentX = paperX + CONTENT_PADDING;
        contentY = paperY + CONTENT_PADDING;
        contentWidth = paperWidth - CONTENT_PADDING * 2;
        contentHeight = paperHeight - CONTENT_PADDING * 2;
    }

    /**
     * 计算最大滚动偏移
     */
    private void calculateMaxScroll() {
        // 计算所有内容的总高度（包括标题、信息、正文）
        float lineHeight = mc.font.lineHeight;
        float totalContentHeight = 0f;

        // 标题区域高度
        totalContentHeight += (int) (mc.font.lineHeight * TITLE_SCALE) + TITLE_BOTTOM_MARGIN;

        // 信息区域高度（2行：时间和作者）
        totalContentHeight += (int) (mc.font.lineHeight * INFO_SCALE) * 2 + INFO_LINE_SPACING;

        // 分隔线高度
        totalContentHeight += SEPARATOR_MARGIN;

        // 正文内容高度
        for (String line : contentLines) {
            if (line.trim().isEmpty()) {
                totalContentHeight += CONTENT_PARAGRAPH_SPACING;
            } else {
                // 计算换行后的行数
                var lines = mc.font.getSplitter().splitLines(line, contentWidth, net.minecraft.network.chat.Style.EMPTY);
                int linesNeeded = lines.size();
                totalContentHeight += linesNeeded * (lineHeight + CONTENT_LINE_SPACING) + CONTENT_PARAGRAPH_SPACING;
            }
        }

        // 可见内容区域高度（纸张内容区域高度）
        float visibleContentHeight = contentHeight;

        // 计算最大滚动偏移
        maxScrollOffset = Math.max(0, totalContentHeight - visibleContentHeight);

        // 限制当前滚动偏移
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScrollOffset);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 每帧重新计算虚拟尺寸（支持窗口大小变化）
        calculateVirtualSize();
        calculateLayout();

        updateAnimation();
        float alpha = Mth.clamp(fadeProgress, 0f, 1f);

        // 背景
        renderBackground(guiGraphics, alpha);

        // ==================== 应用全局缩放 ====================
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制所有内容（使用虚拟坐标）
        renderPaperBackground(guiGraphics, alpha);
        renderContent(guiGraphics, alpha);

        // 恢复矩阵状态
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateAnimation() {
        long elapsed = System.currentTimeMillis() - openTime;
        fadeProgress = Math.min(elapsed / 500f, 1f);
    }

    private void renderBackground(GuiGraphics guiGraphics, float alpha) {
        int bgColor = withAlpha(0xFF000000, alpha * 0.7f);
        guiGraphics.fill(0, 0, this.width, this.height, bgColor);
    }

    /**
     * 绘制纸张背景（带描边）
     */
    private void renderPaperBackground(GuiGraphics guiGraphics, float alpha) {
        int paperColor = withAlpha(COLOR_PAPER, alpha);
        int borderColor = withAlpha(COLOR_BORDER, alpha);

        // 绘制纸张背景
        guiGraphics.fill(RenderType.gui(), paperX, paperY, paperX + paperWidth, paperY + paperHeight, paperColor);

        // 绘制淡灰色描边
        guiGraphics.fill(RenderType.gui(), paperX - PAPER_BORDER_WIDTH, paperY - PAPER_BORDER_WIDTH,
                paperX + paperWidth + PAPER_BORDER_WIDTH, paperY, borderColor);  // 上边框
        guiGraphics.fill(RenderType.gui(), paperX - PAPER_BORDER_WIDTH, paperY + paperHeight,
                paperX + paperWidth + PAPER_BORDER_WIDTH, paperY + paperHeight + PAPER_BORDER_WIDTH, borderColor);  // 下边框
        guiGraphics.fill(RenderType.gui(), paperX - PAPER_BORDER_WIDTH, paperY,
                paperX, paperY + paperHeight, borderColor);  // 左边框
        guiGraphics.fill(RenderType.gui(), paperX + paperWidth, paperY,
                paperX + paperWidth + PAPER_BORDER_WIDTH, paperY + paperHeight, borderColor);  // 右边框
    }

    /**
     * 绘制内容（支持滚动）
     */
    private void renderContent(GuiGraphics guiGraphics, float alpha) {
        PoseStack poseStack = guiGraphics.pose();

        // 启用剪裁测试（剪裁纸张区域）
        // 将虚拟坐标转换为屏幕坐标
        int screenX = (int) (paperX * uiScale);
        int screenY = (int) (paperY * uiScale);
        int screenWidth = (int) (paperWidth * uiScale);
        int screenHeight = (int) (paperHeight * uiScale);

        guiGraphics.enableScissor(screenX, screenY, screenX + screenWidth, screenY + screenHeight);

        poseStack.pushPose();

        // 整个内容区域一起滚动（包括标题、信息、正文）
        int currentY = contentY - (int) scrollOffset;

        // 标题（可滚动）
        renderTitle(guiGraphics, contentX, currentY, alpha);
        currentY += (int) (mc.font.lineHeight * TITLE_SCALE) + TITLE_BOTTOM_MARGIN;

        // 信息（可滚动）- 只显示时间和作者，不显示阶段
        renderInfo(guiGraphics, contentX, currentY, alpha);
        currentY += (int) (mc.font.lineHeight * INFO_SCALE) * 2 + INFO_LINE_SPACING;

        // 分隔线（可滚动）
        renderSeparator(guiGraphics, contentX, currentY, contentWidth, alpha);
        currentY += SEPARATOR_MARGIN;

        // 正文内容（可滚动）
        renderClueContent(guiGraphics, contentX, currentY, contentWidth, alpha);

        poseStack.popPose();

        // 禁用剪裁测试
        guiGraphics.disableScissor();

        // 底部提示（右下角，不闪烁）
        renderFooter(guiGraphics, alpha);

        // 滚动指示器（如果有可滚动内容）
        if (maxScrollOffset > 0) {
            renderScrollIndicator(guiGraphics, alpha);
        }
    }

    private void renderTitle(GuiGraphics guiGraphics, int x, int y, float alpha) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(TITLE_SCALE, TITLE_SCALE, 1f);

        int titleColor = withAlpha(COLOR_TEXT_PRIMARY, alpha);
        int scaledX = (int) (x / TITLE_SCALE);
        int scaledY = (int) (y / TITLE_SCALE);

        guiGraphics.drawString(mc.font, title, scaledX, scaledY, titleColor, false);
        poseStack.popPose();
    }

    private void renderInfo(GuiGraphics guiGraphics, int x, int y, float alpha) {
        int infoColor = withAlpha(COLOR_TEXT_SECONDARY, alpha);
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.scale(INFO_SCALE, INFO_SCALE, 1f);

        int scaledX = (int) (x / INFO_SCALE);
        int scaledY = (int) (y / INFO_SCALE);

        // 只显示时间和作者，不显示阶段
        String timeText = TIME_PREFIX + (time != null && !time.isEmpty() ? time : "未知");
        guiGraphics.drawString(mc.font, timeText, scaledX, scaledY, infoColor, false);

        String authorText = AUTHOR_PREFIX + (author != null && !author.isEmpty() ? author : "???");
        guiGraphics.drawString(mc.font, authorText, scaledX, scaledY + INFO_LINE_SPACING, infoColor, false);

        poseStack.popPose();
    }

    private void renderSeparator(GuiGraphics guiGraphics, int x, int y, int width, float alpha) {
        int separatorColor = withAlpha(COLOR_SEPARATOR, alpha);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, separatorColor);
    }

    private void renderClueContent(GuiGraphics guiGraphics, int x, int y, int maxWidth, float alpha) {
        int textColor = withAlpha(COLOR_TEXT_PRIMARY, alpha);
        int currentY = y;
        float lineHeight = mc.font.lineHeight;

        for (String line : contentLines) {
            if (line.trim().isEmpty()) {
                currentY += CONTENT_PARAGRAPH_SPACING;
                continue;
            }

            var wrappedLines = mc.font.getSplitter().splitLines(line, maxWidth, net.minecraft.network.chat.Style.EMPTY);
            for (var wrappedLine : wrappedLines) {
                guiGraphics.drawString(mc.font, wrappedLine.getString(), x, currentY, textColor, false);
                currentY += lineHeight + CONTENT_LINE_SPACING;
            }

            currentY += CONTENT_PARAGRAPH_SPACING;
        }
    }

    private void renderFooter(GuiGraphics guiGraphics, float alpha) {
        String footerText = PRESS_ESC;
        int footerColor = withAlpha(COLOR_TEXT_DIM, alpha);

        // 右下角显示（虚拟坐标）
        int footerX = virtualWidth - mc.font.width(footerText) - 10;
        int footerY = virtualHeight - 10;

        guiGraphics.drawString(mc.font, footerText, footerX, footerY, footerColor, false);
    }

    /**
     * 绘制滚动指示器
     */
    private void renderScrollIndicator(GuiGraphics guiGraphics, float alpha) {
        // 滚动条位置：纸张右侧边缘内侧
        int barWidth = 3;
        int barX = paperX + paperWidth - 8;
        int barY = contentY;
        int barHeight = contentHeight;

        // 滚动条背景
        int bgColor = withAlpha(0xFF333333, alpha * 0.5f);
        guiGraphics.fill(RenderType.gui(), barX, barY, barX + barWidth, barY + barHeight, bgColor);

        // 滚动条滑块
        float scrollPercent = scrollOffset / maxScrollOffset;
        int sliderHeight = Math.max(20, (int) (barHeight * (barHeight / (barHeight + maxScrollOffset))));
        int sliderY = barY + (int) ((barHeight - sliderHeight) * scrollPercent);
        int sliderColor = withAlpha(0xFF666666, alpha);

        guiGraphics.fill(RenderType.gui(), barX, sliderY, barX + barWidth, sliderY + sliderHeight, sliderColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 转换为虚拟坐标判断是否在纸张区域内
        double virtualMouseX = mouseX / uiScale;
        double virtualMouseY = mouseY / uiScale;

        // 只在纸张区域内响应滚轮
        if (virtualMouseX >= paperX && virtualMouseX <= paperX + paperWidth &&
            virtualMouseY >= paperY && virtualMouseY <= paperY + paperHeight) {

            if (maxScrollOffset > 0) {
                float newOffset = scrollOffset - (float) delta * SCROLL_SPEED;
                scrollOffset = Mth.clamp(newOffset, 0, maxScrollOffset);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 只响应ESC键退出
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 点击不退出
        return false;
    }

    private int withAlpha(int color, float alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
