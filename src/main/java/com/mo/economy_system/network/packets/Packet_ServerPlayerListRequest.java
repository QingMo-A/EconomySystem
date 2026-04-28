package com.mo.economy_system.network.packets;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class Packet_ServerPlayerListRequest {
    public Packet_ServerPlayerListRequest() {}

    public static void encode(Packet_ServerPlayerListRequest msg, FriendlyByteBuf buf) {
        // 无需数据
    }

    public static Packet_ServerPlayerListRequest decode(FriendlyByteBuf buf) {
        return new Packet_ServerPlayerListRequest();
    }

    public static void handle(Packet_ServerPlayerListRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel serverLevel = player.serverLevel();
                if (serverLevel != null) {
                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                    List<Map.Entry<UUID, String>> accounts = Util_Player.getAllPlayerName(data, serverLevel.getServer());
                    // 发送响应包到客户端
                    EconomySystem_NetworkManager.INSTANCE.send(player, new Packet_ServerPlayerListResponse(accounts));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
