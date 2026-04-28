package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_SyncRespawnPointData;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkDirection;

/**
 * 服务端复活点数同步工具类（主动发送同步包给客户端）
 */
public class RespawnPointSyncManager {

    /**
     * 服务端向指定玩家发送复活点数同步包
     */
    public static void syncRespawnPointToClient(ServerPlayer serverPlayer) {
        if (serverPlayer == null) return;

        // 获取服务端真实复活点数数据
        PlayerAttributesData playerData = PlayerAttributesDataManager.getPlayerAttributesData(serverPlayer.getUUID());
        if (playerData == null) return;

        float respawnPoint = playerData.getRespawnPoint();
        boolean isInfected = playerData.isInfected();

        // 发送同步包（服务端→客户端）
        EconomySystem_NetworkManager.INSTANCE.sendTo(
                new Packet_SyncRespawnPointData(respawnPoint, isInfected),
                serverPlayer,
                NetworkDirection.PLAY_TO_CLIENT
        );

        EconomySystem.LOGGER.debug("已同步玩家 {} 的复活点数: {} (感染者: {})",
                serverPlayer.getScoreboardName(), respawnPoint, isInfected);
    }
}
