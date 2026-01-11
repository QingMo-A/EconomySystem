package com.mo.economy_system.network.packets.playerdata_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI_Screen;
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
    private final String playerName;
    private final boolean isOnline;
    private final String rankName;
    private final String titleName;
    private final int level;
    private final long experience; // 当前等级内的经验
    private final String onlineTime;

    // 服务端专用构造器（在线玩家）
    public Packet_SyncPlayerData(ServerPlayer serverPlayer) {
        this.playerUUID = serverPlayer.getUUID();
        this.playerName = serverPlayer.getScoreboardName();
        this.isOnline = true;
        this.rankName = PlayerRankManager.getPlayerRankServer(serverPlayer).getRankName();
        this.titleName = PlayerTitleManager.getPlayerTitleServer(serverPlayer).getTitleName();
        this.level = PlayerLevelManager.getPlayerLevelServer(serverPlayer);
        this.experience = PlayerLevelManager.getPlayerExperienceServer(serverPlayer);
        this.onlineTime = getPlayerOnlineTime(serverPlayer);
    }

    // 离线玩家构造器
    public Packet_SyncPlayerData(UUID playerUUID, String playerName, String rankName, String titleName, int level, long experience, String onlineTime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.isOnline = false;
        this.rankName = rankName;
        this.titleName = titleName;
        this.level = level;
        this.experience = experience;
        this.onlineTime = onlineTime;
    }

    //服务端获取玩家在线时间
    private static String getPlayerOnlineTime(ServerPlayer player) {
        //本次在线时长（毫秒转XX小时XX分）
        long loginTime = com.mo.economy_system.server.playerdata.PlayerDataManager.getPlayerData(player.getUUID()).getLastLoginTime();
        long onlineMs = System.currentTimeMillis() - loginTime;
        long hours = onlineMs / 3600000;
        long minutes = (onlineMs % 3600000) / 60000;
        return hours + "小时" + minutes + "分";

        // 方式2：最后在线时间（离线玩家用）
        // long lastOnlineMs = player.getLastSeen();
        // return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastOnlineMs), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // 编码
    public static void encode(Packet_SyncPlayerData packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUUID);
        buf.writeUtf(packet.playerName);
        buf.writeBoolean(packet.isOnline);
        buf.writeUtf(packet.rankName);
        buf.writeUtf(packet.titleName);
        buf.writeInt(packet.level);
        buf.writeLong(packet.experience); // 写入经验
        buf.writeUtf(packet.onlineTime);
    }

    // 解码
    public static Packet_SyncPlayerData decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        String playerName = buf.readUtf();
        boolean isOnline = buf.readBoolean();
        String rankName = buf.readUtf();
        String titleName = buf.readUtf();
        int level = buf.readInt();
        long experience = buf.readLong(); // 读取经验
        String onlineTime = buf.readUtf();
        return new Packet_SyncPlayerData(uuid, playerName, rankName, titleName, level, experience, onlineTime);
    }

    //处理逻辑，无客户端类引用
    public static void handle(Packet_SyncPlayerData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // 标记已处理（服务器/客户端通用）
        context.setPacketHandled(true);

        // 传递不可变参数，避免lambda捕获导致的类引用
        final UUID safeUUID = packet.playerUUID;
        final String safePlayerName = packet.playerName;
        final boolean safeIsOnline = packet.isOnline;
        final String safeRank = packet.rankName;
        final String safeTitle = packet.titleName;
        final int safeLevel = packet.level;
        final long safeExperience = packet.experience; // 添加经验
        final String safeOnlineTime = packet.onlineTime;

        // 主线程执行（仅分发，无客户端逻辑）
        context.enqueueWork(() -> processOnMainThread(safeUUID, safePlayerName, safeIsOnline, safeRank, safeTitle, safeLevel, safeExperience, safeOnlineTime));
    }

    //分发方法，无客户端类引用
    private static void processOnMainThread(UUID playerUUID, String playerName, boolean isOnline, String rankName, String titleName, int level, long experience, String onlineTime) {
        //用SafeRunnable隔离客户端逻辑，服务器端仅加载接口，不加载实现
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientSyncRunnable(playerUUID, playerName, isOnline, rankName, titleName, level, experience, onlineTime));
    }

    //纯客户端逻辑（@OnlyIn标记，服务器完全不加载）=
    @OnlyIn(Dist.CLIENT)
    private static class ClientSyncRunnable implements DistExecutor.SafeRunnable {
        private final UUID playerUUID;
        private final String playerName;
        private final boolean isOnline;
        private final String rankName;
        private final String titleName;
        private final int level;
        private final long experience; // 添加经验字段
        private final String onlineTime;

        public ClientSyncRunnable(UUID playerUUID, String playerName, boolean isOnline, String rankName, String titleName, int level, long experience, String onlineTime) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.isOnline = isOnline;
            this.rankName = rankName;
            this.titleName = titleName;
            this.level = level;
            this.experience = experience;
            this.onlineTime = onlineTime;
        }

        @Override
        public void run() {
            syncPlayerDataOnClient(playerUUID, playerName, isOnline, rankName, titleName, level, experience, onlineTime);
        }
    }

    // 客户端方法
    @OnlyIn(Dist.CLIENT)
    private static void syncPlayerDataOnClient(UUID playerUUID, String playerName, boolean isOnline, String rankName, String titleName, int level, long experience, String onlineTime) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                EconomySystem.LOGGER.error("客户端同步数据失败：Minecraft/Player未加载");
                return;
            }

            // TODO: 排行榜功能暂时注释，等待重写
            /*
            if (isOnline) {
                ServerScreenUI_Screen.ONLINE_PLAYER_UUIDS.add(playerUUID);
            } else {
                ServerScreenUI_Screen.ONLINE_PLAYER_UUIDS.remove(playerUUID);
            }
            */

            Player targetPlayer = mc.level.getPlayerByUUID(playerUUID);
            if (targetPlayer == null) {
                EconomySystem.LOGGER.warn("客户端同步数据失败：未找到UUID为{}的玩家", playerUUID);
                // TODO: 排行榜功能暂时注释，等待重写
                // ServerScreenUI_Screen.updatePlayerRankLevelCache(playerUUID, playerName, level, rankName, titleName, onlineTime);
                return;
            }

            //客户端缓存更新
            Rank rank = RankRegistry.getRankByName(rankName);
            Title title = TitleRegistry.getTitleByName(titleName);

            PlayerRankManager.setPlayerRankClient(targetPlayer, rank);
            PlayerTitleManager.setPlayerTitleClient(targetPlayer, title);
            PlayerLevelManager.setPlayerLevelClient(targetPlayer, level);
            PlayerLevelManager.setPlayerExperienceClient(targetPlayer, experience); // 同步经验

            // TODO: 排行榜功能暂时注释，等待重写
            // ServerScreenUI_Screen.updatePlayerRankLevelCache(playerUUID, playerName, level, rankName, titleName, onlineTime);

            EconomySystem.LOGGER.info("客户端同步数据成功：Rank={}, Title={}, Level={}, Exp={}",
                    rank.getRankName(), title.getTitleName(), level, experience);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("客户端同步玩家数据失败", e);
        }
    }
}