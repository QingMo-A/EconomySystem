package com.mo.economy_system.screen.server_screen.customsystemui;

import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 系统消息显示系统 - 在玩家信息框下方显示系统消息（玩家进服、离开、死亡等）
 */
@EventBusSubscriber(modid = "economy_system", value = Dist.CLIENT)
public class SystemMessageDisplay {
    // 消息显示配置
    private static final int MAX_MESSAGES = 10;
    private static final int MESSAGE_DISPLAY_TIME = 8000;
    private static final int MESSAGE_FADE_TIME = 1000;
    private static final int BOX_PADDING = 5;
    private static final int BOX_SPACING = 3;
    private static final int RIGHT_OFFSET = 2;
    private static final float MESSAGE_TEXT_SCALE = 0.85f;
    private static final int CORNER_RADIUS = 2;
    private static final int ACCENT_WIDTH = 2;
    private static final int LEFT_PAD = 5;
    private static final int RIGHT_PAD = 6;
    private static final int GAP_AFTER_ACCENT = 3;

    // 颜色定义
    private static final int COLOR_TASK = 0x55FF55;         // 普通进度 - 绿色
    private static final int COLOR_GOAL = 0x55FFFF;         // 目标 - 蓝色
    private static final int COLOR_CHALLENGE = 0xAA00AA;    // 挑战 - 紫色

    // 消息列表
    private static final List<SystemMessage> messages = new ArrayList<>();

    /**
     * 系统消息数据类
     */
    private static class SystemMessage {
        final Component text;
        final long createTime;
        final int borderColor; // 每条消息有自己的边框颜色

        SystemMessage(Component text, int borderColor) {
            this.text = text;
            this.borderColor = borderColor;
            this.createTime = System.currentTimeMillis();
        }

        long getAge() {
            return System.currentTimeMillis() - createTime;
        }

        float getAlpha() {
            long age = getAge();
            if (age < MESSAGE_DISPLAY_TIME - MESSAGE_FADE_TIME) {
                return 1.0f;
            } else if (age < MESSAGE_DISPLAY_TIME) {
                return (MESSAGE_DISPLAY_TIME - age) / (float) MESSAGE_FADE_TIME;
            } else {
                return 0.0f;
            }
        }

        boolean isExpired() {
            return getAge() >= MESSAGE_DISPLAY_TIME;
        }

        // 获取实际使用的边框颜色
        int getActualBorderColor() {
            // 如果是 -1，使用本地玩家 Rank 颜色
            if (borderColor == -1) {
                return getPlayerRankBorderColor();
            }
            return 0xFF000000 | borderColor;
        }
    }

    /**
     * 添加系统消息（使用指定边框颜色）
     * @param text 消息文本
     * @param borderColor 边框颜色（RGB），-1 表示使用本地玩家 Rank 颜色
     */
    public static void addMessage(Component text, int borderColor) {
        messages.add(new SystemMessage(text, borderColor));

        // 限制消息数量
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    /**
     * 添加系统消息（使用本地玩家 Rank 颜色）
     * @param text 消息文本
     */
    public static void addMessage(Component text) {
        addMessage(text, -1); // -1 表示使用本地玩家 Rank 颜色
    }

    /**
     * 清除所有消息
     */
    public static void clearMessages() {
        messages.clear();
    }

    /**
     * 客户端Tick - 清理过期消息
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // 移除过期消息
        Iterator<SystemMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) {
                iterator.remove();
            }
        }
    }

    /**
     * 渲染系统消息框（在RenderGuiEvent.Post中调用）
     */
    public static void renderSystemMessages(GuiGraphics guiGraphics, Font font, int screenWidth, int playerInfoBoxY, int playerInfoBoxHeight) {
        if (messages.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        // Follow vanilla HUD visibility: F1 hides custom system messages too.
        if (mc.options.hideGui) return;

        // F3 调试菜单打开时隐藏系统消息
        if (mc.getDebugOverlay().showDebugScreen()) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 消息框起始位置（玩家信息框下方）
        int baseY = playerInfoBoxY + playerInfoBoxHeight + BOX_SPACING;
        int lineHeight = (int)(font.lineHeight * MESSAGE_TEXT_SCALE) + BOX_PADDING * 2;

        // 从下往上渲染消息
        int currentY = baseY;

        for (int i = messages.size() - 1; i >= 0; i--) {
            SystemMessage message = messages.get(i);

            // 计算消息框宽度：左内边距 + 强调条 + 间距 + 缩放后文字宽 + 右内边距
            int textWidth = font.width(message.text);
            int scaledTextWidth = (int)(textWidth * MESSAGE_TEXT_SCALE);
            int boxWidth = LEFT_PAD + ACCENT_WIDTH + GAP_AFTER_ACCENT + scaledTextWidth + RIGHT_PAD;

            // 框的位置（右上角对齐玩家信息框）
            int boxX = screenWidth - boxWidth - RIGHT_OFFSET;
            int boxY = currentY;

            // 获取该消息的边框颜色
            int borderColor = message.getActualBorderColor();

            // 渲染消息框
            renderMessageBox(guiGraphics, font, boxX, boxY, boxWidth, lineHeight, message, borderColor);

            currentY += lineHeight + BOX_SPACING;
        }

        poseStack.popPose();
    }

    /**
     * 渲染单个消息框（圆角 + 左侧细强调条 + 柔和边框）
     */
    private static void renderMessageBox(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                        SystemMessage message, int borderColor) {
        float alpha = message.getAlpha();
        if (alpha <= 0.0f) return;

        int aBg = Math.min(0xD8, (int)(0xD8 * alpha));
        int aGlow = Math.min(0x38, (int)(0x38 * alpha));
        int aBorder = Math.min(0xCC, (int)(0xCC * alpha));

        int bgColor = (aBg << 24) | 0x151A22;
        int glowColor = (aGlow << 24) | (borderColor & 0x00FFFFFF);
        int borderColorFinal = (aBorder << 24) | (borderColor & 0x00FFFFFF);
        int accentColor = (aBorder << 24) | (borderColor & 0x00FFFFFF);

        // 柔和外发光
        drawRoundedRect(guiGraphics, x - 1, y - 1, width + 2, height + 2, CORNER_RADIUS + 1, glowColor);

        // 主体背景（圆角）
        drawRoundedRect(guiGraphics, x, y, width, height, CORNER_RADIUS, bgColor);

        // 圆角边框
        drawRoundedBorder(guiGraphics, x, y, width, height, CORNER_RADIUS, borderColorFinal);

        // 左侧细强调条
        int accentX = x + LEFT_PAD - 2;
        int accentY = y + BOX_PADDING;
        int accentH = height - BOX_PADDING * 2;
        guiGraphics.fill(RenderType.gui(), accentX, accentY, accentX + ACCENT_WIDTH, accentY + accentH, accentColor);

        // 渲染文本
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int textX = x + LEFT_PAD + ACCENT_WIDTH + GAP_AFTER_ACCENT;
        int scaledTextH = (int)(font.lineHeight * MESSAGE_TEXT_SCALE);
        int textY = y + (height - scaledTextH) / 2;

        poseStack.translate(textX, textY, 0);
        poseStack.scale(MESSAGE_TEXT_SCALE, MESSAGE_TEXT_SCALE, 1.0f);

        guiGraphics.drawString(font, message.text, 0, 0, 0xFFFFFFFF, false);

        poseStack.popPose();
    }

    // ==================== 圆角绘制辅助 ====================

    private static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0 || (color >>> 24) == 0) {
            if ((color >>> 24) != 0) {
                guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, color);
            }
            return;
        }
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        int right = x + width;
        int bottom = y + height;

