package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.client.cache.ClientCacheManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * 复活点数同步包（服务端→客户端）
 */
public class Packet_SyncRespawnPointData implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncRespawnPointData> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/death_system/packet_sync_respawn_point_data"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncRespawnPointData> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncRespawnPointData.encode(packet, buf), Packet_SyncRespawnPointData::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final float respawnPoint;
    private final boolean isInfected;

    public Packet_SyncRespawnPointData(float respawnPoint, boolean isInfected) {
        this.respawnPoint = respawnPoint;
        this.isInfected = isInfected;
    }

    public static void encode(Packet_SyncRespawnPointData packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.respawnPoint);
        buf.writeBoolean(packet.isInfected);
    }

    public static Packet_SyncRespawnPointData decode(FriendlyByteBuf buf) {
        float respawnPoint = buf.readFloat();
        boolean isInfected = buf.readBoolean();
        return new Packet_SyncRespawnPointData(respawnPoint, isInfected);
    }

    public static void handle(Packet_SyncRespawnPointData packet, IPayloadContext context) {
        final float safeRespawnPoint = packet.respawnPoint;
        final boolean safeIsInfected = packet.isInfected;

        context.enqueueWork(() -> processOnMainThread(safeRespawnPoint, safeIsInfected));
    }

    private static void processOnMainThread(float respawnPoint, boolean isInfected) {
        new ClientRunnable(respawnPoint, isInfected).run();
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements Runnable {
        private final float respawnPoint;
        private final boolean isInfected;

        public ClientRunnable(float respawnPoint, boolean isInfected) {
            this.respawnPoint = respawnPoint;
            this.isInfected = isInfected;
        }

        @Override
        public void run() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            UUID uuid = mc.player.getUUID();
            // 更新客户端复活点数缓存
            ClientCacheManager.setRespawnPoint(uuid, respawnPoint);
            ClientCacheManager.setInfected(uuid, isInfected);
        }
    }

    public float getRespawnPoint() {
        return respawnPoint;
    }

    public boolean isInfected() {
        return isInfected;
    }
}
