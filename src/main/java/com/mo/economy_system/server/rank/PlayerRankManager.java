package com.mo.economy_system.server.rank;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.LoginSync;
import com.mo.economy_system.server.playerdatasave.PlayerData;
import com.mo.economy_system.server.playerdatasave.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * 玩家Rank数据管理器（使用全局统一存储）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerRankManager {
    public static void setPlayerRankServer(ServerPlayer serverPlayer, Rank rank) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        PlayerDataManager.updatePlayerData(serverPlayer, rank, playerData.getTitle(), playerData.getLevel());
    }
    public static Rank getPlayerRankServer(ServerPlayer serverPlayer) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getRank();
    }

    //客户端缓存
    private static final Map<UUID, Rank> CLIENT_RANK_CACHE = new HashMap<>();

    public static void setPlayerRankClient(Player clientPlayer, Rank rank) {
        if (clientPlayer == null || rank == null) return;
        CLIENT_RANK_CACHE.put(clientPlayer.getUUID(), rank);
        EconomySystem.LOGGER.info("本地rank缓存已保存：{},{}", clientPlayer.getScoreboardName(), rank.getRankName());
    }

    public static Rank getPlayerRankClient(Player clientPlayer) {
        if (clientPlayer == null) return RankRegistry.NO_RANK;
        return CLIENT_RANK_CACHE.getOrDefault(clientPlayer.getUUID(), RankRegistry.NO_RANK);
    }
}