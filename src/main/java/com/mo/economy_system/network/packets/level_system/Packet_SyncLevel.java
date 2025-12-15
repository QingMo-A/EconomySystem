package com.mo.economy_system.network.packets.level_system;

import com.mo.economy_system.playerlevel.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_SyncLevel {
    private final int entityId; // 玩家实体ID（用于客户端查找实体）
    private final int level;    // 玩家等级

    // 1. 服务端创建数据包时使用（根据Player实例获取数据）
    public Packet_SyncLevel(Player player) {
        this.entityId = player.getId(); // 获取实体ID
        this.level = PlayerLevelManager.getPlayerLevel(player); // 获取等级
    }

    // 2. 解码专用构造方法（仅在decode中使用，设为private）
    private Packet_SyncLevel(int entityId, int level) {
        this.entityId = entityId;
        this.level = level;
    }

    // 编码：写入实体ID和等级（与构造方法参数对应）
    public static void encode(Packet_SyncLevel msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId); // 写入实体ID（int）
        buf.writeInt(msg.level);    // 写入等级（int）
    }

    // 解码：读取实体ID和等级，创建数据包实例
    public static Packet_SyncLevel decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt(); // 读取实体ID（int）
        int level = buf.readInt();    // 读取等级（int）
        return new Packet_SyncLevel(entityId, level); // 调用专用构造方法
    }

    // 客户端处理：更新玩家等级缓存
    public static void handle(Packet_SyncLevel msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端通过实体ID查找玩家
            Player player = (Player) Minecraft.getInstance().level.getEntity(msg.entityId);
            if (player != null) {
                PlayerLevelManager.setClientPlayerLevel(player, msg.level);
                // 若为当前玩家，主动刷新信息栏
                if (player.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
                    ServerInformationDisplay.refreshData();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}