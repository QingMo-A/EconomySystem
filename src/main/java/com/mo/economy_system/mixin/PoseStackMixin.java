package com.mo.economy_system.mixin;

import com.mo.economy_system.EconomySystem;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;

@Mixin(PoseStack.class)
public abstract class PoseStackMixin {
    @Shadow
    @Final
    private Deque<?> poseStack;

    @Unique
    private static boolean economySystem$loggedRootPop;

    @Inject(method = "popPose", at = @At("HEAD"), cancellable = true)
    private void economySystem$preventRootPosePop(CallbackInfo ci) {
        if (this.poseStack.size() <= 1) {
            if (!economySystem$loggedRootPop) {
                EconomySystem.LOGGER.warn("Prevented root PoseStack pop; a GUI renderer has an unmatched popPose call.");
                economySystem$loggedRootPop = true;
            }
            ci.cancel();
        }
    }
}
