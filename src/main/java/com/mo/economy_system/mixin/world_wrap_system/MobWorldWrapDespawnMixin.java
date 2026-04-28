package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapEntityMirrorManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobWorldWrapDespawnMixin {
    @Redirect(
            method = "checkDespawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"
            )
    )
    private double economySystem$useWrappedDespawnDistance(Entity nearestPlayer, Entity mob) {
        return WorldWrapEntityMirrorManager.getDespawnDistanceToNearestPlayerSqr(mob, nearestPlayer);
    }
}
