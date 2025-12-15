package com.mo.economy_system.server.chattitle;

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


@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerTitleManager {
    public static void setPlayerTitleServer(ServerPlayer serverPlayer, Title title) {
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), title, playerData.getLevel());
        LoginSync.broadcastPlayerDataToAllOnlinePlayers(serverPlayer);
        sendSyncPacket(serverPlayer);
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

    private static void sendSyncPacket(ServerPlayer serverPlayer) {
        try {
            Packet_SyncPlayerData packet = new Packet_SyncPlayerData(serverPlayer);
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer), // 指定发送给该玩家
                    packet
            );
            EconomySystem.LOGGER.info("称号同步包已发送给玩家：{}", serverPlayer.getScoreboardName());
        } catch (Exception e) {
            EconomySystem.LOGGER.error("发送称号同步包失败", e);
        }
    }


}