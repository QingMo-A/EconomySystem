package com.mo.economy_system.core.world_wrap_system;

import com.mo.economy_system.mixin.world_wrap_system.ClientboundLevelChunkWithLightPacketAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldWrapChunkMirrorManager {
    private static final Map<UUID, Set<Long>> PLAYER_MIRROR_CHUNKS = new ConcurrentHashMap<>();

    public static void tickPlayer(ServerPlayer player, WorldWrapConfig.WorldWrapConfigData config) {
        if (!config.isEnabled() || !config.isClientChunkMirrorEnabled() || !WorldWrapManager.isConfiguredDimension(player, config)) {
            clearPlayer(player);
            return;
        }

        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        if (!transformer.isChunkAligned()) {
            clearPlayer(player);
            return;
        }

        ServerLevel level = player.serverLevel();
        ChunkPos centerChunk = new ChunkPos(BlockPos.containing(player.getX(), player.getY(), player.getZ()));
        int radius = Math.min(config.getClientChunkMirrorRadius(), player.server.getPlayerList().getViewDistance() + 1);
        int sendBudget = config.getClientChunkMirrorSendBudgetPerTick();
        int sentThisTick = 0;
        Set<Long> desiredChunks = new HashSet<>();
        Set<Long> realMirrorChunks = new HashSet<>();
        List<MirrorChunk> mirrorChunks = collectMirrorChunks(centerChunk, radius, transformer);

        for (MirrorChunk mirrorChunk : mirrorChunks) {
            long fakeChunkKey = ChunkPos.asLong(mirrorChunk.fakeX(), mirrorChunk.fakeZ());
            desiredChunks.add(fakeChunkKey);
            long realChunkKey = ChunkPos.asLong(mirrorChunk.realX(), mirrorChunk.realZ());
            realMirrorChunks.add(realChunkKey);
            keepRealMirrorChunkLoaded(level, mirrorChunk.realX(), mirrorChunk.realZ());
            if (hasMirrorChunk(player, fakeChunkKey)) {
                continue;
            }
            if (sentThisTick >= sendBudget) {
                continue;
            }
            if (sendMirrorChunk(level, player, mirrorChunk)) {
                sentThisTick++;
            }
        }

        unloadStaleChunks(player, desiredChunks);
        WorldWrapEntityMirrorManager.syncMirroredEntities(player, level, realMirrorChunks);
    }

    public static void clearPlayer(ServerPlayer player) {
        clearPlayer(player, true);
    }

    public static void clearPlayerChunksOnly(ServerPlayer player) {
        clearPlayer(player, false);
    }

    private static void clearPlayer(ServerPlayer player, boolean clearEntities) {
        if (clearEntities) {
            WorldWrapEntityMirrorManager.clearPlayer(player);
        }
        Set<Long> sentChunks = PLAYER_MIRROR_CHUNKS.remove(player.getUUID());
        if (sentChunks == null || sentChunks.isEmpty()) {
            return;
        }
        for (long chunkKey : sentChunks) {
            ChunkPos chunkPos = new ChunkPos(chunkKey);
            player.connection.send(new ClientboundForgetLevelChunkPacket(new net.minecraft.world.level.ChunkPos(chunkPos.x, chunkPos.z)));
        }
    }

    public static boolean isMirroredChunk(ServerPlayer player, int chunkX, int chunkZ) {
        Set<Long> sentChunks = PLAYER_MIRROR_CHUNKS.get(player.getUUID());
        return sentChunks != null && sentChunks.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public static boolean shouldBlockVanillaChunk(ServerPlayer player, ChunkPos chunkPos) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        if (!config.isEnabled() || !config.isClientChunkMirrorEnabled() || !WorldWrapManager.isConfiguredDimension(player, config)) {
            return false;
        }

        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        return transformer.isChunkAligned() && !transformer.isChunkInside(chunkPos);
    }

    public static boolean shouldProtectMirrorForget(ServerPlayer player, ChunkPos chunkPos) {
        return shouldBlockVanillaChunk(player, chunkPos) && isMirroredChunk(player, chunkPos.x, chunkPos.z);
    }

    public static void prefillTeleportTarget(ServerPlayer player, ServerLevel level, BlockPos targetPos,
                                             WorldWrapConfig.WorldWrapConfigData config) {
        if (!config.isEnabled() || !config.isClientChunkMirrorEnabled() || !WorldWrapManager.isConfiguredDimension(player, config)) {
            return;
        }

        WorldWrapTransformer transformer = new WorldWrapTransformer(config);
        if (!transformer.isChunkAligned()) {
            return;
        }

        int radius = Math.min(config.getTeleportPrefillRadius(), player.server.getPlayerList().getViewDistance());
        int sendLimit = config.getTeleportPrefillSendLimit();
        if (radius <= 0 || sendLimit <= 0) {
            return;
        }

        ChunkPos centerChunk = new ChunkPos(targetPos);
        List<PrefillChunk> prefillChunks = collectPrefillChunks(centerChunk, radius, transformer);
        player.connection.send(new ClientboundSetChunkCacheCenterPacket(centerChunk.x, centerChunk.z));

        int sent = 0;
        for (PrefillChunk prefillChunk : prefillChunks) {
            if (sent >= sendLimit) {
                return;
            }
            if (prefillChunk.mirrorChunk() != null) {
                if (sendMirrorChunk(level, player, prefillChunk.mirrorChunk())) {
                    sent++;
                }
            } else if (sendRealChunk(level, player, prefillChunk.chunkX(), prefillChunk.chunkZ())) {
                sent++;
            }
        }
    }

    private static List<MirrorChunk> collectMirrorChunks(ChunkPos centerChunk, int radius, WorldWrapTransformer transformer) {
        List<MirrorChunk> mirrorChunks = new ArrayList<>();
        for (int chunkX = centerChunk.x - radius; chunkX <= centerChunk.x + radius; chunkX++) {
            for (int chunkZ = centerChunk.z - radius; chunkZ <= centerChunk.z + radius; chunkZ++) {
                if (!isChunkInRange(chunkX, chunkZ, centerChunk, radius)) {
                    continue;
                }

                MirrorChunk mirrorChunk = resolveMirrorChunk(chunkX, chunkZ, transformer);
                if (mirrorChunk != null) {
                    mirrorChunks.add(mirrorChunk);
                }
            }
        }
        mirrorChunks.sort(Comparator.comparingInt(mirrorChunk ->
                chunkDistanceSquare(mirrorChunk.fakeX(), mirrorChunk.fakeZ(), centerChunk)));
        return mirrorChunks;
    }

    private static List<PrefillChunk> collectPrefillChunks(ChunkPos centerChunk, int radius, WorldWrapTransformer transformer) {
        List<PrefillChunk> prefillChunks = new ArrayList<>();
        for (int chunkX = centerChunk.x - radius; chunkX <= centerChunk.x + radius; chunkX++) {
            for (int chunkZ = centerChunk.z - radius; chunkZ <= centerChunk.z + radius; chunkZ++) {
                if (!isChunkInRange(chunkX, chunkZ, centerChunk, radius)) {
                    continue;
                }

                MirrorChunk mirrorChunk = resolveMirrorChunk(chunkX, chunkZ, transformer);
                prefillChunks.add(new PrefillChunk(chunkX, chunkZ, mirrorChunk));
            }
        }
        prefillChunks.sort(Comparator.comparingInt(prefillChunk ->
                chunkDistanceSquare(prefillChunk.chunkX(), prefillChunk.chunkZ(), centerChunk)));
        return prefillChunks;
    }

    private static MirrorChunk resolveMirrorChunk(int fakeChunkX, int fakeChunkZ, WorldWrapTransformer transformer) {
        if (transformer.isChunkInside(fakeChunkX, fakeChunkZ)) {
            return null;
        }

        int realChunkX = transformer.wrapChunkX(fakeChunkX);
        int realChunkZ = transformer.wrapChunkZ(fakeChunkZ);

        return new MirrorChunk(fakeChunkX, fakeChunkZ, realChunkX, realChunkZ);
    }

    private static boolean hasMirrorChunk(ServerPlayer player, long fakeChunkKey) {
        Set<Long> sentChunks = PLAYER_MIRROR_CHUNKS.get(player.getUUID());
        return sentChunks != null && sentChunks.contains(fakeChunkKey);
    }

    private static boolean sendMirrorChunk(ServerLevel level, ServerPlayer player, MirrorChunk mirrorChunk) {
        long fakeChunkKey = ChunkPos.asLong(mirrorChunk.fakeX(), mirrorChunk.fakeZ());
        Set<Long> sentChunks = PLAYER_MIRROR_CHUNKS.computeIfAbsent(player.getUUID(), uuid -> ConcurrentHashMap.newKeySet());
        if (!sentChunks.add(fakeChunkKey)) {
            return false;
        }

        player.connection.send(new ClientboundForgetLevelChunkPacket(new net.minecraft.world.level.ChunkPos(mirrorChunk.fakeX(), mirrorChunk.fakeZ())));

        LevelChunk realChunk = level.getChunk(mirrorChunk.realX(), mirrorChunk.realZ());
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                realChunk,
                level.getChunkSource().getLightEngine(),
                null,
                null
        );
        ClientboundLevelChunkWithLightPacketAccessor accessor = (ClientboundLevelChunkWithLightPacketAccessor) packet;
        accessor.economySystem$setX(mirrorChunk.fakeX());
        accessor.economySystem$setZ(mirrorChunk.fakeZ());
        player.connection.send(packet);
        return true;
    }

    private static void keepRealMirrorChunkLoaded(ServerLevel level, int chunkX, int chunkZ) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        level.getChunkSource().addRegionTicket(TicketType.UNKNOWN, chunkPos, 2, chunkPos);
        level.getChunk(chunkX, chunkZ);
    }

    private static boolean sendRealChunk(ServerLevel level, ServerPlayer player, int chunkX, int chunkZ) {
        LevelChunk realChunk = level.getChunk(chunkX, chunkZ);
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                realChunk,
                level.getChunkSource().getLightEngine(),
                null,
                null
        );
        player.connection.send(packet);
        return true;
    }

    private static void unloadStaleChunks(ServerPlayer player, Set<Long> desiredChunks) {
        Set<Long> sentChunks = PLAYER_MIRROR_CHUNKS.get(player.getUUID());
        if (sentChunks == null || sentChunks.isEmpty()) {
            return;
        }

        Set<Long> staleChunks = new HashSet<>(sentChunks);
        staleChunks.removeAll(desiredChunks);
        for (long chunkKey : staleChunks) {
            ChunkPos chunkPos = new ChunkPos(chunkKey);
            sentChunks.remove(chunkKey);
            player.connection.send(new ClientboundForgetLevelChunkPacket(new net.minecraft.world.level.ChunkPos(chunkPos.x, chunkPos.z)));
        }
    }

    private static boolean isChunkInRange(int chunkX, int chunkZ, ChunkPos centerChunk, int radius) {
        int deltaX = Math.max(0, Math.abs(chunkX - centerChunk.x) - 1);
        int deltaZ = Math.max(0, Math.abs(chunkZ - centerChunk.z) - 1);
        long cornerDistance = (long) Math.min(deltaX, deltaZ);
        long edgeDistance = (long) Math.max(0, Math.max(deltaX, deltaZ) - 1);
        return cornerDistance * cornerDistance + edgeDistance * edgeDistance < (long) radius * radius;
    }

    private static int chunkDistanceSquare(int chunkX, int chunkZ, ChunkPos centerChunk) {
        int deltaX = chunkX - centerChunk.x;
        int deltaZ = chunkZ - centerChunk.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private record PrefillChunk(int chunkX, int chunkZ, MirrorChunk mirrorChunk) {
    }

    private record MirrorChunk(int fakeX, int fakeZ, int realX, int realZ) {
    }
}
