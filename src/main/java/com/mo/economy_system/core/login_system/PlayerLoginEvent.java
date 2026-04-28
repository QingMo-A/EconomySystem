package com.mo.economy_system.core.login_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.death.RevivalInfoManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginRequest;
import com.mo.economy_system.server.notice.NewPlayerGuide;
import com.mo.economy_system.screen.server_screen.tips.TipPushHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

//整个登录的流程，都在这里
@EventBusSubscriber(modid = EconomySystem.MODID)
public class PlayerLoginEvent {
    //登录注册事件
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogging(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        //单人游戏跳过
        if (player.getServer().isSingleplayer()) {
            return;
        }

        UUID playerUUID = player.getUUID();
        EconomySystem.LOGGER.info("玩家 {} 尝试登录，UUID: {}", player.getName().getString(), playerUUID);

        // 获取玩家当前IP
        String playerIp = player.getIpAddress();

        // 检查是否已注册
        boolean isRegistered = PlayerLoginDataManager.hasLoginData(playerUUID);

        if (!isRegistered) {
            // 玩家没注册过，第一次进服
            EconomySystem.LOGGER.info("玩家 {} 未注册，发送注册请求包 (loginOrRegister=true)", player.getName().getString());
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal("§c您尚未登录或者注册，登录后会将您的游戏模式变成生存模式"));
            // 发送注册请求网络包 (true = 注册)
            EconomySystem_NetworkManager.sendToClient(new Packet_PlayerLoginRequest(true), player);
        } else {
            // 玩家已注册，检查是否可以快速登录
            PlayerLoginData loginData = PlayerLoginDataManager.getLoginData(playerUUID);

            if (loginData != null && loginData.canQuickLogin(playerIp)) {
                // 符合快速登录条件
                EconomySystem.LOGGER.info("玩家 {} 符合快速登录条件，执行自动登录", player.getName().getString());

                // 恢复上次保存的游戏模式
                GameType lastGameMode = loginData.getLastGameMode();
                if (lastGameMode == null) {
                    // 如果没有保存的游戏模式，使用服务器默认
                    lastGameMode = player.getServer().getDefaultGameType();
                }
                player.setGameMode(lastGameMode);
                EconomySystem.LOGGER.info("玩家 {} 快速登录成功，游戏模式恢复为: {}",
                    player.getName().getString(), lastGameMode);

                // 更新登录信息
                loginData.setLastLoginIP(playerIp);
                loginData.setLastLoginTime(String.valueOf(System.currentTimeMillis()));
                PlayerLoginDataManager.saveLoginData(playerUUID, loginData);

                // 发送提示消息
                TipPushHelper.sendTipToPlayer(player, "§a欢迎回来！已自动登录（5分钟内重连）");
                player.sendSystemMessage(Component.literal("§a欢迎回来！已自动登录（5分钟内同IP重连）"));

                // 检查并发送复活提示
                RevivalInfoManager.checkAndSendRevivalTip(player);

                //如果没有完成新手教程，继续新手教程
                if (!loginData.gethasCompletedNewPlayerGuidence()) {
                    NewPlayerGuide.sendNewPlayerGuide(player);
                }

                EconomySystem.LOGGER.info("玩家 {} 快速登录成功", player.getName().getString());
            } else {
                // 不符合快速登录条件，需要手动登录
                EconomySystem.LOGGER.info("玩家 {} 已注册但不符快速登录条件，发送登录请求包 (loginOrRegister=false)", player.getName().getString());
                player.setGameMode(GameType.SPECTATOR);
                // 标记登录验证状态为未完成
                loginData.setLoginSessionCompleted(false);
                PlayerLoginDataManager.saveLoginData(playerUUID, loginData);
                player.sendSystemMessage(Component.literal("§c请在弹出的窗口中完成登录/注册操作"));
                // 发送登录请求网络包 (false = 登录)
                EconomySystem_NetworkManager.sendToClient(new Packet_PlayerLoginRequest(false), player);
            }
        }
    }

    //退出记录事件
    @SubscribeEvent
    public static void onPlayerLoggout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.getServer().isSingleplayer()) {
            return;
        }

        UUID playerUUID = player.getUUID();

        //保存离开游戏前的模式，时间，ip
        PlayerLoginData playerLoginData;
        playerLoginData = PlayerLoginDataManager.getLoginData(playerUUID);
        if (playerLoginData == null) {
            return;
        }

        EconomySystem.LOGGER.info("玩家 {} 退出，记录退出信息", player.getName().getString());

        // 只有当玩家完成了登录验证时，才保存当前游戏模式
        // 这样可以区分"登录验证状态的SPECTATOR"和"玩家真实的SPECTATOR模式"
        if (playerLoginData.isLoginSessionCompleted()) {
            GameType currentGameMode = player.gameMode.getGameModeForPlayer();
            playerLoginData.setGameMode(currentGameMode);
            EconomySystem.LOGGER.info("玩家退出时游戏模式: {}", currentGameMode);
        } else {
            EconomySystem.LOGGER.info("玩家未完成登录验证即退出，不保存游戏模式");
        }

        playerLoginData.setLastLoginIP(player.getIpAddress());
        playerLoginData.setLastLoginTime(String.valueOf(System.currentTimeMillis()));

        // 记录退出时间戳（用于快速登录判断）
        playerLoginData.setLastLogoutTime(System.currentTimeMillis());

        PlayerLoginDataManager.saveLoginData(playerUUID, playerLoginData);

        // 清除该玩家的缓存，下次登录时从文件重新读取
        PlayerLoginDataManager.clearPlayerCache(playerUUID);

        EconomySystem.LOGGER.info("玩家 {} 退出信息已记录：IP={}, 时间={}",
            player.getName().getString(),
            player.getIpAddress(),
            playerLoginData.getLastLoginTime());
    }
}
