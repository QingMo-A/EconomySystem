package com.mo.economy_system.network.packets;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import com.mo.economy_system.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 独立的在线玩家数请求包（获取实时在线人数）
 */
public class Packet_OnlinePlayerCountRequest {
    // 无参构造（客户端发送请求时无需传参）
    public Packet_OnlinePlayerCountRequest() {}

    // 编码
    public static void encode(Packet_OnlinePlayerCountRequest msg, FriendlyByteBuf buf) {}

    // 解码
    public static Packet_OnlinePlayerCountRequest decode(FriendlyByteBuf buf) {
        return new Packet_OnlinePlayerCountRequest();
    }

    // 服务端处理逻辑（实时获取在线玩家数并返回）
    public static void handle(Packet_OnlinePlayerCountRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 获取请求的玩家和服务器
            if (context.getSender() == null || context.getSender().getServer() == null) return;

            // 实时获取服务端所有在线玩家数量（核心！）
            int onlinePlayerCount = context.getSender().getServer().getPlayerList().getPlayers().size();

            // 发送响应包给客户端
            EconomySystem_NetworkManager.INSTANCE.send(
                    context.getSender(),
                    new Packet_OnlinePlayerCountResponse(onlinePlayerCount)
            );
        });
        context.setPacketHandled(true);
    }
}