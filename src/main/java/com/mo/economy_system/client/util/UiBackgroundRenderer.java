package com.mo.economy_system.client.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class UiBackgroundRenderer {

    private static final int BACKGROUND_TEXTURE_WIDTH = 1536;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 1024;

    private UiBackgroundRenderer() {
    }

    public static void renderCover(GuiGraphics guiGraphics, ResourceLocation texture, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        float scale = Math.max(
            screenWidth / (float) BACKGROUND_TEXTURE_WIDTH,
            screenHeight / (float) BACKGROUND_TEXTURE_HEIGHT
        );

        int drawWidth = Math.round(BACKGROUND_TEXTURE_WIDTH * scale);
        int drawHeight = Math.round(BACKGROUND_TEXTURE_HEIGHT * scale);
        int drawX = (screenWidth - drawWidth) / 2;
        int drawY = (screenHeight - drawHeight) / 2;

        guiGraphics.blit(
            texture,
            drawX, drawY, drawWidth, drawHeight,
            0, 0, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT,
            BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT
        );
    }
}
