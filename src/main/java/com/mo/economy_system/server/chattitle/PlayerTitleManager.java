package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerTitleManager {
    public static void setPlayerTitleServer(ServerPlayer serverPlayer, Title title) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), title, playerData.getLevel(), playerData.getCurrentExperience());
    }
    public static Title getPlayerTitleServer(ServerPlayer serverPlayer) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getTitle();
    }

    // 客户端缓存
    public static void setPlayerTitleClient(Player clientPlayer, Title title) {
        if (clientPlayer == null) return;
        PlayerData data = ClientCacheManager.getOrCreatePlayerData(clientPlayer.getUUID());
        data.setTitle(title);
        ClientCacheManager.setPlayerData(clientPlayer.getUUID(), data);
    }

    public static Title getPlayerTitleClient(Player clientPlayer) {
        if (clientPlayer == null) return TitleRegistry.getDefaultTitle();
        PlayerData data = ClientCacheManager.getPlayerData(clientPlayer.getUUID());
        return data != null ? data.getTitle() : TitleRegistry.getDefaultTitle();
    }
}