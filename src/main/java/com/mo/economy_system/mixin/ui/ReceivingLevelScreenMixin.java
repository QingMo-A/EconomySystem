package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ReceivingLevelScreen Mixin
 * 使用虚拟坐标系统（640×360）的自定义加载世界界面
 */
@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin extends Screen {

    private static final int BG_OUTER = 0xDD00000A;
    private static final int BG_INNER = 0xEE05051A;
    private static final int BORDER_DARK = 0xFF00003D;
    private static final int BORDER_GLOW = 0xFF1A1AFF;
    private static final int ACCENT_BLUE = 0xFF0088FF;

    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    protected ReceivingLevelScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderCustomScreen(guiGraphics, partialTick);
    }

    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics, float partialTick) {
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        guiGraphics.fillGradient(0, 0, this.width, this.height, BG_OUTER, BG_OUTER);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 450;
        int boxHeight = 180;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
        renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

        PoseStack poseStack = guiGraphics.pose();

        // 世界图标
        String worldIcon = "⚛";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int iconX = (int) ((boxX + 30) / 3.0f);
        int iconY = (int) ((boxY + 25) / 3.0f);
        guiGraphics.drawString(this.font, worldIcon, iconX, iconY, ACCENT_BLUE, false);
        poseStack.popPose();

        // 标题
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        String titleText = "§9§l正在加载世界...";
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
        guiGraphics.fill(boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 2, ACCENT_BLUE);
        guiGraphics.fill(boxX + PADDING, lineY + 3, boxX + boxWidth - PADDING, lineY + 4, 0xAA000066);

        // 加载提示
        String[] tips = {
            "§7正在构建世界地形...",
            "§7正在生成生物群系...",
            "§7正在加载区块数据...",
            "§7正在准备世界生成..."
        };

        long time = System.currentTimeMillis();
        int tipIndex = (int) ((time / 3000) % tips.length);
        int tipY = boxY + 80;
        guiGraphics.drawCenteredString(this.font, tips[tipIndex], centerX, tipY, 0xFFFFFFFF);

        // 加载动画
        int dots = (int) ((time / 500) % 4);
        String loadingDots = "";
        for (int i = 0; i < dots; i++) loadingDots += ".";

        int loadingY = boxY + 115;
        guiGraphics.drawCenteredString(this.font, "§9正在进入世界" + loadingDots, centerX, loadingY, 0xFFFFFFFF);

        // 进度条
        int progressBarY = boxY + boxHeight - 25;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;

        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 10, 0xAA000000);

        int progressWidth = (int) ((time % 2000) / 2000.0f * progressBarWidth);
        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 10, ACCENT_BLUE);

        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 2, 0xFF55AAFF);

        // 进度百分比
        int percent = (int) ((time % 2000) / 2000.0f * 100);
        String percentText = "§b" + percent + "%";
        guiGraphics.drawCenteredString(this.font, percentText, centerX, progressBarY + 12, 0xFFFFFFFF);

        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }
}
