package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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

    private static final Map<UUID, Title> CLIENT_TITLE_CACHE = new HashMap<>();

    public static void setPlayerTitleClient(Player clientPlayer, Title title) {
        if (clientPlayer == null) return;
        CLIENT_TITLE_CACHE.put(clientPlayer.getUUID(), title);
    }

    public static Title getPlayerTitleClient(Player clientPlayer) {
        if (clientPlayer == null) return TitleRegistry.getDefaultTitle();
        return CLIENT_TITLE_CACHE.getOrDefault(clientPlayer.getUUID(), TitleRegistry.getDefaultTitle());
    }
}