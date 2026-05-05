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
    private static final int MAX_MESSAGES = 10;              // 最多显示5条消息
    private static final int MESSAGE_DISPLAY_TIME = 8000;   // 每条消息显示8秒
    private static final int MESSAGE_FADE_TIME = 1000;      // 消失前1秒淡出
    private static final int BOX_PADDING = 4;               // 框内边距
    private static final int BOX_SPACING = 2;               // 消息之间间距
    private static final int RIGHT_OFFSET = 2;              // 右侧偏移
    private static final float MESSAGE_TEXT_SCALE = 0.85f;  // 文字缩放比例

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

            // 计算消息框宽度
            int textWidth = font.width(message.text);
            int scaledTextWidth = (int)(textWidth * MESSAGE_TEXT_SCALE);
            int boxWidth = scaledTextWidth + BOX_PADDING * 2;

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
     * 渲染单个消息框
     */
    private static void renderMessageBox(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                        SystemMessage message, int borderColor) {
        float alpha = message.getAlpha();

        // 如果完全透明，不渲染
        if (alpha <= 0.0f) return;

        // 计算带透明度的颜色
        int bgColor = ((int)(0xD0 * alpha) & 0xFF) << 24 | 0x181818;
        int glowColor = ((int)(0x40 * alpha) & 0xFF) << 24 | (borderColor & 0x00FFFFFF);
        int finalBorderColor = ((int)(0xFF * alpha) & 0xFF) << 24 | (borderColor & 0x00FFFFFF);

        // 背景和发光效果
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 边框（上、下、左、右）
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, finalBorderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, finalBorderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, finalBorderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, finalBorderColor);

        // 渲染文本（应用缩放）- 保持原版颜色
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        int textX = x + BOX_PADDING;
        int textY = y + BOX_PADDING;

        poseStack.translate(textX, textY, 0);
        poseStack.scale(MESSAGE_TEXT_SCALE, MESSAGE_TEXT_SCALE, 1.0f);

        // 直接绘制Component，保持原版颜色
        guiGraphics.drawString(font, message.text, 0, 0, 0xFFFFFFFF, false);

        poseStack.popPose();
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
