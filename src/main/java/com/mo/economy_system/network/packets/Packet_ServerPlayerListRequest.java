package com.mo.economy_system.network.packets;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public class Packet_ServerPlayerListRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ServerPlayerListRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "packet_server_player_list_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ServerPlayerListRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ServerPlayerListRequest.encode(packet, buf), Packet_ServerPlayerListRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_ServerPlayerListRequest() {}

    public static void encode(Packet_ServerPlayerListRequest msg, FriendlyByteBuf buf) {
        // 无需数据
    }

    public static Packet_ServerPlayerListRequest decode(FriendlyByteBuf buf) {
        return new Packet_ServerPlayerListRequest();
    }

    public static void handle(Packet_ServerPlayerListRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                ServerLevel serverLevel = player.serverLevel();
                if (serverLevel != null) {
                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                    List<Map.Entry<UUID, String>> accounts = Util_Player.getAllPlayerName(data, serverLevel.getServer());
                    // 发送响应包到客户端
                    EconomySystem_NetworkManager.sendToClient(player, new Packet_ServerPlayerListResponse(accounts));
                }
            }
        });
    }
}
