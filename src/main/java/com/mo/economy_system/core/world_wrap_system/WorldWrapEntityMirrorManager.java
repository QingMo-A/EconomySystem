package com.mo.economy_system.core.world_wrap_system;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldWrapEntityMirrorManager {
    private static final int CHUNK_SIZE = 16;
    private static final Map<UUID, Set<Integer>> PLAYER_MANUAL_MIRRORED_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<UUID, EntityMirrorDebugData> PLAYER_DEBUG_DATA = new ConcurrentHashMap<>();

    public static boolean canMirrorEntities(ServerPlayer player) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        return config.isEnabled()
                && config.isClientChunkMirrorEnabled()
                && config.isEntityMirrorEnabled()
                && WorldWrapManager.isConfiguredDimension(player, config)
                && new WorldWrapTransformer(config).isChunkAligned();
    }

    public static double wrappedDistanceToSqr(ServerPlayer player, Entity entity) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        double entityX = transformer.unwrapXFromServerToClient(player.getX(), entity.getX());
        double entityZ = transformer.unwrapZFromServerToClient(player.getZ(), entity.getZ());
        double deltaX = player.getX() - entityX;
        double deltaY = player.getY() - entity.getY();
        double deltaZ = player.getZ() - entityZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public static double getDespawnDistanceToNearestPlayerSqr(Entity entity, Entity vanillaNearestPlayer) {
        if (!(entity.level() instanceof ServerLevel serverLevel) || !canUseWrappedDistance(serverLevel, entity)) {
            return vanillaNearestPlayer.distanceToSqr(entity);
        }

        double nearestDistance = -1.0D;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double distance = wrappedDistanceToSqr(player, entity);
            if (nearestDistance < 0.0D || distance < nearestDistance) {
                nearestDistance = distance;
            }
        }

        return nearestDistance < 0.0D ? vanillaNearestPlayer.distanceToSqr(entity) : nearestDistance;
    }

    public static double unwrapXForPlayer(ServerPlayer player, double serverX) {
        WorldWrapTransformer transformer = new WorldWrapTransformer(WorldWrapConfig.getConfig());
        return transformer.unwrapXFromServerToClient(player.getX(), serverX);
    }

    public static double unwrapZForPlayer(ServerPlayer player, double serverZ) {
        WorldWrapTransformer transformer = new WorldWrapTransformer(WorldWrapConfig.getConfig());
        return transformer.unwrapZFromServerToClient(player.getZ(), serverZ);
    }

    public static boolean isEntityInsideFiniteWorld(Entity entity) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        return transformer.isChunkAligned() && transformer.isChunkInside(entity.chunkPosition());
    }

    private static boolean canUseWrappedDistance(ServerLevel level, Entity entity) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        return config.isEnabled()
                && config.isClientChunkMirrorEnabled()
                && config.isEntityMirrorEnabled()
                && level.dimension().location().toString().equals(config.getDimension())
                && transformer.isChunkAligned()
                && transformer.isChunkInside(entity.chunkPosition());
    }

    public static boolean shouldTrackWrappedEntity(ServerPlayer player, Entity entity, double range) {
        if (!canMirrorEntities(player)
                || entity == player
                || entity.isRemoved()
                || !isEntityInsideFiniteWorld(entity)
                || !entity.broadcastToPlayer(player)) {
            return false;
        }

        return wrappedDistanceToSqr(player, entity) <= range * range;
    }

    public static void syncMirroredEntities(ServerPlayer player, ServerLevel level, Set<Long> realMirrorChunks) {
        if (!canMirrorEntities(player) || realMirrorChunks.isEmpty()) {
            PLAYER_DEBUG_DATA.put(player.getUUID(), EntityMirrorDebugData.inactive(canMirrorEntities(player), realMirrorChunks.size()));
            return;
        }

        EntityMirrorDebugCounter debugCounter = new EntityMirrorDebugCounter(realMirrorChunks.size());
        Set<Integer> desiredEntityIds = new HashSet<>();
        for (long chunkKey : realMirrorChunks) {
            ChunkPos chunkPos = new ChunkPos(chunkKey);
            syncChunkEntities(player, level, chunkPos.x, chunkPos.z, desiredEntityIds, debugCounter);
        }

        unloadStaleManualMirrors(player, desiredEntityIds, debugCounter);
        debugCounter.mirroredEntityCount = desiredEntityIds.size();
        PLAYER_DEBUG_DATA.put(player.getUUID(), debugCounter.toData(true));
    }

    public static void clearPlayer(ServerPlayer player) {
        PLAYER_DEBUG_DATA.remove(player.getUUID());
        unloadStaleManualMirrors(player, Set.of(), new EntityMirrorDebugCounter(0));
    }

    public static void refreshManualMirrorsAfterTeleport(ServerPlayer player) {
        unloadStaleManualMirrors(player, Set.of(), new EntityMirrorDebugCounter(0));
    }

    public static EntityMirrorDebugData getDebugData(ServerPlayer player) {
        return PLAYER_DEBUG_DATA.getOrDefault(player.getUUID(), EntityMirrorDebugData.inactive(canMirrorEntities(player), 0));
    }

    private static void syncChunkEntities(ServerPlayer player, ServerLevel level, int chunkX, int chunkZ,
                                          Set<Integer> desiredEntityIds, EntityMirrorDebugCounter debugCounter) {
        AABB chunkBox = new AABB(
                chunkX * CHUNK_SIZE,
                level.getMinBuildHeight(),
                chunkZ * CHUNK_SIZE,
                (chunkX + 1) * CHUNK_SIZE,
                level.getMaxBuildHeight(),
                (chunkZ + 1) * CHUNK_SIZE
        );
        List<Entity> entities = level.getEntities((Entity) null, chunkBox, entity -> {
            debugCounter.scannedEntityCount++;
            boolean accepted = shouldMirrorEntity(player, entity);
            if (accepted) {
                debugCounter.acceptedEntityCount++;
            }
            return accepted;
        });
        for (Entity entity : entities) {
            desiredEntityIds.add(entity.getId());
            syncManualMirrorEntity(player, entity, debugCounter);
        }
    }

    private static boolean shouldMirrorEntity(ServerPlayer player, Entity entity) {
        int trackingRange = player.server.getScaledTrackingDistance(entity.getType().clientTrackingRange() * CHUNK_SIZE);
        int viewRange = player.server.getPlayerList().getViewDistance() * CHUNK_SIZE;
        double range = Math.min(trackingRange, viewRange);
        return shouldTrackWrappedEntity(player, entity, range);
    }

    private static void syncManualMirrorEntity(ServerPlayer player, Entity entity, EntityMirrorDebugCounter debugCounter) {
        if (isNormallyVisible(player, entity)) {
            return;
        }

        Set<Integer> mirroredEntityIds = PLAYER_MANUAL_MIRRORED_ENTITIES.computeIfAbsent(
                player.getUUID(),
                uuid -> ConcurrentHashMap.newKeySet()
        );
        if (mirroredEntityIds.add(entity.getId())) {
            sendEntitySpawn(player, entity);
            debugCounter.sentSpawnCount++;
        }

        player.connection.send(new ClientboundTeleportEntityPacket(entity));
        debugCounter.sentTeleportCount++;
    }

    private static void sendEntitySpawn(ServerPlayer player, Entity entity) {
        Packet<?> addEntityPacket = new ClientboundAddEntityPacket(entity, 0, entity.blockPosition());
        player.connection.send(addEntityPacket);

        List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> dataValues = entity.getEntityData().getNonDefaultValues();
        if (dataValues != null && !dataValues.isEmpty()) {
            player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), dataValues));
        }

        if (entity instanceof LivingEntity livingEntity) {
            if (!livingEntity.getAttributes().getSyncableAttributes().isEmpty()) {
                player.connection.send(new ClientboundUpdateAttributesPacket(entity.getId(), livingEntity.getAttributes().getSyncableAttributes()));
            }
            List<Pair<EquipmentSlot, ItemStack>> equipmentSlots = collectEquipment(livingEntity);
            if (!equipmentSlots.isEmpty()) {
                player.connection.send(new ClientboundSetEquipmentPacket(entity.getId(), equipmentSlots));
            }
        }
    }

    private static List<Pair<EquipmentSlot, ItemStack>> collectEquipment(LivingEntity entity) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentSlots = new ArrayList<>();
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = entity.getItemBySlot(equipmentSlot);
            if (!itemStack.isEmpty()) {
                equipmentSlots.add(Pair.of(equipmentSlot, itemStack.copy()));
            }
        }
        return equipmentSlots;
    }

    private static void unloadStaleManualMirrors(ServerPlayer player, Set<Integer> desiredEntityIds,
                                                 EntityMirrorDebugCounter debugCounter) {
        Set<Integer> mirroredEntityIds = PLAYER_MANUAL_MIRRORED_ENTITIES.get(player.getUUID());
        if (mirroredEntityIds == null || mirroredEntityIds.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Set<Integer> staleEntityIds = new HashSet<>(mirroredEntityIds);
        staleEntityIds.removeAll(desiredEntityIds);
        for (int entityId : staleEntityIds) {
            Entity entity = level.getEntity(entityId);
            mirroredEntityIds.remove(entityId);
            if (entity != null && isNormallyVisible(player, entity)) {
                player.connection.send(new ClientboundRemoveEntitiesPacket(entityId));
                sendEntitySpawn(player, entity);
                player.connection.send(new ClientboundTeleportEntityPacket(entity));
                debugCounter.sentSpawnCount++;
                debugCounter.sentTeleportCount++;
            } else {
                player.connection.send(new ClientboundRemoveEntitiesPacket(entityId));
                debugCounter.staleRemovedCount++;
            }
        }
    }

    private static boolean isNormallyVisible(ServerPlayer player, Entity entity) {
        if (entity == player || entity.isRemoved() || !entity.broadcastToPlayer(player)) {
            return false;
        }

        int trackingRange = player.server.getScaledTrackingDistance(entity.getType().clientTrackingRange() * CHUNK_SIZE);
        int viewRange = player.server.getPlayerList().getViewDistance() * CHUNK_SIZE;
        double range = Math.min(trackingRange, viewRange);
        double deltaX = player.getX() - entity.getX();
        double deltaZ = player.getZ() - entity.getZ();
        return deltaX * deltaX + deltaZ * deltaZ <= range * range;
    }

    public record EntityMirrorDebugData(
            boolean active,
            int mirrorChunkCount,
            int scannedEntityCount,
            int acceptedEntityCount,
            int mirroredEntityCount,
            int sentSpawnCount,
            int sentTeleportCount,
            int staleRemovedCount
    ) {
        private static EntityMirrorDebugData inactive(boolean active, int mirrorChunkCount) {
            return new EntityMirrorDebugData(active, mirrorChunkCount, 0, 0, 0, 0, 0, 0);
        }
    }

    private static class EntityMirrorDebugCounter {
        private final int mirrorChunkCount;
        private int scannedEntityCount;
        private int acceptedEntityCount;
        private int mirroredEntityCount;
        private int sentSpawnCount;
        private int sentTeleportCount;
        private int staleRemovedCount;

        private EntityMirrorDebugCounter(int mirrorChunkCount) {
            this.mirrorChunkCount = mirrorChunkCount;
        }

        private EntityMirrorDebugData toData(boolean active) {
            return new EntityMirrorDebugData(
                    active,
                    mirrorChunkCount,
                    scannedEntityCount,
                    acceptedEntityCount,
                    mirroredEntityCount,
                    sentSpawnCount,
                    sentTeleportCount,
                    staleRemovedCount
            );
        }
    }
}
