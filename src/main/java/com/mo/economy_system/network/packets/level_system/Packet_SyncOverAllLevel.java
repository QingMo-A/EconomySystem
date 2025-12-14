package com.mo.economy_system.network.packets.level_system;

import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步玩家总等级到客户端的数据包
 */
public class Packet_SyncOverAllLevel {
    private final UUID playerUUID;
    private final int overallLevel;

    public Packet_SyncOverAllLevel(UUID playerUUID, int overallLevel) {
        this.playerUUID = playerUUID;
        this.overallLevel = overallLevel;
    }

    // 编码：写入玩家UUID和等级
    public static void encode(Packet_SyncOverAllLevel msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
        buf.writeInt(msg.overallLevel);
    }

    // 解码：读取数据
    public static Packet_SyncOverAllLevel decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int level = buf.readInt();
        return new Packet_SyncOverAllLevel(uuid, level);
    }

    // 客户端逻辑 更新本地缓存
    public static void handle(Packet_SyncOverAllLevel msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 更新当前玩家的总等级缓存（仅同步自己的等级）
            if (ServerInformationDisplay.getCurrentPlayerUUID() != null &&
                    ServerInformationDisplay.getCurrentPlayerUUID().equals(msg.playerUUID)) {
                ServerInformationDisplay.PLAYER_OVERALL_LEVEL = msg.overallLevel;
            }
        });
        context.setPacketHandled(true);
    }
}