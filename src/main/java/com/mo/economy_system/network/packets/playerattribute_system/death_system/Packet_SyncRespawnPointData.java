package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.client.cache.ClientCacheManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.mo.economy_system.compat.DistExecutor;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 复活点数同步包（服务端→客户端）
 */
public class Packet_SyncRespawnPointData {
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

    public static void handle(Packet_SyncRespawnPointData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final float safeRespawnPoint = packet.respawnPoint;
        final boolean safeIsInfected = packet.isInfected;

        context.enqueueWork(() -> processOnMainThread(safeRespawnPoint, safeIsInfected));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(float respawnPoint, boolean isInfected) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(respawnPoint, isInfected));
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
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
