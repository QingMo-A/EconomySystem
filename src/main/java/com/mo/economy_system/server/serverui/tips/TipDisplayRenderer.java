package com.mo.economy_system.server.serverui.tips;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class TipDisplayRenderer {
    //样式参数
    private static final int MARGIN = 5;
    private static final int BORDER_SIZE = 1;
    private static final int BACKGROUND_ALPHA = 180;
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int LINE_SPACING = 3;
    private static final int MAX_WIDTH = 300;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        List<TipMessage> messages = TipDisplayManager.getActiveMessages();
        if (messages.isEmpty()) {
            return;
        }

        // 计算文本总高度和宽度（使用 FormattedCharSequence 计算）
        int totalHeight = MARGIN * 2;
        int maxTextWidth = 0;
        // 存储每行的 FormattedCharSequence
        List<List<FormattedCharSequence>> wrappedLinesList = new ArrayList<>();

        for (TipMessage msg : messages) {
            // 直接获取拆分后的 FormattedCharSequence 列表
            List<FormattedCharSequence> wrappedLines = mc.font.split(Component.literal(msg.getText()), MAX_WIDTH);
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

        // 计算黑框位置
        int x = MARGIN;
        int y = MARGIN;
        int boxWidth = maxTextWidth + MARGIN * 2;
        int boxHeight = totalHeight;

        // 绘制黑框背景和边框
        guiGraphics.fill(RenderType.gui(), x, y, x + boxWidth, y + boxHeight, BG_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + boxWidth, y + BORDER_SIZE, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y + boxHeight - BORDER_SIZE, x + boxWidth, y + boxHeight, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + BORDER_SIZE, y + boxHeight, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x + boxWidth - BORDER_SIZE, y, x + boxWidth, y + boxHeight, BORDER_COLOR);

        // 绘制文本
        int currentY = y + MARGIN;
        for (List<FormattedCharSequence> lines : wrappedLinesList) {
            for (FormattedCharSequence line : lines) {
                guiGraphics.drawString(mc.font, line, x + MARGIN, currentY, TEXT_COLOR);
                currentY += mc.font.lineHeight + LINE_SPACING;
            }
        }
    }
}