package com.mo.economy_system.screen.server_screen.tips;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class TipDisplayRenderer {
    //样式参数
    private static final int MARGIN = 5;
    private static final int INNER_PADDING = 7;
    private static final int CORNER_RADIUS = 3;
    private static final int ACCENT_WIDTH = 2;
    private static final int BG_COLOR = 0xD8181C24;
    private static final int WARNING_BG_COLOR = 0xDA241214;
    private static final int BORDER_COLOR = 0xAA8AB4FF;
    private static final int WARNING_BORDER_COLOR = 0xD8FF5555;
    private static final int GLOW_COLOR = 0x30456BA8;
    private static final int WARNING_GLOW_COLOR = 0x55FF3030;
    private static final int ACCENT_COLOR = 0xFF74A9FF;
    private static final int WARNING_ACCENT_COLOR = 0xFFFF3030;
    private static final int TEXT_COLOR = 0xFFEAF3FF;
    private static final int WARNING_TEXT_COLOR = 0xFFFF5555;
    private static final int LINE_SPACING = 3;
    private static final int MAX_WIDTH = 300;
    private static final String TIP_WORLD_BOUNDARY_KEYWORD = "世界边界";

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // F3 调试菜单打开时隐藏提示消息
        if (mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        List<TipMessage> messages = TipDisplayManager.getActiveMessages();
        if (messages.isEmpty()) {
            return;
        }

        // 计算文本总高度和宽度（使用 FormattedCharSequence 计算）
        int totalHeight = INNER_PADDING * 2;
        int maxTextWidth = 0;
        boolean hasWarningMessage = false;
        // 存储每行的 FormattedCharSequence
        List<List<FormattedCharSequence>> wrappedLinesList = new ArrayList<>();

        for (TipMessage msg : messages) {
            if (msg.getText() != null && msg.getText().contains(TIP_WORLD_BOUNDARY_KEYWORD)) {
                hasWarningMessage = true;
            }

            // 支持消息主动换行，同时保留过长文本的自动折行
            List<FormattedCharSequence> wrappedLines = splitMessageLines(mc, msg.getText());
            wrappedLinesList.add(wrappedLines);

            // 计算每行宽度（font.width 支持 FormattedCharSequence）
            for (FormattedCharSequence line : wrappedLines) {
                int lineWidth = mc.font.width(line);
                if (lineWidth > maxTextWidth) {
                    maxTextWidth = lineWidth;
                }
            }

            // 累加高度（行高 + 行间距）
            totalHeight += wrappedLines.size() * (mc.font.lineHeight + LINE_SPACING);
        }
        totalHeight -= LINE_SPACING; // 减去最后一行的多余行间距

        // 计算提示框位置
        int x = MARGIN;
        int y = MARGIN;
        int boxWidth = maxTextWidth + INNER_PADDING * 2 + ACCENT_WIDTH + 4;
        int boxHeight = totalHeight;
        int bgColor = hasWarningMessage ? WARNING_BG_COLOR : BG_COLOR;
        int borderColor = hasWarningMessage ? WARNING_BORDER_COLOR : BORDER_COLOR;
        int glowColor = hasWarningMessage ? WARNING_GLOW_COLOR : GLOW_COLOR;
        int accentColor = hasWarningMessage ? WARNING_ACCENT_COLOR : ACCENT_COLOR;
        int textColor = hasWarningMessage ? WARNING_TEXT_COLOR : TEXT_COLOR;

        // 绘制圆角背景、柔和边框和左侧强调条
        drawRoundedRect(guiGraphics, x - 1, y - 1, boxWidth + 2, boxHeight + 2, CORNER_RADIUS + 1, glowColor);
        drawRoundedRect(guiGraphics, x, y, boxWidth, boxHeight, CORNER_RADIUS, bgColor);
        drawRoundedBorder(guiGraphics, x, y, boxWidth, boxHeight, CORNER_RADIUS, borderColor);
        drawRoundedRect(guiGraphics, x + INNER_PADDING - 2, y + INNER_PADDING - 1,
                ACCENT_WIDTH, boxHeight - INNER_PADDING * 2 + 2, 1, accentColor);

        // 绘制文本
        int textX = x + INNER_PADDING + ACCENT_WIDTH + 5;
        int currentY = y + INNER_PADDING;
        for (List<FormattedCharSequence> lines : wrappedLinesList) {
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(mc.font, line, textX, currentY, textColor);
                currentY += mc.font.lineHeight + LINE_SPACING;
            }
        }
    }

    private static List<FormattedCharSequence> splitMessageLines(Minecraft mc, String text) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] manualLines = text.split("\\R");
        for (String manualLine : manualLines) {
            lines.addAll(mc.font.split(Component.literal(manualLine), MAX_WIDTH));
        }
        return lines;
    }

    private static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            guiGraphics.fill(RenderType.guiOverlay(), x, y, x + width, y + height, color);
            return;
        }

        int r = Math.min(radius, Math.min(width / 2, height / 2));
        int right = x + width;
        int bottom = y + height;
        guiGraphics.fill(RenderType.guiOverlay(), x + r, y, right - r, bottom, color);
        guiGraphics.fill(RenderType.guiOverlay(), x, y + r, right, bottom - r, color);

        if (r >= 2) {
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 1, x + r, y + r, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - r, y + 1, right - 1, y + r, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, bottom - r, x + r, bottom - 1, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - r, bottom - r, right - 1, bottom - 1, color);
        }

        if (r >= 3) {
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 2, x + 2, bottom - 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 2, y + 2, right - 1, bottom - 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 2, y + 1, right - 2, y + 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 2, bottom - 2, right - 2, bottom - 1, color);
        }
    }

    private static void drawRoundedBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            guiGraphics.fill(RenderType.guiOverlay(), x, y, x + width, y + 1, color);
            guiGraphics.fill(RenderType.guiOverlay(), x, y + height - 1, x + width, y + height, color);
            guiGraphics.fill(RenderType.guiOverlay(), x, y, x + 1, y + height, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + width - 1, y, x + width, y + height, color);
            return;
        }

        int r = Math.min(radius, Math.min(width / 2, height / 2));
        int right = x + width;
        int bottom = y + height;
        guiGraphics.fill(RenderType.guiOverlay(), x + r, y, right - r, y + 1, color);
        guiGraphics.fill(RenderType.guiOverlay(), x + r, bottom - 1, right - r, bottom, color);
        guiGraphics.fill(RenderType.guiOverlay(), x, y + r, x + 1, bottom - r, color);
        guiGraphics.fill(RenderType.guiOverlay(), right - 1, y + r, right, bottom - r, color);

        if (r >= 2) {
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 1, x + 2, y + 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 2, y + 1, right - 1, y + 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, bottom - 2, x + 2, bottom - 1, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 2, bottom - 2, right - 1, bottom - 1, color);
        }

        if (r >= 3) {
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, y + 2, x + 2, y + 3, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 2, y + 1, x + 3, y + 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 2, y + 2, right - 1, y + 3, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 3, y + 1, right - 2, y + 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 1, bottom - 3, x + 2, bottom - 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), x + 2, bottom - 2, x + 3, bottom - 1, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 2, bottom - 3, right - 1, bottom - 2, color);
            guiGraphics.fill(RenderType.guiOverlay(), right - 3, bottom - 2, right - 2, bottom - 1, color);
        }
    }
}
