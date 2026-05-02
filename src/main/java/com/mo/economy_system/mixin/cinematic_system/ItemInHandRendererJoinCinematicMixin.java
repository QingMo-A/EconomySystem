package com.mo.economy_system.mixin.cinematic_system;

import com.mo.economy_system.client.cinematic.JoinCinematicController;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererJoinCinematicMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void economySystem$hideJoinCinematicHands(float partialTick, PoseStack poseStack,
                                                     MultiBufferSource.BufferSource buffer, LocalPlayer player,
                                                     int combinedLight, CallbackInfo ci) {
        if (JoinCinematicController.isActive()) {
            ci.cancel();
        }
    }
}
