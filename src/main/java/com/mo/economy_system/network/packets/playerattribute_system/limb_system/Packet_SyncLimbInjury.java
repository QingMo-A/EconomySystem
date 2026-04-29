package com.mo.economy_system.network.packets.playerattribute_system.limb_system;

import com.mo.economy_system.core.playerattributes_system.limb_health_system.LimbClientInjurySync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 肢体受伤同步包（服务端→客户端）
 * 同步玩家受伤部位信息
 */
public class Packet_SyncLimbInjury implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncLimbInjury> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/limb_system/packet_sync_limb_injury"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncLimbInjury> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncLimbInjury.encode(packet, buf), Packet_SyncLimbInjury::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final String limbTypeName;  // 受伤部位名称
    private final long injuryTime;      // 受伤时间戳

    public Packet_SyncLimbInjury(String limbTypeName, long injuryTime) {
        this.limbTypeName = limbTypeName;
        this.injuryTime = injuryTime;
    }

    public static void encode(Packet_SyncLimbInjury packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.limbTypeName);
        buf.writeLong(packet.injuryTime);
    }

    public static Packet_SyncLimbInjury decode(FriendlyByteBuf buf) {
        String limbTypeName = buf.readUtf();
        long injuryTime = buf.readLong();
        return new Packet_SyncLimbInjury(limbTypeName, injuryTime);
    }

    public static void handle(Packet_SyncLimbInjury packet, IPayloadContext context) {
        final String safeLimbTypeName = packet.limbTypeName;
        final long safeInjuryTime = packet.injuryTime;

        context.enqueueWork(() -> processOnMainThread(safeLimbTypeName, safeInjuryTime));
    }

    private static void processOnMainThread(String limbTypeName, long injuryTime) {
        new ClientRunnable(limbTypeName, injuryTime).run();
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements Runnable {
        private final String limbTypeName;
        private final long injuryTime;

        public ClientRunnable(String limbTypeName, long injuryTime) {
            this.limbTypeName = limbTypeName;
            this.injuryTime = injuryTime;
        }

        @Override
        public void run() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;

            // 更新客户端受伤数据
            LimbClientInjurySync.recordInjury(player, limbTypeName, injuryTime);
        }
    }

    public String getLimbTypeName() {
        return limbTypeName;
    }

    public long getInjuryTime() {
        return injuryTime;
    }
}
