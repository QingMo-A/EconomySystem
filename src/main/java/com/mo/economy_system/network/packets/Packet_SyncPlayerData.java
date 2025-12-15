package com.mo.economy_system.network.packets;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.playerlevel.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_SyncPlayerData {
    private final UUID playerUUID;
    private final String rankName;
    private final String titleName;
    private final int level;

    // 服务端专用构造器
    public Packet_SyncPlayerData(ServerPlayer serverPlayer) {
        this.playerUUID = serverPlayer.getUUID();
        this.rankName = PlayerRankManager.getPlayerRankServer(serverPlayer).getRankName();
        this.titleName = PlayerTitleManager.getPlayerTitleServer(serverPlayer).getTitleName();
        this.level = PlayerLevelManager.getPlayerLevelServer(serverPlayer);
    }

    // 解码专用构造器
    public Packet_SyncPlayerData(UUID playerUUID, String rankName, String titleName, int level) {
        this.playerUUID = playerUUID;
        this.rankName = rankName;
        this.titleName = titleName;
        this.level = level;
    }

    // 编码
    public static void encode(Packet_SyncPlayerData packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUUID);
        buf.writeUtf(packet.rankName);
        buf.writeUtf(packet.titleName);
        buf.writeInt(packet.level);
    }

    // 解码
    public static Packet_SyncPlayerData decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        String rankName = buf.readUtf();
        String titleName = buf.readUtf();
        int level = buf.readInt();
        return new Packet_SyncPlayerData(uuid, rankName, titleName, level);
    }

    //处理逻辑，无客户端类引用
    public static void handle(Packet_SyncPlayerData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // 标记已处理（服务器/客户端通用）
        context.setPacketHandled(true);

        // 传递不可变参数，避免lambda捕获导致的类引用
        final UUID safeUUID = packet.playerUUID;
        final String safeRank = packet.rankName;
        final String safeTitle = packet.titleName;
        final int safeLevel = packet.level;

        // 主线程执行（仅分发，无客户端逻辑）
        context.enqueueWork(() -> processOnMainThread(safeUUID, safeRank, safeTitle, safeLevel));
    }

    //分发方法，无客户端类引用
    private static void processOnMainThread(UUID playerUUID, String rankName, String titleName, int level) {
        //用SafeRunnable隔离客户端逻辑，服务器端仅加载接口，不加载实现
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientSyncRunnable(playerUUID, rankName, titleName, level));
    }

    //纯客户端逻辑（@OnlyIn标记，服务器完全不加载）=
    @OnlyIn(Dist.CLIENT)
    private static class ClientSyncRunnable implements DistExecutor.SafeRunnable {
        private final UUID playerUUID;
        private final String rankName;
        private final String titleName;
        private final int level;

        public ClientSyncRunnable(UUID playerUUID, String rankName, String titleName, int level) {
            this.playerUUID = playerUUID;
            this.rankName = rankName;
            this.titleName = titleName;
            this.level = level;
        }

        @Override
        public void run() {
            // 客户端专属逻辑，服务器永远不会执行/加载
            syncPlayerDataOnClient(playerUUID, rankName, titleName, level);
        }
    }

    // 客户端方法
    @OnlyIn(Dist.CLIENT)
    private static void syncPlayerDataOnClient(UUID playerUUID, String rankName, String titleName, int level) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                EconomySystem.LOGGER.error("客户端同步数据失败：Minecraft/Player未加载");
                return;
            }

            Player targetPlayer = mc.level.getPlayerByUUID(playerUUID);
            if (targetPlayer == null) {
                EconomySystem.LOGGER.warn("客户端同步数据失败：未找到UUID为{}的玩家", playerUUID);
                return;
            }

            //客户端缓存更新
            Rank rank = RankRegistry.getRankByName(rankName);
            Title title = TitleRegistry.getTitleByName(titleName);

            PlayerRankManager.setPlayerRankClient(targetPlayer, rank);
            PlayerTitleManager.setPlayerTitleClient(targetPlayer, title);
            PlayerLevelManager.setPlayerLevelClient(targetPlayer, level);

            EconomySystem.LOGGER.info("客户端同步数据成功：Rank={}, Title={}, Level={}",
                    rank.getRankName(), title.getTitleName(), level);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("客户端同步玩家数据失败", e);
        }
    }
}