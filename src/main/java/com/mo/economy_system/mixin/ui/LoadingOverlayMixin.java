package com.mo.economy_system.mixin.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * LoadingOverlay Mixin
 * Blue rounded progress bar like world generation screen
 * Injects at RETURN to draw custom UI after original rendering
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin extends Overlay {

    @Unique private static final int ACCENT_BLUE = 0xFF0088FF;
    @Unique private static final int BAR_BACKGROUND = 0x66000000;

    @Unique private static final ResourceLocation ECONOMY_SYSTEM_BACKGROUND =
        ResourceLocation.fromNamespaceAndPath("economy_system", "background.png");

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void economySystem$init(CallbackInfo ci) {
        EconomySystem.LOGGER.info("LoadingOverlayMixin initialized!");
    }

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At("RETURN")
    )
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        long now = Util.getMillis();

        // Draw full-screen background
        RenderSystem.enableBlend();
        UiBackgroundRenderer.renderCover(guiGraphics, ECONOMY_SYSTEM_BACKGROUND, width, height);

        // Update progress
        float actualProgress = this.reload.getActualProgress();
        this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actualProgress * 0.05F, 0.0F, 1.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Progress bar at bottom
        int barMargin = 40;
        int progressBarHeight = 8;
        int progressBarX = barMargin;
        int progressBarWidth = width - barMargin * 2;
        int progressBarY = height - 35;

        // Background bar
        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressBarWidth, progressBarHeight, BAR_BACKGROUND);

        // Progress bar (blue)
        int progressWidth = (int) (this.currentProgress * progressBarWidth);
        if (progressWidth > 0) {
            economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, ACCENT_BLUE);

            // Top highlight line
            if (progressWidth > 2) {
                guiGraphics.fill(progressBarX + 2, progressBarY, progressBarX + progressWidth - 2, progressBarY + 1, 0xFF55AAFF);
            }

            // Pulsing glow effect
            if ((now / 500) % 2 == 0) {
                economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, 0x330055FF);
            }

            // Star-like sparkles
            if (progressWidth > 2) {
                int sparkleCount = Math.min(8, Math.max(4, progressWidth / 40));
                int sparkleY1 = progressBarY + 1;
                int sparkleY2 = progressBarY + progressBarHeight - 1;
                for (int i = 0; i < sparkleCount; i++) {
                    int offset = (int) ((now / 220 + i * 13) % 1000);
                    int sx = progressBarX + (offset * 37 + i * 53) % Math.max(1, progressWidth);
                    int sy = sparkleY1 + (i * 3 + (int) (now / 350)) % Math.max(1, (sparkleY2 - sparkleY1));
                    guiGraphics.fill(sx, sy, sx + 1, sy + 1, 0x33FFFFFF);
                    if ((now / 700 + i) % 2 == 0) {
                        guiGraphics.fill(sx - 1, sy, sx, sy + 1, 0x2200FFFF);
                    }
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Unique
    private void economySystem$renderRoundedBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int radius = height >= 6 ? height / 3 : 1;
        int innerHeight = Math.max(1, height - 2);
        int left = x + radius;
        int right = x + width - radius;
        if (right > left) {
            guiGraphics.fill(left, y, right, y + height, color);
        }
        guiGraphics.fill(x, y + 1, x + radius, y + 1 + innerHeight, color);
        guiGraphics.fill(x + width - radius, y + 1, x + width, y + 1 + innerHeight, color);
    }
}
