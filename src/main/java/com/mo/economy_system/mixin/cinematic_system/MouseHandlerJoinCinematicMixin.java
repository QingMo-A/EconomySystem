package com.mo.economy_system.mixin.cinematic_system;

import com.mo.economy_system.client.cinematic.JoinCinematicController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerJoinCinematicMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void economySystem$blockJoinCinematicLook(double partialTicks, CallbackInfo ci) {
        if (JoinCinematicController.isInputBlocked()) {
            ci.cancel();
        }
    }
}
