package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * LevelLoadingScreen Mixin
 * Modern glass-style loading screen.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

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

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

    protected LevelLoadingScreenMixin(Component title) {
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

        int boxWidth = 420;
        int boxHeight = 160;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        economySystem$renderGlassPanel(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0xAAFFFFFF);

        String titleText = "§lLoading world...";
        guiGraphics.drawCenteredString(this.font, titleText, centerX, boxY + 14, TEXT_WHITE);
        guiGraphics.fill(boxX + PADDING, boxY + 32, boxX + boxWidth - PADDING, boxY + 33, ACCENT_BLUE);

        String statusText = getCurrentStatusText();
        guiGraphics.drawCenteredString(this.font, "§7" + statusText, centerX, boxY + 50, TEXT_GRAY);

        int realProgress = Mth.clamp(progressListener.getProgress(), 0, 100);
        int barMargin = 24;
        int progressBarHeight = 6;
        int progressBarX = barMargin;
        int progressBarWidth = virtualSize.virtualWidth - barMargin * 2;
        int progressBarY = virtualSize.virtualHeight - 28;

        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressBarWidth, progressBarHeight, 0x66000000);
        int progressWidth = (int) (realProgress / 100.0f * progressBarWidth);
        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, ACCENT_BLUE);
        if (progressWidth > 2) {
            guiGraphics.fill(progressBarX + 2, progressBarY, progressBarX + progressWidth - 2, progressBarY + 1, 0xFF55AAFF);
        }
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, 0x330055FF);
        }

        // Slow star-like sparkles inside the filled area
        if (progressWidth > 0 && progressBarHeight >= 4) {
            long time = System.currentTimeMillis();
            int sparkleCount = Math.min(8, Math.max(4, progressWidth / 40));
            int sparkleY1 = progressBarY + 1;
            int sparkleY2 = progressBarY + progressBarHeight - 1;
            for (int i = 0; i < sparkleCount; i++) {
                int offset = (int) ((time / 220 + i * 13) % 1000);
                int sx = progressBarX + (offset * 37 + i * 53) % Math.max(1, progressWidth);
                int sy = sparkleY1 + (i * 3 + (int) (time / 350)) % Math.max(1, (sparkleY2 - sparkleY1));
                guiGraphics.fill(sx, sy, sx + 1, sy + 1, 0x33FFFFFF);
                if ((time / 700 + i) % 2 == 0) {
                    guiGraphics.fill(sx - 1, sy, sx, sy + 1, 0x2200FFFF);
                }
            }
        }

        String percentText = "§b" + realProgress + "%";
        guiGraphics.drawCenteredString(this.font, percentText, centerX, progressBarY - 10, TEXT_WHITE);

        guiGraphics.pose().popPose();
    }

    @Unique
    private String getCurrentStatusText() {
        Map<ChunkStatus, Integer> statusCount = new HashMap<>();
        int diameter = progressListener.getDiameter();

        for (int x = 0; x < diameter; x++) {
            for (int z = 0; z < diameter; z++) {
                ChunkStatus status = progressListener.getStatus(x, z);
                if (status != null) {
                    statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
                }
            }
        }

        ChunkStatus mostCommonStatus = null;
        int maxCount = 0;
        for (Map.Entry<ChunkStatus, Integer> entry : statusCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommonStatus = entry.getKey();
            }
        }

        if (mostCommonStatus == null) {
            return "Preparing world...";
        }

        return getStatusDisplayName(mostCommonStatus);
    }

    @Unique
    private String getStatusDisplayName(ChunkStatus status) {
        if (status == ChunkStatus.EMPTY) return "Initializing chunks...";
        if (status == ChunkStatus.STRUCTURE_STARTS) return "Placing structures...";
        if (status == ChunkStatus.STRUCTURE_REFERENCES) return "Linking structures...";
        if (status == ChunkStatus.BIOMES) return "Shaping biomes...";
        if (status == ChunkStatus.NOISE) return "Building terrain...";
        if (status == ChunkStatus.SURFACE) return "Forming surface...";
        if (status == ChunkStatus.CARVERS) return "Carving caves...";
        if (status == ChunkStatus.FEATURES) return "Placing features...";
        if (status == ChunkStatus.INITIALIZE_LIGHT) return "Initializing light...";
        if (status == ChunkStatus.LIGHT) return "Calculating light...";
        if (status == ChunkStatus.SPAWN) return "Spawning entities...";
        if (status == ChunkStatus.FULL) return "World ready.";
        return "Processing: " + status.toString();
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
}
