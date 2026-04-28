package com.mo.economy_system.screen.server_screen.serverscreen;

import com.mo.economy_system.server.notice.NoticeData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderType;

import java.util.List;
import java.util.ArrayList;

/**
 * ServerScreenUI 渲染工具类
 *
 * 包含所有通用的渲染工具方法，包括：
 * - 基础图形绘制（圆角矩形、进度条、进度环）
 * - 卡片组件（游戏卡片、按钮、任务卡片）
 * - 页面渲染（公告列表、服务器信息等）
 *
 * 所有方法都是静态的，可以在任何需要渲染的地方调用
 */
public class ServerScreenUI_RendererUtils {

    // ==================== 颜色常量 ====================
    public static final int CARD_BG = 0x18FFFFFF;
    public static final int CARD_BORDER = 0x30FFFFFF;
    public static final int CARD_TEXT_DESC = 0x80FFFFFF;
    public static final int CARD_TEXT_VALUE = 0xFFFFFFFF;

    // 卡片主题色
    public static final int CARD_RANK_GOLD = 0xFFFFD700;
    public static final int CARD_TITLE_PURPLE = 0xFF9B59B6;
    public static final int CARD_GOLD_ORANGE = 0xFFFF8C00;
    public static final int CARD_TERRITORY_GREEN = 0xFF27AE60;
    public static final int CARD_BIOME_CYAN = 0xFF1ABC9C;
    public static final int CARD_BLUEPRINT_BLUE = 0xFF3498DB;

    // 进度条颜色
    public static final int BAR_HEALTH_COLOR = 0xFFFF8888;
    public static final int BAR_FOOD_COLOR = 0xFFFFCC00;
    public static final int BAR_STRENGTH_COLOR = 0xFF00DD00;
    public static final int BAR_COURAGE_COLOR = 0xFFCC00FF;
    public static final int BAR_INFECTION_COLOR = 0xFF00DD00;

    // 按钮颜色
    public static final int BTN_BG = 0x25FFFFFF;
    public static final int BTN_BG_HOVER = 0x35FFFFFF;
    public static final int BTN_BG_SELECTED = 0x45FFFFFF;
    public static final int BTN_TEXT = 0xB0FFFFFF;
    public static final int BTN_TEXT_SELECTED = 0xFFFFFFFF;
    public static final int BTN_ACCENT = 0xFF4FC3F7;

    // 信息区颜色
    public static final int INFO_BG = 0x40FFFFFF;
    public static final int INFO_TEXT = 0x90FFFFFF;
    public static final int INFO_ACCENT = 0xFF4FC3F7;

    // ==================== 基础图形绘制 ====================

