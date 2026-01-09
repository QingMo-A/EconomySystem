package com.mo.economy_system.core.auth_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.auth_system.network.ServerAuthHandler;
import com.mo.economy_system.server.serverui.tips.TipPushHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家认证事件处理器
 * 在玩家加入服务器时进行拦截
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class EventHandler_PlayerAuth {

    /**
     * 玩家加入世界时的处理
     * 这在PlayerList.placeNewPlayer之后触发
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel() instanceof ServerLevel level && !level.isClientSide) {
            EconomySystem.LOGGER.info("=== EventHandler_PlayerAuth.onPlayerJoin 被调用 ===");
            EconomySystem.LOGGER.info("玩家: " + player.getName().getString());
            EconomySystem.LOGGER.info("维度: " + level.dimension().location());

            // 获取玩家IP地址
            String playerIp = "unknown";
            try {
                if (player.connection != null && player.connection.connection != null) {
                    String rawAddress = player.connection.connection.getRemoteAddress().toString();
                    EconomySystem.LOGGER.info("原始地址: " + rawAddress);

                    if (rawAddress != null && !rawAddress.isEmpty()) {
                        // 处理地址格式：可能是 /127.0.0.1:12345 或 127.0.0.1:12345
                        // 移除开头的斜杠
                        playerIp = rawAddress.replace("/", "");
                        // 移除端口号
                        if (playerIp.contains(":")) {
                            playerIp = playerIp.substring(0, playerIp.lastIndexOf(":"));
                        }
                        // 处理IPv6地址格式
                        if (playerIp.contains("[") && playerIp.contains("]")) {
                            playerIp = playerIp.substring(playerIp.indexOf("[") + 1, playerIp.indexOf("]"));
                        }
                    }
                } else {
                    EconomySystem.LOGGER.warn("无法获取玩家连接信息");
                }
            } catch (Exception e) {
                EconomySystem.LOGGER.error("获取玩家IP地址失败", e);
                playerIp = "error";
            }

            EconomySystem.LOGGER.info("处理后IP: [" + playerIp + "]");

            // 检查玩家是否已经通过认证
            AuthSavedData data = AuthSavedData.getInstance(level);

            boolean isLoggedIn = data.isLoggedIn(player.getUUID());
            boolean isRegistered = data.isRegistered(player.getUUID());
            EconomySystem.LOGGER.info("已注册: " + isRegistered + ", 已登录: " + isLoggedIn);

            // 如果已经登录，确保是生存模式，不发送认证挑战
            if (isLoggedIn) {
                EconomySystem.LOGGER.info("玩家已登录，确保是生存模式");
                if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR) {
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                }
                // 重要：已登录玩家直接返回，不发送任何认证挑战
                return;
            }

            // 检查是否可以快速登录（同IP且5分钟内退出）
            boolean canQuickLogin = false;
            if (isRegistered) {
                canQuickLogin = data.canQuickLogin(player.getUUID(), playerIp);
                EconomySystem.LOGGER.info("快速登录检查结果: " + canQuickLogin);
            }

            if (canQuickLogin) {
                EconomySystem.LOGGER.info("玩家符合快速登录条件，执行自动登录");

                // 快速登录并立即保存
                data.quickLoginImmediate(player.getUUID(), level);

                // 设置为生存模式
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

                // 发送左上角提示
                TipPushHelper.sendTipToPlayer(player, "§a欢迎回来！已自动登录（5分钟内重连）");

                EconomySystem.LOGGER.info("玩家 " + player.getName().getString() + " 快速登录成功");
                return;
            }

            // 未登录且不符合快速登录条件，发送认证挑战
            EconomySystem.LOGGER.info("玩家未登录，设置为旁观者模式并发送认证挑战");

            // 设置为旁观者模式
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);

            // 发送认证挑战
            ServerAuthHandler.sendAuthChallenge(player);

            // 玩家未登录，发送系统消息
            if (!data.isRegistered(player.getUUID())) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e欢迎来到服务器！请先注册账号。"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你处于旁观者模式，登录后将自动切换到生存模式。"));
            } else {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e欢迎回来！请先登录。"));
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c你处于旁观者模式，登录后将自动切换到生存模式。"));
            }
        }
    }

    /**
     * 监听玩家tick，确保未登录玩家保持旁观者模式
     * 如果玩家已经登录但没有登录状态，重新发送认证挑战
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();
        AuthSavedData data = AuthSavedData.getInstance(level);

        if (!data.isLoggedIn(player.getUUID())) {
            // 玩家未登录，保持旁观者模式
            if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SPECTATOR) {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            }

            // 每隔5秒（100 tick）检查一次是否需要重新发送认证挑战
            if (player.tickCount % 100 == 0 && !ServerAuthHandler.isPendingAuth(player.getUUID())) {
                EconomySystem.LOGGER.info("玩家未登录且没有待认证连接，重新发送认证挑战");
                ServerAuthHandler.sendAuthChallenge(player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e你还未登录！请完成登录后继续游戏。"));
            }
        }
    }

    /**
     * 玩家离开世界时的处理
     * 清除登录状态以支持快速登录，死亡重生时保持登录
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 清除待认证连接和登录状态
            ServerAuthHandler.removePendingConnection(player.getUUID());

            // 记录玩家退出信息（IP地址和退出时间）
            ServerLevel level = player.serverLevel();
            AuthSavedData data = AuthSavedData.getInstance(level);

            // 获取玩家IP地址
            String playerIp = "unknown";
            try {
                if (player.connection != null && player.connection.connection != null) {
                    String rawAddress = player.connection.connection.getRemoteAddress().toString();
                    EconomySystem.LOGGER.info("退出时原始地址: " + rawAddress);

                    if (rawAddress != null && !rawAddress.isEmpty()) {
                        // 处理地址格式
                        playerIp = rawAddress.replace("/", "");
                        if (playerIp.contains(":")) {
                            playerIp = playerIp.substring(0, playerIp.lastIndexOf(":"));
                        }
                        if (playerIp.contains("[") && playerIp.contains("]")) {
                            playerIp = playerIp.substring(playerIp.indexOf("[") + 1, playerIp.indexOf("]"));
                        }
                    }
                }
            } catch (Exception e) {
                EconomySystem.LOGGER.error("获取退出玩家IP地址失败", e);
            }

            EconomySystem.LOGGER.info("退出时处理后IP: [" + playerIp + "]");

            // 清除登录状态以便下次进入时触发快速登录
            data.playerLogout(player.getUUID());

            // 记录退出信息
            data.recordLogoutInfo(player.getUUID(), playerIp);

            // 立即保存数据到磁盘
            try {
                data.setDirty();
                level.save(null, true, false);
                EconomySystem.LOGGER.info("玩家 " + player.getName().getString() + " 退出，已记录IP和退出时间并保存到磁盘");
            } catch (Exception e) {
                EconomySystem.LOGGER.error("保存玩家退出信息失败", e);
            }
        }
    }

    /**
     * 玩家重生时的处理
     * 保持登录状态
     */
    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 玩家重生，保持登录状态不变
            ServerLevel level = player.serverLevel();
            AuthSavedData data = AuthSavedData.getInstance(level);

            // 如果未登录，确保保持旁观者模式
            if (!data.isLoggedIn(player.getUUID())) {
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            }
        }
    }
}
