package com.mo.economy_system.mixin.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancel vanilla dirt background on SelectWorldScreen and JoinMultiplayerScreen,
 * since they don't override renderBackground and can't be @Overwrite'd.
 */
@Mixin(Screen.class)
public class VanillaScreenBackgroundMixin {

    private static final String SELECT_WORLD = "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen";
    private static final String JOIN_MULTIPLAYER = "net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen";

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void economySystem$cancelVanillaDirt(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        String name = this.getClass().getName();
        if (SELECT_WORLD.equals(name) || JOIN_MULTIPLAYER.equals(name)) {
            ci.cancel();
        }
    }
}
