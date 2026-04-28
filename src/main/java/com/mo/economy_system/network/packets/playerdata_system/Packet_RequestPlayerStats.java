package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 请求玩家统计数据（群系 + 配方）
 */
public class Packet_RequestPlayerStats {
    public Packet_RequestPlayerStats() {}

    public static void encode(Packet_RequestPlayerStats msg, FriendlyByteBuf buf) {}

    public static Packet_RequestPlayerStats decode(FriendlyByteBuf buf) {
        return new Packet_RequestPlayerStats();
    }

    public static void handle(Packet_RequestPlayerStats msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 发送统计数据到客户端
                Packet_SyncPlayerStats.sendToClient(player);
            }
        });
        context.setPacketHandled(true);
    }
}
