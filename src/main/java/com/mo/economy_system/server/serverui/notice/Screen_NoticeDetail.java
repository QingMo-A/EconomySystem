package com.mo.economy_system.server.serverui.notice;

import com.mo.economy_system.server.notice.NoticeData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 公告详情弹窗
 * 点击公告卡片后打开的子屏幕
 * 风格参考 ConnectScreenMixin
 */
public class Screen_NoticeDetail extends Screen {

    // ==================== 颜色定义（青色调，参考 ConnectScreenMixin 风格） ====================
    private static final int BG_OUTER = 0xDD001A1A;      // 外层背景（深红）
    private static final int BG_INNER = 0xEE051A1A;      // 内层背景
    private static final int BORDER_DARK = 0xFF003D3D;   // 深色边框
    private static final int BORDER_GLOW = 0xFF1AFFFF;   // 发光边框（青色）
    private static final int ACCENT_CYAN = 0xFF00CCCC;   // 强调色（青色）

    private static final int TITLE_COLOR = 0xFFFFFFFF;   // 标题颜色
    private static final int CONTENT_COLOR = 0xFFDDDDDD; // 内容文字颜色
    private static final int TIME_COLOR = 0xFF888888;    // 时间颜色

    // 按钮颜色
    private static final int BTN_TOP_NORMAL = 0xCC004444;
    private static final int BTN_BOTTOM_NORMAL = 0xCC002222;
    private static final int BTN_BORDER_NORMAL = 0xCC008888;
    private static final int BTN_TOP_HOVER = 0xCC006666;
    private static final int BTN_BOTTOM_HOVER = 0xCC003333;
    private static final int BTN_BORDER_HOVER = 0xFF00CCCC;

    private static final int PADDING = 12;

    // 公告数据
    private final NoticeData notice;

    // 打开前的页面索引（用于关闭后返回原页面）
    private final int previousPageIndex;

    // 打开时间戳（用于防止点击事件传播导致立刻关闭）
    private final long openTime;

    // 弹窗尺寸（像素）
    private int boxWidth = 420;
    private int boxHeight = 280;
    private int boxX, boxY;

    // 关闭按钮区域
    private int closeButtonX, closeButtonY, closeButtonWidth, closeButtonHeight;

    // 点击冷却时间（毫秒）
    private static final long CLICK_COOLDOWN = 300;

    // 标记：初始化是否完成
    private boolean initialized = false;

    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    public Screen_NoticeDetail(NoticeData notice, int previousPageIndex) {
        super(Component.literal("NoticeDetail"));
        this.notice = notice;
        this.previousPageIndex = previousPageIndex;
        this.openTime = System.currentTimeMillis();
    }

    public Screen_NoticeDetail(NoticeData notice) {
        this(notice, 0);
    }

