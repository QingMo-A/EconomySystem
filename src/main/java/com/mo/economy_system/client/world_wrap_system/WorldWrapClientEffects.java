package com.mo.economy_system.client.world_wrap_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.server_screen.tips.TipDisplayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class WorldWrapClientEffects {
    private static final String TIP_BOUNDARY_WARNING = "您处于世界边界\n请不要在边界区域丢弃物品，避免导致吞物品\n也不要进行不必要的投掷操作";
    private static final String TIP_WORLD_WRAP_CROSSED = "您已跨越世界边界\n正在稳定当前位置";
    private static final int BOUNDARY_TIP_DURATION = 10000;
    private static final int CROSS_TIP_DURATION = 5000;
    private static final int FULL_BLACK_DURATION_MS = 320;
    private static final int FADE_OUT_DURATION_MS = 900;
    private static final int POST_WRAP_PROXIMITY_SUPPRESS_MS = 1200;
    private static final int MAX_ALPHA = 255;
    private static final int NO_ACTIVE_TRANSITION = -1;
    private static final int RED_GLOW_COLOR = 0xFF3030;
    private static final int RED_GLOW_WIDTH = 8;
    private static final int MIN_RED_GLOW_ALPHA = 24;
    private static final int MAX_RED_GLOW_ALPHA = 110;
    private static final double BOUNDARY_FADE_DISTANCE = 3.0D;

    private static long transitionStartTime = NO_ACTIVE_TRANSITION;
    private static long proximitySuppressUntil = 0L;
    private static boolean boundaryWarningVisible = false;

    public static void handleVisualState(boolean showBoundaryWarning, boolean playTransition) {
        setBoundaryWarningVisible(showBoundaryWarning);
        if (playTransition) {
            playTransition();
        }
    }

    private static void setBoundaryWarningVisible(boolean visible) {
        if (boundaryWarningVisible == visible) {
            return;
        }

        boundaryWarningVisible = visible;
        TipDisplayManager.removeTipContainingText(TIP_BOUNDARY_WARNING);
        if (visible) {
            TipDisplayManager.addMessage(TIP_BOUNDARY_WARNING, BOUNDARY_TIP_DURATION);
        }
    }

    private static void playTransition() {
        transitionStartTime = System.currentTimeMillis();
        proximitySuppressUntil = transitionStartTime + FULL_BLACK_DURATION_MS + FADE_OUT_DURATION_MS + POST_WRAP_PROXIMITY_SUPPRESS_MS;
        TipDisplayManager.addMessage(TIP_WORLD_WRAP_CROSSED, CROSS_TIP_DURATION);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int redGlowAlpha = getBoundaryRedGlowAlpha(minecraft);
        if (redGlowAlpha > 0) {
            renderBoundaryRedGlow(guiGraphics, screenWidth, screenHeight, redGlowAlpha);
        }

        int alpha = getOverlayAlpha(minecraft);
        if (alpha > 0) {
            int color = alpha << 24;
            guiGraphics.fill(RenderType.guiOverlay(), 0, 0, screenWidth, screenHeight, color);
        }
    }

    private static int getOverlayAlpha(Minecraft minecraft) {
        int transitionAlpha = getTransitionOverlayAlpha();
        if (transitionAlpha > 0) {
            return transitionAlpha;
        }

        return getBoundaryProximityAlpha(minecraft);
    }

    private static int getTransitionOverlayAlpha() {
        if (transitionStartTime == NO_ACTIVE_TRANSITION) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - transitionStartTime;
        if (elapsed <= FULL_BLACK_DURATION_MS) {
            return MAX_ALPHA;
        }

        long fadeElapsed = elapsed - FULL_BLACK_DURATION_MS;
        if (fadeElapsed >= FADE_OUT_DURATION_MS) {
            transitionStartTime = NO_ACTIVE_TRANSITION;
            return 0;
        }

        double progress = fadeElapsed / (double) FADE_OUT_DURATION_MS;
        return Math.max(0, Math.min(MAX_ALPHA, (int) Math.round(MAX_ALPHA * (1.0D - progress))));
    }

    private static int getBoundaryProximityAlpha(Minecraft minecraft) {
        if (System.currentTimeMillis() < proximitySuppressUntil
                || minecraft.level == null
                || !ClientWorldWrapData.isEnabled()
                || !minecraft.level.dimension().location().toString().equals(ClientWorldWrapData.getDimension())) {
            return 0;
        }

        double distance = Math.min(
                Math.min(minecraft.player.getX() - ClientWorldWrapData.getMinX(), ClientWorldWrapData.getMaxX() - minecraft.player.getX()),
                Math.min(minecraft.player.getZ() - ClientWorldWrapData.getMinZ(), ClientWorldWrapData.getMaxZ() - minecraft.player.getZ())
        );
        if (distance > BOUNDARY_FADE_DISTANCE) {
            return 0;
        }
        if (distance <= 0.0D) {
            return MAX_ALPHA;
        }

        double progress = 1.0D - distance / BOUNDARY_FADE_DISTANCE;
        double eased = progress * progress * (3.0D - 2.0D * progress);
        return Math.max(0, Math.min(MAX_ALPHA, (int) Math.round(MAX_ALPHA * eased)));
    }

    private static int getBoundaryRedGlowAlpha(Minecraft minecraft) {
        if (!boundaryWarningVisible
                || minecraft.level == null
                || !ClientWorldWrapData.isEnabled()
                || !minecraft.level.dimension().location().toString().equals(ClientWorldWrapData.getDimension())) {
            return 0;
        }

        double warningDistance = Math.max(1.0D, ClientWorldWrapData.getBoundaryWarningDistance());
        double distance = Math.min(
                Math.min(minecraft.player.getX() - ClientWorldWrapData.getMinX(), ClientWorldWrapData.getMaxX() - minecraft.player.getX()),
                Math.min(minecraft.player.getZ() - ClientWorldWrapData.getMinZ(), ClientWorldWrapData.getMaxZ() - minecraft.player.getZ())
        );
        double progress = 1.0D - Math.max(0.0D, Math.min(1.0D, distance / warningDistance));
        int alpha = (int) Math.round(MIN_RED_GLOW_ALPHA + (MAX_RED_GLOW_ALPHA - MIN_RED_GLOW_ALPHA) * progress);
        return Math.max(0, Math.min(MAX_ALPHA, alpha));
    }

    private static void renderBoundaryRedGlow(GuiGraphics guiGraphics, int screenWidth, int screenHeight, int alpha) {
        for (int layer = 0; layer < RED_GLOW_WIDTH; layer++) {
            int layerAlpha = alpha * (RED_GLOW_WIDTH - layer) / RED_GLOW_WIDTH;
            int color = (layerAlpha << 24) | RED_GLOW_COLOR;
            guiGraphics.fill(RenderType.guiOverlay(), 0, layer, screenWidth, layer + 1, color);
            guiGraphics.fill(RenderType.guiOverlay(), 0, screenHeight - layer - 1, screenWidth, screenHeight - layer, color);
            guiGraphics.fill(RenderType.guiOverlay(), layer, 0, layer + 1, screenHeight, color);
            guiGraphics.fill(RenderType.guiOverlay(), screenWidth - layer - 1, 0, screenWidth - layer, screenHeight, color);
        }
    }
}
