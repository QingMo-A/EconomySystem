package com.mo.economy_system.mixin.cinematic_system;

import com.mo.economy_system.client.cinematic.JoinCinematicController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraJoinCinematicMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void economySystem$applyJoinCinematicCamera(BlockGetter level, Entity entity, boolean detached,
                                                       boolean thirdPersonReverse, float partialTick,
                                                       CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!JoinCinematicController.isActive() || minecraft.player == null || entity != minecraft.player) {
            return;
        }

        JoinCinematicController.CameraFrame frame = JoinCinematicController.getCameraFrame((LocalPlayer) minecraft.player, partialTick);
        this.setPosition(frame.position());
        this.setRotation(frame.yaw(), frame.pitch());
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void economySystem$showLocalPlayerDuringJoinCinematic(CallbackInfoReturnable<Boolean> cir) {
        if (JoinCinematicController.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
