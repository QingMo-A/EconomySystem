package com.mo.economy_system.server.rank;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;


/**
 * 玩家Rank数据管理器（使用全局统一存储）
 */
@EventBusSubscriber(modid = EconomySystem.MODID)
public class PlayerRankManager {
    public static void setPlayerRankServer(ServerPlayer serverPlayer, Rank rank) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        PlayerDataManager.updatePlayerData(serverPlayer, rank, playerData.getTitle(), playerData.getLevel(), playerData.getCurrentExperience());
    }
    public static Rank getPlayerRankServer(ServerPlayer serverPlayer) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getRank();
    }

    // 客户端缓存
    public static void setPlayerRankClient(Player clientPlayer, Rank rank) {
        if (clientPlayer == null || rank == null) return;
        PlayerData data = ClientCacheManager.getOrCreatePlayerData(clientPlayer.getUUID());
        data.setRank(rank);
        ClientCacheManager.setPlayerData(clientPlayer.getUUID(), data);
    }

    public static Rank getPlayerRankClient(Player clientPlayer) {
        if (clientPlayer == null) return RankRegistry.NO_RANK;
        PlayerData data = ClientCacheManager.getPlayerData(clientPlayer.getUUID());
        return data != null ? data.getRank() : RankRegistry.NO_RANK;
    }
}