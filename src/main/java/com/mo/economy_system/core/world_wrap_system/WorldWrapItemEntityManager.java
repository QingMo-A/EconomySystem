package com.mo.economy_system.core.world_wrap_system;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

public class WorldWrapItemEntityManager {
    public static void wrapIfNeeded(ItemEntity itemEntity) {
        if (itemEntity.isRemoved() || !(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        if (!config.isEnabled() || !serverLevel.dimension().location().toString().equals(config.getDimension())) {
            return;
        }

        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        double targetX = itemEntity.getX();
        double targetZ = itemEntity.getZ();
        boolean shouldWrap = false;

        if (targetX >= config.getMaxX() || targetX < config.getMinX()) {
            targetX = transformer.wrapX(targetX);
            shouldWrap = true;
        }

        if (targetZ >= config.getMaxZ() || targetZ < config.getMinZ()) {
            targetZ = transformer.wrapZ(targetZ);
            shouldWrap = true;
        }

        if (!shouldWrap) {
            return;
        }

        Vec3 velocity = itemEntity.getDeltaMovement();
        itemEntity.teleportTo(targetX, itemEntity.getY(), targetZ);
        itemEntity.setDeltaMovement(velocity);
        itemEntity.hasImpulse = true;
    }
}