        guiGraphics.fill(RenderType.gui(), x + r, y, right - r, bottom, color);
        guiGraphics.fill(RenderType.gui(), x, y + r, right, bottom - r, color);

        if (r >= 2) {
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + r, y + r, color);
            guiGraphics.fill(RenderType.gui(), right - r, y + 1, right - 1, y + r, color);
            guiGraphics.fill(RenderType.gui(), x + 1, bottom - r, x + r, bottom - 1, color);
            guiGraphics.fill(RenderType.gui(), right - r, bottom - r, right - 1, bottom - 1, color);
        }
        if (r >= 3) {
            guiGraphics.fill(RenderType.gui(), x + 1, y + 2, x + 2, bottom - 2, color);
            guiGraphics.fill(RenderType.gui(), right - 2, y + 2, right - 1, bottom - 2, color);
            guiGraphics.fill(RenderType.gui(), x + 2, y + 1, right - 2, y + 2, color);
            guiGraphics.fill(RenderType.gui(), x + 2, bottom - 2, right - 2, bottom - 1, color);
        }
    }

    private static void drawRoundedBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        int a = color >>> 24;
        if (a == 0) return;
        if (radius <= 0) {
            guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, color);
            guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, color);
            guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, color);
            guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, color);
            return;
        }
        int r = Math.min(radius, Math.min(width / 2, height / 2));
        int right = x + width;
        int bottom = y + height;

        guiGraphics.fill(RenderType.gui(), x + r, y, right - r, y + 1, color);
        guiGraphics.fill(RenderType.gui(), x + r, bottom - 1, right - r, bottom, color);
        guiGraphics.fill(RenderType.gui(), x, y + r, x + 1, bottom - r, color);
        guiGraphics.fill(RenderType.gui(), right - 1, y + r, right, bottom - r, color);

        if (r >= 2) {
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + 2, y + 2, color);
            guiGraphics.fill(RenderType.gui(), right - 2, y + 1, right - 1, y + 2, color);
            guiGraphics.fill(RenderType.gui(), x + 1, bottom - 2, x + 2, bottom - 1, color);
            guiGraphics.fill(RenderType.gui(), right - 2, bottom - 2, right - 1, bottom - 1, color);
        }
        if (r >= 3) {
            guiGraphics.fill(RenderType.gui(), x + 1, y + 2, x + 2, y + 3, color);
            guiGraphics.fill(RenderType.gui(), x + 2, y + 1, x + 3, y + 2, color);
            guiGraphics.fill(RenderType.gui(), right - 2, y + 2, right - 1, y + 3, color);
            guiGraphics.fill(RenderType.gui(), right - 3, y + 1, right - 2, y + 2, color);
            guiGraphics.fill(RenderType.gui(), x + 1, bottom - 3, x + 2, bottom - 2, color);
            guiGraphics.fill(RenderType.gui(), x + 2, bottom - 2, x + 3, bottom - 1, color);
            guiGraphics.fill(RenderType.gui(), right - 2, bottom - 3, right - 1, bottom - 2, color);
            guiGraphics.fill(RenderType.gui(), right - 3, bottom - 2, right - 2, bottom - 1, color);
        }
    }

    /**
     * 获取本地玩家 Rank 的边框颜色
     */
    private static int getPlayerRankBorderColor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0xFFFFFF; // 默认白色
        }

        // 获取本地玩家的 Rank 颜色
        var rank = PlayerRankManager.getPlayerRankClient(mc.player);
        return 0xFF000000 | rank.getRankColor();
    }
}
