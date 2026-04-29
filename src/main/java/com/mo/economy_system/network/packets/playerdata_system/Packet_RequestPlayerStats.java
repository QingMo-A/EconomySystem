package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 请求玩家统计数据（群系 + 配方）
 */
public class Packet_RequestPlayerStats implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RequestPlayerStats> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerdata_system/packet_request_player_stats"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RequestPlayerStats> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RequestPlayerStats.encode(packet, buf), Packet_RequestPlayerStats::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_RequestPlayerStats() {}

    public static void encode(Packet_RequestPlayerStats msg, FriendlyByteBuf buf) {}

    public static Packet_RequestPlayerStats decode(FriendlyByteBuf buf) {
        return new Packet_RequestPlayerStats();
    }

    public static void handle(Packet_RequestPlayerStats msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                // 发送统计数据到客户端
                Packet_SyncPlayerStats.sendToClient(player);
            }
        });
    }
}
