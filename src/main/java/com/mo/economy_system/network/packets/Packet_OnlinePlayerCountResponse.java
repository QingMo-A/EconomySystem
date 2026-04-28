package com.mo.economy_system.network.packets;

import com.mo.economy_system.screen.server_screen.ServerInformationDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 独立的在线玩家数响应包（仅返回数量，轻量无依赖）
 */
public class Packet_OnlinePlayerCountResponse {
    private final int playerCount; // 仅存储在线玩家数量

    // 构造方法：接收服务端传的玩家数
    public Packet_OnlinePlayerCountResponse(int playerCount) {
        this.playerCount = playerCount;
    }

    // 编码：写入玩家数量（仅一个int，高效）
    public static void encode(Packet_OnlinePlayerCountResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerCount);
    }

    // 解码：读取玩家数量
    public static Packet_OnlinePlayerCountResponse decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        return new Packet_OnlinePlayerCountResponse(count);
    }

    // 客户端处理逻辑：直接更新UI的在线玩家数
    public static void handle(Packet_OnlinePlayerCountResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerInformationDisplay.ONLINE_PLAYERS = msg.playerCount;
        });
        context.setPacketHandled(true);
    }
}