package com.mo.economy_system.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UiBackgroundRenderer {

    private static final int BG_COUNT = 13;
    private static final ResourceLocation[] BG_TEXTURES = new ResourceLocation[BG_COUNT];
    static {
        for (int i = 0; i < BG_COUNT; i++) {
            BG_TEXTURES[i] = ResourceLocation.fromNamespaceAndPath("economy_system", "background_" + (i + 1) + ".png");
        }
    }

    private static int currentBgIndex = 0;
    private static int prevBgIndex = 0;
    private static long lastBgSwitchTime = 0;
    private static final long BG_SWITCH_INTERVAL = 5000;
    private static final long BG_CROSSFADE_DURATION = 1000;

    private static final Map<ResourceLocation, ImageSize> SIZE_CACHE = new ConcurrentHashMap<>();

    private UiBackgroundRenderer() {
    }

    /** 5秒轮换背景，供所有界面共用 */
    public static void renderCyclingBackground(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        updateCycle();
        renderCover(guiGraphics, BG_TEXTURES[currentBgIndex], screenWidth, screenHeight);
    }

    /**
     * 带渐变的5秒轮换背景，供标题界面使用。
     * 所有界面共享同一个 cycle timer，保证切换同步。
     *
     * @param fadeAlpha 屏幕淡入 alpha（0~1），仅在非渐变阶段应用
     */
    public static void renderCyclingBackgroundCrossfade(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float fadeAlpha) {
        long now = System.currentTimeMillis();
        if (lastBgSwitchTime == 0) {
            lastBgSwitchTime = now;
        }
        long elapsed = now - lastBgSwitchTime;
        if (elapsed >= BG_SWITCH_INTERVAL) {
            prevBgIndex = currentBgIndex;
            currentBgIndex = (currentBgIndex + 1) % BG_COUNT;
            lastBgSwitchTime = now;
            elapsed = 0;
        }

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();

        if (fadeAlpha >= 1.0F && elapsed < BG_CROSSFADE_DURATION && prevBgIndex != currentBgIndex) {
            float t = (float) elapsed / BG_CROSSFADE_DURATION;
            float eased = t * t * (3.0F - 2.0F * t); // smoothstep

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F - eased);
            renderCover(guiGraphics, BG_TEXTURES[prevBgIndex], screenWidth, screenHeight);

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, eased);
            renderCover(guiGraphics, BG_TEXTURES[currentBgIndex], screenWidth, screenHeight);
        } else {
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, fadeAlpha);
            renderCover(guiGraphics, BG_TEXTURES[currentBgIndex], screenWidth, screenHeight);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void updateCycle() {
        long now = System.currentTimeMillis();
        if (lastBgSwitchTime == 0) {
            lastBgSwitchTime = now;
        }
        if (now - lastBgSwitchTime >= BG_SWITCH_INTERVAL) {
            prevBgIndex = currentBgIndex;
            currentBgIndex = (currentBgIndex + 1) % BG_COUNT;
            lastBgSwitchTime = now;
        }
    }

    /** 动态读取贴图真实尺寸并缓存。根据屏幕宽高做 cover 裁切铺满，始终保持原图宽高比。 */
    public static void renderCover(GuiGraphics guiGraphics, ResourceLocation texture, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        ImageSize size = getImageSize(texture);
        if (size.width <= 0 || size.height <= 0) {
            return;
        }

        float scale = Math.max(
            screenWidth / (float) size.width,
            screenHeight / (float) size.height
        );

        int drawWidth = Math.round(size.width * scale);
        int drawHeight = Math.round(size.height * scale);
        int drawX = (screenWidth - drawWidth) / 2;
        int drawY = (screenHeight - drawHeight) / 2;

        guiGraphics.blit(
            texture,
            drawX, drawY, drawWidth, drawHeight,
            0, 0, size.width, size.height,
            size.width, size.height
        );
    }

    private static ImageSize getImageSize(ResourceLocation texture) {
        return SIZE_CACHE.computeIfAbsent(texture, loc -> {
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(loc);
                if (resource.isPresent()) {
                    try (NativeImage image = NativeImage.read(resource.get().open())) {
                        return new ImageSize(image.getWidth(), image.getHeight());
                    }
                }
            } catch (Exception e) {
                EconomySystem.LOGGER.warn("Failed to read dimensions for texture: {}", loc, e);
            }
            return new ImageSize(1920, 1080); // fallback 16:9
        });
    }

    private record ImageSize(int width, int height) {
    }
}
