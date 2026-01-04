package com.mo.economy_system.server.serverui.tips;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.tip_system.Packet_SendTipToClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collection;

/**
 * Tip 信息推送工具类（支持指定玩家/全服推送）
 */
public class TipPushHelper {
    // 默认 Tip 显示时长（5秒）
    private static final int DEFAULT_DISPLAY_DURATION = 15000;

    /**
     * 给指定玩家推送 Tip 信息（使用默认显示时长）
     * @param targetPlayer 目标玩家（服务端对象）
     * @param tipText Tip 文本内容
     */
    public static void sendTipToPlayer(ServerPlayer targetPlayer, String tipText) {
        sendTipToPlayer(targetPlayer, tipText, DEFAULT_DISPLAY_DURATION);
    }

    /**
     * 给指定玩家推送 Tip 信息（自定义显示时长）
     * @param targetPlayer 目标玩家（服务端对象）
     * @param tipText Tip 文本内容
     * @param displayDuration 显示时长（毫秒）
     */
    public static void sendTipToPlayer(ServerPlayer targetPlayer, String tipText, int displayDuration) {
        if (targetPlayer == null || tipText == null || tipText.isEmpty()) {
            EconomySystem.LOGGER.warn("Tip 推送失败：目标玩家或文本为空");
            return;
        }
        // 构建数据包并发送给指定玩家
        Packet_SendTipToClient tipPacket = new Packet_SendTipToClient(tipText, displayDuration);
        EconomySystem_NetworkManager.INSTANCE.sendTo(tipPacket, targetPlayer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        EconomySystem.LOGGER.info("已给玩家 {} 推送 Tip：{}", targetPlayer.getScoreboardName(), tipText);
    }

    /**
     * 给全服所有在线玩家推送 Tip 信息（使用默认显示时长）
     * @param tipText Tip 文本内容
     */
    public static void broadcastTipToAllPlayers(String tipText) {
        broadcastTipToAllPlayers(tipText, DEFAULT_DISPLAY_DURATION);
    }

    /**
     * 给全服所有在线玩家推送 Tip 信息（自定义显示时长）
     * @param tipText Tip 文本内容
     * @param displayDuration 显示时长（毫秒）
     */
    public static void broadcastTipToAllPlayers(String tipText, int displayDuration) {
        if (tipText == null || tipText.isEmpty()) {
            EconomySystem.LOGGER.warn("全服 Tip 推送失败：文本为空");
            return;
        }
        // 获取服务器实例
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            EconomySystem.LOGGER.error("全服 Tip 推送失败：无法获取服务器实例");
            return;
        }
        // 获取所有在线玩家
        Collection<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
        if (onlinePlayers.isEmpty()) {
            EconomySystem.LOGGER.info("全服 Tip 推送：当前无在线玩家，无需推送");
            return;
        }
        // 构建数据包并批量发送
        Packet_SendTipToClient tipPacket = new Packet_SendTipToClient(tipText, displayDuration);
        for (ServerPlayer player : onlinePlayers) {
            EconomySystem_NetworkManager.INSTANCE.sendTo(tipPacket, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
        EconomySystem.LOGGER.info("已给全服 {} 名玩家推送 Tip：{}", onlinePlayers.size(), tipText);
    }
}