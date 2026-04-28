package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GenericWaitingScreen Mixin
 * Modern glass-style waiting screen without progress bar.
 */
@Mixin(GenericWaitingScreen.class)
public abstract class GenericWaitingScreenMixin extends Screen {

    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int GLASS_TOP = 0x66FFFFFF;
    private static final int GLASS_BOTTOM = 0x33000000;
    private static final int GLASS_BORDER = 0x55FFFFFF;
    private static final int GLASS_SHADOW = 0x33000000;
    private static final int GLASS_HIGHLIGHT = 0x66FFFFFF;
    private static final int GLASS_INNER = 0x22FFFFFF;
    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("economy_system", "background.png");

    protected GenericWaitingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderCustomScreen(guiGraphics);
    }

    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics) {
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        UiBackgroundRenderer.renderCover(guiGraphics, BACKGROUND_TEXTURE, this.width, this.height);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 400;
        int boxHeight = 140;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        economySystem$renderGlassPanel(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0xAAFFFFFF);

        String titleText = this.title != null ? this.title.getString() : "Waiting...";
        if (!titleText.contains("§")) {
            titleText = "§l" + titleText;
        }
        guiGraphics.drawCenteredString(this.font, titleText, centerX, boxY + 16, TEXT_WHITE);
        guiGraphics.fill(boxX + PADDING, boxY + 34, boxX + boxWidth - PADDING, boxY + 35, ACCENT_BLUE);

        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String loadingDots = ".".repeat(dots);
        guiGraphics.drawCenteredString(this.font, "§7Processing" + loadingDots, centerX, boxY + 70, TEXT_GRAY);

        guiGraphics.pose().popPose();
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
}
