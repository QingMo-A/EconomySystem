package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ConnectScreen Mixin
 * 使用虚拟坐标系统（640×360）的自定义连接服务器界面
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    // ==================== 颜色定义 - 绿色调 ====================
    private static final int BG_OUTER = 0xDD001A00;
    private static final int BG_INNER = 0xEE051A05;
    private static final int BORDER_DARK = 0xFF003D00;
    private static final int BORDER_GLOW = 0xFF1AFF1A;
    private static final int ACCENT_GREEN = 0xFF00CC00;

    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderCustomScreen(guiGraphics, partialTick);
    }

    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics, float partialTick) {
        // 计算虚拟尺寸
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        // 背景填充（屏幕坐标）
        guiGraphics.fillGradient(0, 0, this.width, this.height, BG_OUTER, BG_OUTER);

        // 应用虚拟坐标缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        // 虚拟坐标布局
        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 400;
        int boxHeight = 140;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // 主面板
        renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
        renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

        PoseStack poseStack = guiGraphics.pose();

        // 连接图标
        String icon = "⚡";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int iconX = (int) ((boxX + 30) / 3.0f);
        int iconY = (int) ((boxY + 25) / 3.0f);
        guiGraphics.drawString(this.font, icon, iconX, iconY, ACCENT_GREEN, false);
        poseStack.popPose();

        // 标题
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        String titleText = "§a§l正在连接服务器...";
        int titleX = (int) ((centerX + 10) / 2.0f - font.width(titleText) / 2.0f);
        int titleY = (int) ((boxY + 30) / 2.0f);
        guiGraphics.drawString(this.font, titleText, titleX, titleY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // 右上角
        String domainText = "§b§lDreaming§d§lFish";
        int domainX = boxX + boxWidth - PADDING - font.width(domainText);
        int domainY = boxY + 15;
        guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFFFF, false);

        // 分隔线
        int lineY = boxY + 55;
        guiGraphics.fill(boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 2, ACCENT_GREEN);
        guiGraphics.fill(boxX + PADDING, lineY + 3, boxX + boxWidth - PADDING, lineY + 4, 0xAA006600);

        // 状态文本
        int statusY = boxY + 80;
        guiGraphics.drawCenteredString(this.font, "§7正在建立连接...", centerX, statusY, 0xFFFFFFFF);

        // 加载动画
        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String loadingDots = "";
        for (int i = 0; i < dots; i++) loadingDots += ".";

        int loadingY = boxY + 110;
        guiGraphics.drawCenteredString(this.font, "§a请稍候" + loadingDots, centerX, loadingY, 0xFFFFFFFF);

        // 进度条
        int progressBarY = boxY + boxHeight - 20;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;

        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 8, 0xAA000000);

        int progressWidth = (int) ((time % 2000) / 2000.0f * progressBarWidth);
        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 8, ACCENT_GREEN);

        // 恢复矩阵
        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }
}
