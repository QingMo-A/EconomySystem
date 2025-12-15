package com.mo.economy_system.playerlevel.overalllevel;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_SyncPlayerData;
import com.mo.economy_system.server.headdisplay.LoginSync;
import com.mo.economy_system.server.playerdatasave.PlayerData;
import com.mo.economy_system.server.playerdatasave.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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
        LoginSync.broadcastPlayerDataToAllOnlinePlayers(serverPlayer);
        sendSyncPacket(serverPlayer);
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

    //发送同步包
    private static void sendSyncPacket(ServerPlayer serverPlayer) {
        try {
            Packet_SyncPlayerData packet = new Packet_SyncPlayerData(serverPlayer);
            // 改用PacketDistributor，避免level/connection私有问题
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    packet
            );
            EconomySystem.LOGGER.info("等级同步包已发送给玩家：{}", serverPlayer.getScoreboardName());
        } catch (Exception e) {
            EconomySystem.LOGGER.error("发送等级同步包失败", e);
        }
    }

}