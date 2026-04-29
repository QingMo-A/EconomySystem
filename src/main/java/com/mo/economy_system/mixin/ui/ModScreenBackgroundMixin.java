package com.mo.economy_system.mixin.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ModScreenBackgroundMixin {
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void economySystem$skipVanillaBackgroundForModScreens(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (((Object) this).getClass().getName().startsWith("com.mo.economy_system.")) {
            ci.cancel();
        }
    }
}
