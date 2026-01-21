package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GenericDirtMessageScreen Mixin
 * 使用虚拟坐标系统（640×360）的自定义加入世界界面
 */
@Mixin(GenericDirtMessageScreen.class)
public abstract class GenericDirtMessageScreenMixin extends Screen {

    private static final int BG_OUTER = 0xDD00000A;
    private static final int BG_INNER = 0xEE05051A;
    private static final int BORDER_DARK = 0xFF00003D;
    private static final int BORDER_GLOW = 0xFF1A1AFF;
    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int ACCENT_CYAN = 0xFF00FFFF;

    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    protected GenericDirtMessageScreenMixin(Component title) {
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

        int boxWidth = 400;
        int boxHeight = 140;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
        renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

        PoseStack poseStack = guiGraphics.pose();

        // 图标
        String icon = "⟳";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int iconX = (int) ((boxX + 30) / 3.0f);
        int iconY = (int) ((boxY + 25) / 3.0f);
        guiGraphics.drawString(this.font, icon, iconX, iconY, ACCENT_CYAN, false);
        poseStack.popPose();

        // 标题
        String titleText = this.title != null ? this.title.getString() : "加入世界中...";
        if (!titleText.contains("§")) {
            titleText = "§9§l" + titleText;
        }

        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
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

        // 消息文本
        int messageY = boxY + 85;
        String displayMessage = this.title != null ? this.title.getString() : "";
        guiGraphics.drawCenteredString(this.font, "§7" + displayMessage, centerX, messageY, 0xFFFFFFFF);

        // 加载动画
        long time = System.currentTimeMillis();

        // 旋转的圆点
        int dotCount = 8;
        int radius = 25;
        int dotCenterX = centerX;
        int dotCenterY = boxY + 110;

        for (int i = 0; i < dotCount; i++) {
            double angle = (i * 2 * Math.PI / dotCount) + (time / 500.0);
            int dotX = dotCenterX + (int) (Math.cos(angle) * radius);
            int dotY = dotCenterY + (int) (Math.sin(angle) * radius);

            int alpha = (int) (255 * (0.3 + 0.7 * ((Math.sin(time / 200.0 + i) + 1) / 2)));
            int dotColor = (alpha << 24) | 0x0088FF;

            guiGraphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, dotColor);
        }

        // 中心点
        guiGraphics.fill(dotCenterX - 3, dotCenterY - 3, dotCenterX + 3, dotCenterY + 3, ACCENT_CYAN);

        // 底部进度线
        int progressY = boxY + boxHeight - 15;
        int progressWidth = boxWidth - 2 * PADDING;
        int progressX = boxX + PADDING;

        int currentProgress = (int) ((time % 2000) / 2000.0f * progressWidth);
        guiGraphics.fill(progressX, progressY, progressX + progressWidth, progressY + 3, 0xAA000000);
        guiGraphics.fill(progressX, progressY, progressX + currentProgress, progressY + 3, ACCENT_BLUE);

        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }
}
