package com.mo.economy_system.network.packets;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 独立的在线玩家数请求包（获取实时在线人数）
 */
public class Packet_OnlinePlayerCountRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_OnlinePlayerCountRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "packet_online_player_count_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_OnlinePlayerCountRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_OnlinePlayerCountRequest.encode(packet, buf), Packet_OnlinePlayerCountRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    // 无参构造（客户端发送请求时无需传参）
    public Packet_OnlinePlayerCountRequest() {}

    // 编码
    public static void encode(Packet_OnlinePlayerCountRequest msg, FriendlyByteBuf buf) {}

    // 解码
    public static Packet_OnlinePlayerCountRequest decode(FriendlyByteBuf buf) {
        return new Packet_OnlinePlayerCountRequest();
    }

    // 服务端处理逻辑（实时获取在线玩家数并返回）
    public static void handle(Packet_OnlinePlayerCountRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取请求的玩家和服务器
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null || player.getServer() == null) return;

            // 实时获取服务端所有在线玩家数量（核心！）
            int onlinePlayerCount = player.getServer().getPlayerList().getPlayers().size();

            // 发送响应包给客户端
            EconomySystem_NetworkManager.sendToClient(
                    player,
                    new Packet_OnlinePlayerCountResponse(onlinePlayerCount)
            );
        });
    }
}
