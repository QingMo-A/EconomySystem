package com.mo.economy_system.server.rank;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_SyncPlayerData;
import com.mo.economy_system.server.headdisplay.LoginSync;
import com.mo.economy_system.server.playerdatasave.PlayerData;
import com.mo.economy_system.server.playerdatasave.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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
        LoginSync.broadcastPlayerDataToAllOnlinePlayers(serverPlayer);
        sendSyncPacket(serverPlayer);
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

    //发送同步包
    private static void sendSyncPacket(ServerPlayer serverPlayer) {
        try {
            Packet_SyncPlayerData packet = new Packet_SyncPlayerData(serverPlayer);
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    packet
            );
            EconomySystem.LOGGER.info("Rank同步包已发送给玩家：{}", serverPlayer.getScoreboardName());
        } catch (Exception e) {
            EconomySystem.LOGGER.error("发送Rank同步包失败", e);
        }
    }

    //玩家登录时同步
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            sendSyncPacket(serverPlayer);
        }
    }
}