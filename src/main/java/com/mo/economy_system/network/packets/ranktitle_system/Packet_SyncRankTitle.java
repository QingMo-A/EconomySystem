// network/packets/Packet_SyncRankTitle.java
package com.mo.economy_system.network.packets.ranktitle_system;

import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_SyncRankTitle {
    private final int entityId;      // 玩家的 entityId（用于客户端查找实体）
    private final String rankId;
    private final int rankLevel;
    private final int titleId;
    private final String titleName;

    public Packet_SyncRankTitle(Player player) {
        this.entityId = player.getId();
        Rank rank = PlayerRankManager.getPlayerRank(player);
        Title title = PlayerTitleManager.getPlayerTitle(player);

        this.rankId = rank.getRankName();
        this.rankLevel = rank.getRankLevel();
        this.titleId = title.getTitleID();
        this.titleName = title.getTitleName();
    }

    // 构造器（从网络读取）
    private Packet_SyncRankTitle(int entityId, String rankId, int rankLevel, int titleId, String titleName) {
        this.entityId = entityId;
        this.rankId = rankId;
        this.rankLevel = rankLevel;
        this.titleId = titleId;
        this.titleName = titleName;
    }

    public static void encode(Packet_SyncRankTitle msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.rankId);
        buf.writeInt(msg.rankLevel);
        buf.writeInt(msg.titleId);
        buf.writeUtf(msg.titleName);
    }

    public static Packet_SyncRankTitle decode(FriendlyByteBuf buf) {
        return new Packet_SyncRankTitle(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(Packet_SyncRankTitle msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = (Player) Minecraft.getInstance().level.getEntity(msg.entityId);
            if (player != null) {
                // 从RankRegistry获取规范的Rank实例（避免创建新对象）
                Rank rank = RankRegistry.getRankByName(msg.rankId);
                Title title = new Title(msg.titleId, msg.titleName);

                // 更新客户端Rank缓存（核心修改）
                PlayerRankManager.setClientPlayerRank(player, rank);
                PlayerTitleManager.setClientPlayerTitle(player, title); // 新增这行
            }
        });
        ctx.get().setPacketHandled(true);
    }
}