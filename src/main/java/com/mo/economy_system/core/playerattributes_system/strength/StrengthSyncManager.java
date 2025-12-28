package com.mo.economy_system.core.playerattributes_system.strength;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_SyncStrengthData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

/**
 * 服务端体力同步工具类（主动发送同步包给客户端）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class StrengthSyncManager {
    /**
     * 服务端向指定玩家发送体力同步包（核心方法）
     */
    public static void syncStrengthToClient(ServerPlayer serverPlayer) {
        if (serverPlayer == null) return;

        // 1. 获取服务端真实体力数据（从PlayerData中读取，这是体力的唯一真实来源）
        PlayerAttributesData playerData = PlayerAttributesDataManager.getPlayerAttributesData(serverPlayer.getUUID());
        if (playerData == null) return;
        int currentStrength = playerData.getCurrentStrength();
        int maxStrength = playerData.getMaxStrength();

        // 2. 发送同步包（服务端→客户端）
        EconomySystem_NetworkManager.INSTANCE.sendTo(
                new Packet_SyncStrengthData(currentStrength, maxStrength),
                serverPlayer.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );

        // 可选：打印日志，确认包已发送
        // System.out.println("服务端发送体力同步包：" + currentStrength + "/" + maxStrength);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 同步初始体力给客户端
            StrengthSyncManager.syncStrengthToClient(serverPlayer);
        }
    }
}