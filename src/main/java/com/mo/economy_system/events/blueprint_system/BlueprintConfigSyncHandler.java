package com.mo.economy_system.events.blueprint_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.blueprint_system.BlueprintConfig;
import com.mo.economy_system.core.blueprint_system.BlueprintConfigManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.blueprint_system.Packet_SyncBlueprintConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 蓝图配置同步事件处理器
 * 处理玩家登录时的配置同步
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class BlueprintConfigSyncHandler {

    /**
     * 玩家登录时同步蓝图配置
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            // 服务端逻辑：发送配置到客户端
            ServerPlayer player = (ServerPlayer) event.getEntity();

            try {
                BlueprintConfigManager configManager = BlueprintConfigManager.getInstance();
                BlueprintConfig config = configManager.getCurrentConfig();

                // 获取服务端标识
                String serverIdentifier = configManager.getServerIdentifier();

                // 如果是多人服务器但没有设置标识，使用默认标识
                if (serverIdentifier == null && !player.level().getServer().isSingleplayer()) {
                    serverIdentifier = generateServerIdentifier(player);
                    configManager.setServerIdentifier(serverIdentifier);
                }

                // 发送配置到客户端
                Packet_SyncBlueprintConfig packet = new Packet_SyncBlueprintConfig(config, serverIdentifier);
                EconomySystem_NetworkManager.sendToClient(packet, player);

                EconomySystem.LOGGER.info("已向玩家 {} 发送蓝图配置，服务端标识: {}",
                        player.getScoreboardName(), serverIdentifier);

            } catch (Exception e) {
                EconomySystem.LOGGER.error("同步蓝图配置到客户端失败", e);
            }
        }
    }

    /**
     * 生成服务端标识
     * 基于服务器地址和端口生成唯一标识
     */
    private static String generateServerIdentifier(ServerPlayer player) {
        try {
            String serverAddress = "localhost";

            // 尝试获取服务器地址
            if (player.server != null) {
                // 对于专用服务器，使用配置的地址
                if (!player.server.isSingleplayer()) {
                    // 使用服务器IP作为标识（简化处理）
                    serverAddress = "server_" + System.currentTimeMillis();
                }
            }

            // 清理并返回标识
            return BlueprintConfigManager.getInstance().getServerIdentifier() != null ?
                    BlueprintConfigManager.getInstance().getServerIdentifier() :
                    sanitizeIdentifier(serverAddress);

        } catch (Exception e) {
            EconomySystem.LOGGER.warn("生成服务端标识失败，使用默认值", e);
            return "default_server";
        }
    }

    /**
     * 清理标识中的非法字符
     */
    private static String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "unknown_server";
        }
        return identifier.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 服务器启动时初始化蓝图配置
     */
    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        try {
            EconomySystem.LOGGER.info("初始化蓝图配置管理器...");
            BlueprintConfigManager configManager = BlueprintConfigManager.getInstance();

            // 设置单人模式或多人模式
            if (event.getServer().isSingleplayer()) {
                configManager.setSinglePlayerMode();
                EconomySystem.LOGGER.info("蓝图配置设置为单人模式");
            } else {
                EconomySystem.LOGGER.info("蓝图配置设置为多人模式");
            }

            // 加载配置
            configManager.loadConfig();

            EconomySystem.LOGGER.info("蓝图配置管理器初始化完成");

        } catch (Exception e) {
            EconomySystem.LOGGER.error("初始化蓝图配置管理器失败", e);
        }
    }
}