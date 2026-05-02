package com.mo.economy_system.client.cinematic;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public final class JoinCinematicClientEvents {
    private JoinCinematicClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        JoinCinematicController.clientTick();
        if (JoinCinematicController.isInputBlocked()) {
            releaseGameplayKeys(Minecraft.getInstance().options);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!JoinCinematicController.isInputBlocked()) {
            return;
        }

        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (JoinCinematicController.isInputBlocked() && minecraft.screen == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (JoinCinematicController.isInputBlocked() && minecraft.screen == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (JoinCinematicController.isInputBlocked()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        float alpha = JoinCinematicController.getBlackOverlayAlpha(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (alpha <= 0.0F) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int color = ((int) (alpha * 255.0F) << 24);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
    }

    private static void releaseGameplayKeys(Options options) {
        release(options.keyUp);
        release(options.keyDown);
        release(options.keyLeft);
        release(options.keyRight);
        release(options.keyJump);
        release(options.keyShift);
        release(options.keySprint);
        release(options.keyAttack);
        release(options.keyUse);
        release(options.keyPickItem);
        release(options.keyDrop);
        release(options.keySwapOffhand);
        release(options.keyInventory);
        release(options.keyTogglePerspective);
        release(options.keySaveHotbarActivator);
        release(options.keyLoadHotbarActivator);
        for (KeyMapping key : options.keyHotbarSlots) {
            release(key);
        }
    }

    private static void release(KeyMapping key) {
        key.setDown(false);
        while (key.consumeClick()) {
            // Drain queued clicks so held keys cannot fire after the cinematic releases control.
        }
    }
}
