package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ConnectScreen Mixin
 * 连接界面：绿色玻璃风格，布局与连接失败界面一致。
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    private static final int ACCENT_GREEN = 0xFF3FBF7F;
    private static final int TITLE_COLOR = 0xFFB8FFD6;
    private static final int STATUS_COLOR = 0xFFCFE9DA;

    private static final int GLASS_TOP = 0x663FAF7F;
    private static final int GLASS_BOTTOM = 0x33102218;
    private static final int GLASS_BORDER = 0x5590D9B0;
    private static final int GLASS_SHADOW = 0x33201028;
    private static final int GLASS_HIGHLIGHT = 0x66B8FFD5;

    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

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
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 420;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 380;
        int buttonHeight = 24;
        int virtualButtonX = boxX + (boxWidth - buttonWidth) / 2;
        int virtualButtonY = boxY + boxHeight - 40;

        int screenButtonX = (int) (virtualButtonX * virtualSize.uiScale);
        int screenButtonY = (int) (virtualButtonY * virtualSize.uiScale);
        int screenButtonWidth = (int) (buttonWidth * virtualSize.uiScale);
        int screenButtonHeight = (int) (buttonHeight * virtualSize.uiScale);

        economySystem$disconnectButton = new CustomButton(
            screenButtonX,
            screenButtonY,
            screenButtonWidth,
            screenButtonHeight,
            Component.literal("取消连接"),
            btn -> economySystem$disconnect()
        );
        this.addRenderableWidget(economySystem$disconnectButton);
    }

    @Unique
    private void economySystem$disconnect() {
        Minecraft mc = Minecraft.getInstance();
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
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        UiBackgroundRenderer.renderCover(guiGraphics, BACKGROUND_TEXTURE, this.width, this.height);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 420;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        economySystem$renderGlassPanel(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0xAA78C89A);

        // 左上角闪电标志
        PoseStack iconPose = guiGraphics.pose();
        iconPose.pushPose();
        iconPose.scale(2.6f, 2.6f, 1.0f);
        int iconX = (int) ((boxX + 20) / 2.6f);
        int iconY = (int) ((boxY + 18) / 2.6f);
        guiGraphics.drawString(this.font, "⚡", iconX, iconY, ACCENT_GREEN, false);
        iconPose.popPose();

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(2.2f, 2.2f, 1.0f);
        String titleText = "§l正在连接服务器";
        int titleX = (int) ((centerX + 15) / 2.2f - font.width(titleText) / 2.0f);
        int titleY = (int) ((boxY + 28) / 2.2f);
        guiGraphics.drawString(this.font, titleText, titleX, titleY, TITLE_COLOR, false);
        poseStack.popPose();

        String domainText = "§b§lDreaming§d§lFish";
        int domainX = boxX + boxWidth - 12 - font.width(domainText);
        int domainY = boxY + 15;
        guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFFFF, false);

        int lineY = boxY + 55;
        guiGraphics.fill(boxX + 12, lineY, boxX + boxWidth - 12, lineY + 2, ACCENT_GREEN);
        guiGraphics.fill(boxX + 12, lineY + 3, boxX + boxWidth - 12, lineY + 4, 0xAA2C6E4A);

        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String loadingDots = ".".repeat(dots);
        guiGraphics.drawCenteredString(this.font, "正在建立连接" + loadingDots, centerX, boxY + 90, STATUS_COLOR);

        int progressBarY = boxY + 132;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;
        int progressBarHeight = 6;

        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressBarWidth, progressBarHeight, 0x66000000);
        int progressWidth = (int) ((time % 2000) / 2000.0f * progressBarWidth);
        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, ACCENT_GREEN);
        if (progressWidth > 2) {
            guiGraphics.fill(progressBarX + 2, progressBarY, progressBarX + progressWidth - 2, progressBarY + 1, 0xFF8EF0B8);
        }

        guiGraphics.pose().popPose();

        if (economySystem$disconnectButton != null) {
            economySystem$disconnectButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void economySystem$renderGlassPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int tint) {
        guiGraphics.fillGradient(x, y, x + width, y + height, GLASS_TOP, GLASS_BOTTOM);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, economySystem$withAlpha(tint, 0x12));
        guiGraphics.fill(x, y, x + width, y + 1, GLASS_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x, y, x + 1, y + height, GLASS_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, GLASS_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, GLASS_SHADOW);
        economySystem$renderGlassNoise(guiGraphics, x, y, width, height);
    }

    @Unique
    private void economySystem$renderGlassNoise(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (width < 20 || height < 20) {
            return;
        }
        int maxX = x + width - 6;
        int maxY = y + height - 6;
        for (int i = 0; i < 6; i++) {
            int nx = x + 6 + (i * 23 + x) % (maxX - x);
            int ny = y + 6 + (i * 17 + y) % (maxY - y);
            guiGraphics.fill(nx, ny, nx + 1, ny + 1, 0x22FFFFFF);
        }
    }

    @Unique
    private int economySystem$withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Unique
    private void economySystem$renderRoundedBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int radius = height >= 6 ? 2 : 1;
        int innerHeight = Math.max(1, height - 2);
        int left = x + radius;
        int right = x + width - radius;
        if (right > left) {
            guiGraphics.fill(left, y, right, y + height, color);
        }
        guiGraphics.fill(x, y + 1, x + radius, y + 1 + innerHeight, color);
        guiGraphics.fill(x + width - radius, y + 1, x + width, y + 1 + innerHeight, color);
    }

    @Unique
    private static class CustomButton extends Button {
        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered() || isFocused();

            int topColor;
            int bottomColor;
            int borderColor;
            if (hovered) {
                topColor = 0xCC2E7A58;
                bottomColor = 0xCC1C4A35;
                borderColor = 0xFF89E2B5;
            } else {
                topColor = 0xCC255F46;
                bottomColor = 0xCC173A2B;
                borderColor = 0xCC66B78E;
            }

            int x = getX();
            int y = getY();
            int w = width;
            int h = height;

            guiGraphics.fill(x + 2, y, x + w - 2, y + h, topColor);
            guiGraphics.fill(x + 2, y + h, x + w - 2, y + h + 1, bottomColor);

            guiGraphics.fill(x + 1, y, x + 2, y + h, borderColor);
            guiGraphics.fill(x + w - 2, y, x + w - 1, y + h, borderColor);
            guiGraphics.fill(x + 2, y, x + w - 2, y + 1, borderColor);
            guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, borderColor);

            guiGraphics.fill(x, y, x + 1, y + 1, borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + 1, borderColor);
            guiGraphics.fill(x, y + h - 1, x + 1, y + h, borderColor);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, borderColor);

            String displayText = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, 0xFFFFFF, false);
        }
    }
}
