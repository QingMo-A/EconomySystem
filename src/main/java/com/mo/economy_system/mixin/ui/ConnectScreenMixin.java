package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    @Unique
    private Button economySystem$disconnectButton;

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void economySystem$init(CallbackInfo ci) {
        ci.cancel();
        initCustomScreen();
    }

    @Unique
    private void initCustomScreen() {
        // 计算虚拟尺寸
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        // 虚拟坐标下的按钮位置
        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 400;
        int boxHeight = 170;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = boxWidth - 2 * PADDING;  // 与进度条同宽
        int buttonHeight = 24;
        // 按钮在进度条下方
        int virtualButtonX = boxX + PADDING;
        int virtualButtonY = boxY + 132;
        // 转换虚拟坐标到屏幕坐标
        int screenButtonX = (int) (virtualButtonX * virtualSize.uiScale);
        int screenButtonY = (int) (virtualButtonY * virtualSize.uiScale);
        int screenButtonWidth = (int) (buttonWidth * virtualSize.uiScale);
        int screenButtonHeight = (int) (buttonHeight * virtualSize.uiScale);

        economySystem$disconnectButton = new CustomButton(
                screenButtonX, screenButtonY,
                screenButtonWidth, screenButtonHeight,
                Component.literal("§a取消连接"),
                btn -> economySystem$disconnect(),
                virtualSize.uiScale
        );
        this.addRenderableWidget(economySystem$disconnectButton);
    }

    @Unique
    private void economySystem$disconnect() {
        Minecraft mc = Minecraft.getInstance();

        // 断开当前连接并返回标题界面
        if (mc.getConnection() != null) {
            mc.getConnection().close();
        }
        if (mc.level != null) {
            mc.level.disconnect();
        }
        mc.clearLevel(null);
        mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderCustomScreen(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
        int boxHeight = 170;
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
        int statusY = boxY + 75;
        guiGraphics.drawCenteredString(this.font, "§7正在建立连接...", centerX, statusY, 0xFFFFFFFF);

        // 加载动画
        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String loadingDots = "";
        for (int i = 0; i < dots; i++) loadingDots += ".";

        int loadingY = boxY + 95;
        guiGraphics.drawCenteredString(this.font, "§a请稍候" + loadingDots, centerX, loadingY, 0xFFFFFFFF);

        // 进度条（在按钮上方，间距较大）
        int progressBarY = boxY + 112;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;

        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + 6, 0xAA000000);

        int progressWidth = (int) ((time % 2000) / 2000.0f * progressBarWidth);
        guiGraphics.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + 6, ACCENT_GREEN);

        // 恢复矩阵
        guiGraphics.pose().popPose();

        // ========== 渲染按钮（使用屏幕坐标） ==========
        if (economySystem$disconnectButton != null) {
            economySystem$disconnectButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    /**
     * 自定义断开连接按钮（使用屏幕坐标）
     */
    @Unique
    private static class CustomButton extends Button {
        private final float virtualScale;

        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress, float virtualScale) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.virtualScale = virtualScale;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered() || isFocused();

            int topColor, bottomColor, borderColor;
            if (hovered) {
                topColor = 0xCC006600;
                bottomColor = 0xCC003300;
                borderColor = 0xFF00CC00;
            } else {
                topColor = 0xCC004400;
                bottomColor = 0xCC002200;
                borderColor = 0xCC008800;
            }

            int x = getX();
            int y = getY();
            int w = width;
            int h = height;

            // 渐变背景
            guiGraphics.fill(x + 2, y, x + w - 2, y + h, topColor);
            guiGraphics.fill(x + 2, y + h, x + w - 2, y + h + 1, bottomColor);

            // 边框
            guiGraphics.fill(x + 1, y, x + 2, y + h, borderColor);
            guiGraphics.fill(x + w - 2, y, x + w - 1, y + h, borderColor);
            guiGraphics.fill(x + 2, y, x + w - 2, y + 1, borderColor);
            guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, borderColor);

            // 角落装饰
            guiGraphics.fill(x, y, x + 1, y + 1, borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + 1, borderColor);
            guiGraphics.fill(x, y + h - 1, x + 1, y + h, borderColor);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, borderColor);

            // 文字
            String displayText = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, 0xFFFFFF, false);
        }
    }
}
