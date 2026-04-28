package com.mo.economy_system.core.world_wrap_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.world_wrap_system.Packet_SyncWorldWrapConfig;
import com.mo.economy_system.network.packets.world_wrap_system.Packet_WorldWrapVisualState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldWrapManager {
    private static final Map<UUID, Integer> PLAYER_COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PLAYER_BOUNDARY_WARNINGS = new ConcurrentHashMap<>();

    public static void syncConfigTo(ServerPlayer player) {
        EconomySystem_NetworkManager.sendToClient(new Packet_SyncWorldWrapConfig(WorldWrapConfig.getConfig()), player);
    }

    public static void syncConfigToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncConfigTo(player);
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        if (!config.isEnabled() || !isConfiguredDimension(player, config)) {
            updateBoundaryWarningState(player, false);
            return;
        }

        updateBoundaryWarningState(player, isNearBoundary(player, config));

        int currentTick = player.server.getTickCount();
        int cooldownUntil = PLAYER_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        if (currentTick < cooldownUntil) {
            return;
        }

        WrapTarget target = calculateWrapTarget(player, config);
        if (!target.shouldWrap()) {
            return;
        }

        wrapPlayer(player, target, config, currentTick);
    }

    public static void clearPlayer(ServerPlayer player) {
        PLAYER_COOLDOWNS.remove(player.getUUID());
        PLAYER_BOUNDARY_WARNINGS.remove(player.getUUID());
    }

    public static WrapTarget calculateWrapTarget(ServerPlayer player, WorldWrapConfig.WorldWrapConfigData config) {
        double targetX = player.getX();
        double targetZ = player.getZ();
        boolean wrapped = false;
        WorldWrapTransformer transformer = new WorldWrapTransformer(config);

        if (player.getX() >= config.getMaxX() || player.getX() < config.getMinX()) {
            targetX = transformer.wrapX(player.getX());
            wrapped = true;
        }

        if (player.getZ() >= config.getMaxZ() || player.getZ() < config.getMinZ()) {
            targetZ = transformer.wrapZ(player.getZ());
            wrapped = true;
        }

        return new WrapTarget(wrapped, targetX, player.getY(), targetZ);
    }

    public static boolean isConfiguredDimension(ServerPlayer player, WorldWrapConfig.WorldWrapConfigData config) {
        return player.serverLevel().dimension().location().toString().equals(config.getDimension());
    }

    private static void wrapPlayer(ServerPlayer player, WrapTarget target, WorldWrapConfig.WorldWrapConfigData config, int currentTick) {
        ServerLevel level = player.serverLevel();
        BlockPos targetPos = BlockPos.containing(target.x(), target.y(), target.z());
        Vec3 velocity = player.getDeltaMovement();
        float yRot = player.getYRot();
        float xRot = player.getXRot();

        sendVisualState(player, PLAYER_BOUNDARY_WARNINGS.getOrDefault(player.getUUID(), false), true);
        ensureTargetChunksLoaded(level, targetPos, player.getId());
        WorldWrapChunkMirrorManager.clearPlayerChunksOnly(player);
        WorldWrapChunkMirrorManager.prefillTeleportTarget(player, level, targetPos, config);

        try {
            Entity rootVehicle = player.getRootVehicle();
            if (rootVehicle != player) {
                rootVehicle.teleportTo(target.x(), target.y(), target.z());
                rootVehicle.setDeltaMovement(velocity);
                player.teleportTo(level, target.x(), target.y(), target.z(), yRot, xRot);
                if (!player.isPassenger()) {
                    player.startRiding(rootVehicle, true);
                }
            } else {
                player.teleportTo(level, target.x(), target.y(), target.z(), yRot, xRot);
            }
            player.setDeltaMovement(velocity);
            WorldWrapEntityMirrorManager.refreshManualMirrorsAfterTeleport(player);
            PLAYER_COOLDOWNS.put(player.getUUID(), currentTick + config.getCooldownTicks());
        } catch (Exception e) {
            EconomySystem.LOGGER.error("玩家 {} 世界环绕传送失败", player.getName().getString(), e);
        }
    }

    private static void updateBoundaryWarningState(ServerPlayer player, boolean shouldShow) {
        UUID playerId = player.getUUID();
        Boolean currentlyShown = PLAYER_BOUNDARY_WARNINGS.get(playerId);
        if (currentlyShown != null && currentlyShown == shouldShow) {
            return;
        }

        PLAYER_BOUNDARY_WARNINGS.put(playerId, shouldShow);
        sendVisualState(player, shouldShow, false);
    }

    private static boolean isNearBoundary(ServerPlayer player, WorldWrapConfig.WorldWrapConfigData config) {
        double warningDistance = config.getBoundaryWarningDistance();
        if (warningDistance <= 0.0D) {
            return false;
        }

        double x = player.getX();
        double z = player.getZ();
        return x - config.getMinX() <= warningDistance
                || config.getMaxX() - x <= warningDistance
                || z - config.getMinZ() <= warningDistance
                || config.getMaxZ() - z <= warningDistance;
    }

    private static void sendVisualState(ServerPlayer player, boolean showBoundaryWarning, boolean playTransition) {
        EconomySystem_NetworkManager.sendToClient(new Packet_WorldWrapVisualState(showBoundaryWarning, playTransition), player);
    }

    private static void ensureTargetChunksLoaded(ServerLevel level, BlockPos targetPos, int playerId) {
        ChunkPos centerChunk = new ChunkPos(targetPos);
        int preloadRadius = WorldWrapConfig.getConfig().getPreloadChunkRadius();
        for (int chunkX = centerChunk.x - preloadRadius; chunkX <= centerChunk.x + preloadRadius; chunkX++) {
            for (int chunkZ = centerChunk.z - preloadRadius; chunkZ <= centerChunk.z + preloadRadius; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, playerId);
                level.getChunk(chunkX, chunkZ);
            }
        }

        if (!level.isLoaded(targetPos)) {
            level.getChunkSource().addRegionTicket(
                    TicketType.POST_TELEPORT,
                    centerChunk,
                    1,
                    playerId
            );
        }
    }

    public record WrapTarget(boolean shouldWrap, double x, double y, double z) {
    }
}