    /**
     * 绘制圆角矩形
     */
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // 绘制填充
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + height, fillColor);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + width, y + height - radius, fillColor);
        // 四个圆角
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + radius, y + radius + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y + radius, x + width, y + radius + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - radius, x + width - radius, y + height, fillColor);

        // 绘制边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - borderWidth, x + width - radius, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + borderWidth, y + height - radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y + radius, x + width, y + height - radius, borderColor);
        // 四个圆角边框
        guiGraphics.fill(RenderType.gui(), x, y, x + radius, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y, x + width, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + radius, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - radius, x + borderWidth, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y + height - borderWidth, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y + height - radius, x + width, y + height, borderColor);
    }

    /**
     * 绘制带直角边框的矩形
     */
    public static void drawRoundedRectOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // 绘制填充
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, fillColor);
        // 绘制直角边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + height, borderColor);
    }

    /**
     * 绘制进度环（带进度弧）
     */
    public static void drawProgressCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float progress) {
        int segments = 256;
        int borderColor = 0xFFFFFFFF;
        int progressColor = 0xFFFFAA00;

        // 绘制进度弧
        if (progress > 0) {
            float startAngle = (float) (-Math.PI / 2);
            float endAngle = startAngle + (float) (2 * Math.PI * progress);
            int arcRadius = radius - 3;

            int segmentsToDraw = (int) (segments * progress);
            if (segmentsToDraw < 1) segmentsToDraw = 1;

            int lastX = centerX + (int) (arcRadius * Math.cos(startAngle));
            int lastY = centerY + (int) (arcRadius * Math.sin(startAngle));

            for (int i = 1; i <= segmentsToDraw; i++) {
                float angle = startAngle + (endAngle - startAngle) * i / segmentsToDraw;
                int x = centerX + (int) Math.round(arcRadius * Math.cos(angle));
                int y = centerY + (int) Math.round(arcRadius * Math.sin(angle));

                guiGraphics.hLine(lastX, x, lastY, progressColor);
                guiGraphics.vLine(x, lastY, y, progressColor);

                lastX = x;
                lastY = y;
            }
        }

        // 绘制白色外圈边框
        int lastX = centerX + (int) (radius * Math.cos(0));
        int lastY = centerY + (int) (radius * Math.sin(0));

        for (int i = 1; i <= segments; i++) {
            float angle = i * (float) (2 * Math.PI / segments);
            int x = centerX + (int) Math.round(radius * Math.cos(angle));
            int y = centerY + (int) Math.round(radius * Math.sin(angle));

            guiGraphics.hLine(lastX, x, lastY, borderColor);
            guiGraphics.vLine(x, lastY, y, borderColor);

            lastX = x;
            lastY = y;
        }
    }

    /**
     * 绘制横向进度条（带发光效果）
     */
    public static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float pct, int color) {
        // 外发光效果
        int glowColor = 0x40000000 | (color & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 背景
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, 0x80FFFFFF);

        // 绘制前景（进度）
        int progressWidth = (int) (width * Math.max(0, Math.min(1, pct)));
        if (progressWidth > 2) {
            int deepColor = 0xFF000000 | (color & 0x00FFFFFF);
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + height - 1, deepColor);
            // 顶部高光
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + Math.min(3, height / 2), 0x60FFFFFF);
        }

        // 白色边框
        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);
    }

    /**
     * 绘制圆角进度条
     */
    public static void drawRoundedProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float pct, int color) {
        int radius = Math.min(height / 2, 4);
        int bgColor = 0x60FFFFFF;
        drawRoundedRect(guiGraphics, x, y, width, height, radius, bgColor, bgColor);

        int progressWidth = (int) (width * Math.max(0, Math.min(1, pct)));
        if (progressWidth > radius * 2) {
            int progressColor = 0xFF000000 | (color & 0x00FFFFFF);
            drawRoundedRect(guiGraphics, x, y, progressWidth, height, radius, progressColor, progressColor);
        }
    }

    /**
     * 渲染圆角盒子
     */
    public static void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(RenderType.gui(), x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(RenderType.gui(), x1, y1 + 1, x2, y2 - 1, color);
    }

    /**
     * 绘制双边框盒子
     */
    public static void drawDoubleBorderBox(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, CARD_BG);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, CARD_BORDER);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, CARD_BORDER);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, CARD_BORDER);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, CARD_BORDER);
    }

    /**
     * 绘制渐变梦幻色框
     */
    public static void drawGradientBox(GuiGraphics guiGraphics, int x, int y, int width, int height, int gradientType) {
        // 逐像素绘制渐变
        for (int i = 0; i < width; i++) {
            float ratio = (float) i / width;
            int color = getGradientColor(gradientType, ratio);
            guiGraphics.fill(RenderType.gui(), x + i, y, x + i + 1, y + height, color);
        }

        // 白色边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + borderWidth, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + width, y + height, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + height, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + height, 0xFFFFFFFF);
    }

    /**
     * 获取渐变色
     */
    public static int getGradientColor(int type, float ratio) {
        int r, g, b;

        switch (type) {
            case 0: // 粉紫蓝渐变
                if (ratio < 0.5f) {
                    float t = ratio * 2;
                    r = (int) (255 + (186 - 255) * t);
                    g = (int) (182 + (85 - 182) * t);
                    b = (int) (193 + (211 - 193) * t);
                } else {
                    float t = (ratio - 0.5f) * 2;
                    r = (int) (186 + (138 - 186) * t);
                    g = (int) (85 + (43 - 85) * t);
                    b = (int) (211 + (226 - 211) * t);
                }
                break;
            case 1: // 粉紫渐变
                r = (int) (255 + (186 - 255) * ratio);
                g = (int) (182 + (85 - 182) * ratio);
                b = (int) (193 + (211 - 193) * ratio);
                break;
            case 2: // 金橙渐变
                r = 255;
                g = (int) (215 + (140 - 215) * ratio);
                b = 0;
                break;
            case 3: // 橙红渐变
                r = 255;
                g = (int) (140 + (69 - 140) * ratio);
                b = 0;
                break;
            default:
                r = g = b = 255;
        }

        return 0x80000000 | (r << 16) | (g << 8) | b;
    }

    // ==================== 卡片组件 ====================

    /**
     * 绘制游戏化卡片背景
     */
    public static void drawGameCard(GuiGraphics guiGraphics, int x, int y, int width, int height, int themeColor, boolean isHovered) {
        // 背景
        int bgAlpha = isHovered ? 0x25 : 0x18;
        int bgColor = (bgAlpha << 24) | 0xFFFFFF;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

        // 边框
        int borderAlpha = isHovered ? 0x50 : 0x30;
        int borderColor = (borderAlpha << 24) | 0xFFFFFF;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);

        // 左侧主题色装饰条
        int stripAlpha = isHovered ? 0xFF : 0xCC;
        int stripColor = (stripAlpha << 24) | (themeColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y, x + 3, y + height, stripColor);

        // 顶部渐变效果（悬停时）
        if (isHovered) {
            for (int i = 0; i < 5; i++) {
                int alpha = (5 - i) * 15;
                int gradientColor = (alpha << 24) | (themeColor & 0x00FFFFFF);
                guiGraphics.fill(RenderType.gui(), x + 3, y + i, x + width, y + i + 1, gradientColor);
            }
        }
    }

    /**
     * 绘制极简按钮 + 动态箭头 + 未读提示
     */
    public static void drawCleanButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                       boolean isSelected, boolean isHovered, String icon, String name,
                                       boolean hasUnread, long arrowAnimTime) {
        // 背景
        int bgColor;
        if (isSelected) {
            bgColor = 0x604FC3F7;
        } else if (isHovered) {
            bgColor = BTN_BG_HOVER;
        } else {
            bgColor = BTN_BG;
        }
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

        // 内容位置
        int textX = x + 6;
        int textY = y + (height - font.lineHeight) / 2;

        // 图标
        int iconWidth = font.width(icon);
        int iconColor = isSelected ? BTN_ACCENT : (isHovered ? 0xFFFFFFFF : BTN_TEXT);
        guiGraphics.drawString(font, icon, textX, textY, iconColor);

        // 名称
        int nameColor = isSelected ? 0xFFFFFFFF : BTN_TEXT;
        guiGraphics.drawString(font, name, textX + iconWidth + 4, textY, nameColor);

        // 动态箭头动画
        if (isHovered && !isSelected) {
            float animSpeed = 0.004f;
            float animRange = 5f;
            float offset = (float) Math.sin(arrowAnimTime * animSpeed) * animRange;

            String arrow = "◀";
            int arrowWidth = font.width(arrow);
            float arrowX = x + width - arrowWidth - 5 + offset;
            int arrowY = y + (height - font.lineHeight) / 2;

            float alpha = 0.6f + 0.4f * (float) Math.cos(arrowAnimTime * animSpeed);
            int arrowColor = ((int) (alpha * 255) << 24) | (BTN_ACCENT & 0x00FFFFFF);
            guiGraphics.drawString(font, arrow, (int) arrowX, arrowY, arrowColor);
        } else if (isSelected) {
            String arrow = "◀";
            int arrowWidth = font.width(arrow);
            int arrowX = x + width - arrowWidth - 5;
            int arrowY = y + (height - font.lineHeight) / 2;
            guiGraphics.drawString(font, arrow, arrowX, arrowY, 0xFFFFFFFF);
        }

        // 未读闪烁感叹号
        if (hasUnread) {
            long blinkTime = System.currentTimeMillis() % 1000;
            float blinkAlpha = 0.4f + 0.6f * (float) Math.sin(blinkTime * Math.PI * 2 / 1000);

            int badgeSize = 8;
            int badgeX = x + width - badgeSize - 2;
            int badgeY = y + 2;

            int badgeColor = ((int) (blinkAlpha * 255) << 24) | 0xFF4444;
            guiGraphics.fill(RenderType.gui(), badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeColor);

            String exclamation = "!";
            int exclamationWidth = font.width(exclamation);
            int exclamationX = badgeX + (badgeSize - exclamationWidth) / 2;
            int exclamationY = badgeY - 1;
            guiGraphics.drawString(font, exclamation, exclamationX, exclamationY, 0xFFFFFFFF);
        }
    }

    /**
     * 绘制任务分类标签
     */
    public static void drawTaskTab(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                   String text, boolean isSelected, int themeColor) {
        int bgColor = isSelected ? 0x60FFFFFF : 0x20FFFFFF;
        int borderColor = isSelected ? themeColor : 0x40FFFFFF;

        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);
        // 左侧主题色条
        guiGraphics.fill(RenderType.gui(), x, y, x + 3, y + height, isSelected ? themeColor : 0x40FFFFFF);

        // 文字
        int textWidth = font.width(text);
        int textColor = isSelected ? 0xFFFFFFFF : 0xAAAAAAAA;
        guiGraphics.drawString(font, text, x + (width - textWidth) / 2, y + (height - font.lineHeight) / 2, textColor);
    }

    /**
     * 绘制信息行（图标 + 文字）
     */
    public static void drawInfoLine(GuiGraphics guiGraphics, Font font, int x, int y, int width, String icon, String text, int textColor, int accentColor) {
        int iconWidth = font.width(icon);
        guiGraphics.drawString(font, icon, x, y, accentColor);
        guiGraphics.drawString(font, text, x + iconWidth + 4, y, textColor);
        // 右侧装饰点
        guiGraphics.fill(RenderType.gui(), x + width - 6, y + font.lineHeight / 2 - 1,
            x + width - 2, y + font.lineHeight / 2 + 2, accentColor);
    }

    /**
     * 绘制服务器信息区域
     */
    public static void drawServerInfoArea(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, int animOffsetY, int onlinePlayers, int maxPlayers, float tps) {
        int actualY = y + animOffsetY;

        // 卡片背景
        int cardBg = INFO_BG;
        int cardBorder = 0x50FFFFFF;
        guiGraphics.fill(RenderType.gui(), x, actualY, x + width, actualY + height, cardBg);
        guiGraphics.fill(RenderType.gui(), x, actualY, x + width, actualY + 1, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, actualY, x + 1, actualY + height, cardBorder);
        guiGraphics.fill(RenderType.gui(), x + width - 1, actualY, x + width, actualY + height, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, actualY + height - 1, x + width, actualY + height, cardBorder);

        // 内边距
        int padding = 8;
        int contentX = x + padding;
        int contentWidth = width - padding * 2;

        // 格式化日期时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String dateStr = String.format("%02d/%02d/%02d", now.getYear() % 100, now.getMonthValue(), now.getDayOfMonth());
        String timeStr = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
        String dateTimeStr = dateStr + " " + timeStr;

        // 日期时间
        int dateTimeY = actualY + padding + 4;
        int dateTimeWidth = font.width(dateTimeStr);
        int dateTimeX = x + (width - dateTimeWidth) / 2;

        guiGraphics.drawString(font, dateTimeStr, dateTimeX + 1, dateTimeY + 1, 0x40000000);
        guiGraphics.drawString(font, dateTimeStr, dateTimeX, dateTimeY, 0xFFFFFFFF);

        // 分隔线
        int lineY = dateTimeY + font.lineHeight + 6;
        guiGraphics.fill(RenderType.gui(), contentX, lineY, contentX + contentWidth, lineY + 1, 0x30FFFFFF);

        // 在线人数 + TPS
        int infoY = lineY + 6;
        int infoSpacing = (contentWidth - 20) / 2;

        String onlineText = onlinePlayers + "/" + maxPlayers;
        int onlineTextWidth = font.width(onlineText);
        int onlineIconX = contentX + (infoSpacing - font.width("👥") - onlineTextWidth) / 2;
        guiGraphics.drawString(font, "👥", onlineIconX, infoY, 0xFF4FC3F7);
        guiGraphics.drawString(font, onlineText, onlineIconX + font.width("👥") + 3, infoY, 0xD0FFFFFF);

        String tpsText = String.format("%.1f", tps);
        int tpsTextWidth = font.width(tpsText);
        int tpsIconX = contentX + contentWidth - infoSpacing + (infoSpacing - font.width("⚡") - tpsTextWidth) / 2;
        guiGraphics.drawString(font, "⚡", tpsIconX, infoY, 0xFFFFA726);
        guiGraphics.drawString(font, tpsText, tpsIconX + font.width("⚡") + 3, infoY, 0xD0FFFFFF);
    }

    // ==================== 任务卡片组件 ====================

    /**
     * 渲染单个任务卡片
     */
    public static void renderTaskCard(GuiGraphics guiGraphics, Font font, int x, int y, int width,
                                      String taskName, String taskContent, int themeColor,
                                      boolean isFinished, boolean isServerTask, boolean isHovered) {
        int innerMargin = 8;
        int cardHeight = 60;

        // 绘制游戏化卡片背景
        drawGameCard(guiGraphics, x, y, width, cardHeight, themeColor, isHovered || isFinished);

        // 左侧：任务图标 + 名称
        String taskIcon = isServerTask ? "📋" : "📜";
        String fullTitle = taskIcon + " " + taskName;
        guiGraphics.drawString(font, fullTitle, x + innerMargin + 4, y + innerMargin,
            isFinished ? 0xFF888888 : themeColor);

        // 内容描述（截断）
        int maxWidth = width - innerMargin * 2 - 40;
        String displayContent = taskContent;
        if (font.width(displayContent) > maxWidth) {
            displayContent = truncateText(font, displayContent, maxWidth) + "...";
        }
        guiGraphics.drawString(font, displayContent, x + innerMargin + 4,
            y + innerMargin + font.lineHeight + 2, 0xFFAAAAAA);

        // 右侧：完成状态按钮
        int btnSize = 20;
        int btnX = x + width - innerMargin - btnSize;
        int btnY = y + (cardHeight - btnSize) / 2;

        if (isFinished) {
            guiGraphics.drawString(font, "✓", btnX, btnY, 0xFF00FF00);
        } else {
            int btnColor = 0x40FFFFFF;
            guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + btnSize, btnY + btnSize, btnColor);
            guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + btnSize, btnY + 1, 0x60FFFFFF);
            guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + 1, btnY + btnSize, 0x60FFFFFF);
        }
    }

    // ==================== 公告卡片组件 ====================

    /**
     * 渲染单个公告卡片
     */
    public static void renderNoticeCard(GuiGraphics guiGraphics, Font font, int x, int y, int width,
                                        NoticeData notice, boolean isRead) {
        int innerMargin = 6;
        int cardHeight = 52;

        // 卡片背景
        int cardBg = isRead ? 0x20FFFFFF : 0x30FFFFFF;
        int cardBorder = isRead ? 0x60FFFFFF : 0xFFFFFFFF;

        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + cardHeight, cardBg);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, y + cardHeight - 1, x + width, y + cardHeight, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + cardHeight, cardBorder);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + cardHeight, cardBorder);

        // 内容区域
        int contentX = x + innerMargin;
        int contentY = y + innerMargin;

        // 第一行：日期时间 + 已读/未读状态
        String dateTime = formatDateTime(notice.getPublishTime());
        String statusText = isRead ? "已读" : "未读";
        int statusColor = isRead ? 0xFF888888 : 0xFF4FC3F7;

        guiGraphics.drawString(font, dateTime, contentX, contentY, 0xFFAAAAAA);
        int statusWidth = font.width(statusText);
        guiGraphics.drawString(font, statusText, x + width - innerMargin - statusWidth, contentY, statusColor);

        // 第二行：标题
        String title = notice.getNoticeTitle();
        guiGraphics.drawString(font, title, contentX, contentY + font.lineHeight + 2,
            isRead ? 0xFFCCCCCC : 0xFFFFFFFF);

        // 第三行：内容（截断）
        String content = notice.getNoticeContent();
        int maxWidth = width - innerMargin * 2 - 10;
        if (font.width(content) > maxWidth) {
            content = truncateText(font, content, maxWidth) + "...";
        }
        guiGraphics.drawString(font, content, contentX, contentY + font.lineHeight * 2 + 4, 0xFF999999);
    }

    // ==================== 工具方法 ====================

    /**
     * 截断文本以适应最大宽度
     */
    public static String truncateText(Font font, String text, int maxWidth) {
        for (int i = 1; i < text.length(); i++) {
            if (font.width(text.substring(0, i)) > maxWidth) {
                return text.substring(0, i - 1);
            }
        }
        return text;
    }

    /**
     * 将文字按指定宽度换行
     */
    public static String[] wrapText(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            if (font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }

            int maxChars = 0;
            for (int i = 1; i <= remaining.length(); i++) {
                if (font.width(remaining.substring(0, i)) > maxWidth) {
                    maxChars = i - 1;
                    break;
                }
            }
            if (maxChars == 0) maxChars = 1;

            String line = remaining.substring(0, maxChars);
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace > 0) {
                line = line.substring(0, lastSpace);
                maxChars = lastSpace + 1;
            }

            lines.add(line);
            remaining = remaining.substring(maxChars);
        }

        return lines.toArray(new String[0]);
    }

    /**
     * 格式化数字（添加千分位分隔符）
     */
    public static String formatNumber(int num) {
        return String.format("%,d", num);
    }

    /**
     * 格式化时间戳为可读日期时间
     */
    public static String formatDateTime(long timestamp) {
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            java.time.ZoneId.systemDefault()
        );
        return String.format("%04d/%02d/%02d %02d:%02d",
            dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
            dateTime.getHour(), dateTime.getMinute());
    }

    /**
     * 根据 Rank 等级获取对应颜色
     */
    public static int getRankColor(int rankLevel) {
        return switch (rankLevel) {
            case 0 -> 0xFF888888;  // NO_RANK - 灰色
            case 1 -> 0xFF00AAFF;  // FISH - 蓝色
            case 2 -> 0xFF00FFFF;  // FISH+ - 青色
            case 3 -> 0xFF4FC3F7;  // FISH++ - 金色
            case 4 -> 0xFFFF0000;  // OPERATOR - 红色
            default -> 0xFF888888;
        };
    }

    /**
     * 根据感染值百分比计算动态颜色
     */
    public static int getInfectionColor(float infectionPercent) {
        float t = Math.max(0.0f, Math.min(1.0f, infectionPercent));
        float factor = t * t;

        int r = (int) (187 * (1.0f - factor));
        int g = (int) (255 - (255 - 51) * factor);
        int b = (int) (187 * (1.0f - factor));

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