    @Override
    protected void init() {
        super.init();
        initialized = true;

        // 计算弹窗位置（居中）
        boxX = (this.width - boxWidth) / 2;
        boxY = (this.height - boxHeight) / 2;

        // 设置关闭按钮区域
        closeButtonWidth = boxWidth - 2 * PADDING;
        closeButtonHeight = 24;
        closeButtonX = boxX + PADDING;
        closeButtonY = boxY + boxHeight - PADDING - closeButtonHeight;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        try {
            // 1. 背景填充（屏幕坐标）
            guiGraphics.fillGradient(0, 0, this.width, this.height, BG_OUTER, BG_OUTER);

            // 2. 主面板（双层边框 + 圆角效果）
            // 外层深色边框
            renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
            // 内层发光边框
            renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
            // 主背景
            guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

            var poseStack = guiGraphics.pose();

            // 3. 公告图标（左上角，放大）
            poseStack.pushPose();
            poseStack.scale(3.0f, 3.0f, 1.0f);
            String icon = "📢";
            int iconX = (int) ((boxX + 25) / 3.0f);
            int iconY = (int) ((boxY + 20) / 3.0f);
            guiGraphics.drawString(this.font, icon, iconX, iconY, ACCENT_CYAN, false);
            poseStack.popPose();

            // 4. 标题（右侧，放大）
            poseStack.pushPose();
            poseStack.scale(1.5f, 1.5f, 1.0f);
            String title = notice.getNoticeTitle();
            String titleText = "§l§f" + (title != null ? title : "无标题");
            float scale = 1.5f;
            int titleX = (int) ((boxX + 90) / scale);
            int titleY = (int) ((boxY + 28) / scale);
            guiGraphics.drawString(this.font, titleText, titleX, titleY, TITLE_COLOR, false);
            poseStack.popPose();

            // 5. 右上角服务器名
            String domainText = "§b§lDreaming§d§lFish";
            int domainX = boxX + boxWidth - PADDING - this.font.width(domainText);
            int domainY = boxY + PADDING;
            guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFFFF, false);

            // 6. 双层分隔线
            int lineY = boxY + 58;
            guiGraphics.fill(boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 2, ACCENT_CYAN);
            guiGraphics.fill(boxX + PADDING, lineY + 3, boxX + boxWidth - PADDING, lineY + 4, 0xAA004444);

            // 7. 发布时间
            String dateTime = formatDateTime(notice.getPublishTime());
            String timeText = "§7发布时间: §f" + dateTime;
            int timeWidth = this.font.width(timeText);
            int centerX = boxX + boxWidth / 2;
            guiGraphics.drawString(this.font, timeText, centerX - timeWidth / 2, boxY + 68, TIME_COLOR);

            // 8. 内容区域
            int contentY = boxY + 90;
            int contentWidth = boxWidth - 2 * PADDING;
            int contentX = boxX + PADDING;
            String content = notice.getNoticeContent();
            String safeContent = (content != null) ? content : "暂无内容";
            renderWrappedText(guiGraphics, safeContent, contentX, contentY, contentWidth);

            // 9. 绘制关闭按钮
            renderCustomButton(guiGraphics, mouseX, mouseY, closeButtonX, closeButtonY, closeButtonWidth, closeButtonHeight, "§b关闭");

        } catch (Exception e) {
            e.printStackTrace();
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§c公告渲染出错: " + e.getMessage()), false
                );
            }
        }
    }

    /**
     * 渲染圆角盒子（参考 ConnectScreenMixin）
     */
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(RenderType.gui(), x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(RenderType.gui(), x1, y1 + 1, x2, y2 - 1, color);
    }

    /**
     * 渲染自定义按钮（参考 ConnectScreenMixin.CustomButton）
     */
    private void renderCustomButton(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, String text) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int topColor, bottomColor, borderColor;
        if (hovered) {
            topColor = BTN_TOP_HOVER;
            bottomColor = BTN_BOTTOM_HOVER;
            borderColor = BTN_BORDER_HOVER;
        } else {
            topColor = BTN_TOP_NORMAL;
            bottomColor = BTN_BOTTOM_NORMAL;
            borderColor = BTN_BORDER_NORMAL;
        }

        // 渐变背景
        guiGraphics.fill(RenderType.gui(), x + 2, y, x + width - 2, y + height, topColor);
        guiGraphics.fill(RenderType.gui(), x + 2, y + height, x + width - 2, y + height + 1, bottomColor);

        // 边框
        guiGraphics.fill(RenderType.gui(), x + 1, y, x + 2, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 2, y, x + width - 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 2, y, x + width - 2, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 2, y + height - 1, x + width - 2, y + height, borderColor);

        // 角落装饰
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y + height - 1, x + width, y + height, borderColor);

        // 文字
        String displayText = text;
        int textX = x + width / 2 - this.font.width(displayText) / 2;
        int textY = y + (height - 8) / 2;
        guiGraphics.drawString(this.font, displayText, textX, textY, 0xFFFFFF);
    }

    /**
     * 渲染自动换行的文本
     */
    private void renderWrappedText(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth) {
        String[] lines = text.split("\n");
        int currentY = y;

        for (String line : lines) {
            String remaining = line;
            while (!remaining.isEmpty() && this.font.width(remaining) > maxWidth) {
                int breakPoint = findBreakPoint(remaining, maxWidth);
                guiGraphics.drawString(this.font, remaining.substring(0, breakPoint), x, currentY, CONTENT_COLOR);
                remaining = remaining.substring(breakPoint);
                currentY += this.font.lineHeight + 4;
            }
            if (!remaining.isEmpty()) {
                guiGraphics.drawString(this.font, remaining, x, currentY, CONTENT_COLOR);
                currentY += this.font.lineHeight + 4;
            }
        }
    }

    private int findBreakPoint(String text, int maxWidth) {
        int end = text.length();
        for (int i = end; i > 0; i--) {
            if (this.font.width(text.substring(0, i)) <= maxWidth) {
                return i;
            }
        }
        return 1;
    }

    private String formatDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZONE)
                .format(DATE_FORMAT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI.onSubScreenClosed();
        // 设置返回标记，跳过打开动画
        com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI.setReturningFromSubScreen(true);
        Minecraft mc = Minecraft.getInstance();
        com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI_Screen screen =
            new com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI_Screen();
        screen.setSelectedPageIndex(previousPageIndex);
        mc.setScreen(screen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        long timeSinceOpen = System.currentTimeMillis() - openTime;
        if (timeSinceOpen < CLICK_COOLDOWN || !initialized) {
            return true;
        }

        // 检查是否点击关闭按钮
        if (mouseX >= closeButtonX && mouseX <= closeButtonX + closeButtonWidth &&
            mouseY >= closeButtonY && mouseY <= closeButtonY + closeButtonHeight) {
            onClose();
            return true;
        }

        // 点击弹窗外区域关闭
        if (mouseX < boxX || mouseX > boxX + boxWidth ||
            mouseY < boxY || mouseY > boxY + boxHeight) {
            onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
