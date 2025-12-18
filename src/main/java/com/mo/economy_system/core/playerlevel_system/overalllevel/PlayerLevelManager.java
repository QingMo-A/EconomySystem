package com.mo.economy_system.core.playerlevel_system.overalllevel;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.playerdatasave.PlayerData;
import com.mo.economy_system.server.playerdatasave.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家等级数据
 * */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLevelManager {
    public static void setPlayerLevelServer(ServerPlayer serverPlayer, int level) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), level);
    }
    public static int getPlayerLevelServer(ServerPlayer serverPlayer) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getLevel();
    }

    //客户端缓存
    private static final Map<UUID, Integer> CLIENT_LEVEL_CACHE = new HashMap<>();

    public static void setPlayerLevelClient(Player clientPlayer, int level) {
        if (clientPlayer == null) return;
        CLIENT_LEVEL_CACHE.put(clientPlayer.getUUID(), level);
    }

    public static int getPlayerLevelClient(Player clientPlayer) {
        if (clientPlayer == null) return 0;
        return CLIENT_LEVEL_CACHE.getOrDefault(clientPlayer.getUUID(), 0);
    }
}