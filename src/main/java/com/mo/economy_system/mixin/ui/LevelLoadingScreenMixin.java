package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelLoadingScreen Mixin
 * 使用虚拟坐标系统（640×360）的自定义生成世界界面
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    private static final int BG_OUTER = 0xDD00000A;
    private static final int BG_INNER = 0xEE05051A;
    private static final int BORDER_DARK = 0xFF00003D;
    private static final int BORDER_GLOW = 0xFF1A1AFF;
    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int ACCENT_CYAN = 0xFF00FFFF;

    private static final int PADDING = 12;

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    protected LevelLoadingScreenMixin(Component title) {
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

        int boxWidth = 420;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
        renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

        PoseStack poseStack = guiGraphics.pose();

        // 生成图标
        String genIcon = "✦";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int iconX = (int) ((boxX + 30) / 3.0f);
        int iconY = (int) ((boxY + 25) / 3.0f);
        guiGraphics.drawString(this.font, genIcon, iconX, iconY, ACCENT_CYAN, false);
        poseStack.popPose();

        // 标题 - 显示生成世界中
        String titleText = "§9§l生成世界中...";

        poseStack.pushPose();
        poseStack.scale(1.8f, 1.8f, 1.0f);
        int titleX = (int) ((centerX + 10) / 1.8f - font.width(titleText) / 2.0f);
        int titleY = (int) ((boxY + 32) / 1.8f);
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

        long time = System.currentTimeMillis();

        // 获取真实进度
        int realProgress = Mth.clamp(progressListener.getProgress(), 0, 100);

        // 进度条 - 使用真实的游戏进度
        int progressBarY = boxY + boxHeight - 55;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;

        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 12, 0xAA000000);

        // 进度条填充
        int progressWidth = (int) (realProgress / 100.0f * progressBarWidth);
        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 12, ACCENT_BLUE);
        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 3, 0xFF55AAFF);

        // 闪烁效果
        if ((time / 500) % 2 == 0) {
            guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 12, 0x330055FF);
        }

        // 进度百分比
        String percentText = "§b§l" + realProgress + "%";
        guiGraphics.drawCenteredString(this.font, percentText, centerX, progressBarY + 15, 0xFFFFFFFF);

        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }
}
