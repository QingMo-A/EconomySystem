package com.mo.economy_system.core.auth_system.network;

import com.mo.economy_system.core.auth_system.AuthSavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.auth_system.Packet_AuthChallenge;
import com.mo.economy_system.network.packets.auth_system.Packet_AuthResult;
import com.mo.economy_system.server.notice.NewPlayerGuide;
import com.mo.economy_system.server.serverui.tips.TipPushHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端认证处理器
 */
public class ServerAuthHandler {

    // 存储待认证的玩家连接
    private static final Map<UUID, PendingConnection> pendingConnections = new ConcurrentHashMap<>();

    public static void addPendingConnection(ServerPlayer player) {
        pendingConnections.put(player.getUUID(), new PendingConnection(player));
    }

    public static void removePendingConnection(UUID playerUUID) {
        pendingConnections.remove(playerUUID);
    }

    public static boolean isPendingAuth(UUID playerUUID) {
        return pendingConnections.containsKey(playerUUID);
    }

    /**
     * 发送认证挑战给客户端
     */
    public static void sendAuthChallenge(ServerPlayer player) {
        AuthSavedData authData = AuthSavedData.getInstance(player.serverLevel());
        boolean requireRegistration = !authData.isRegistered(player.getUUID());

        // 添加日志
        com.mo.economy_system.EconomySystem.LOGGER.info("发送认证挑战给玩家 " + player.getName().getString() + ", 需要注册: " + requireRegistration);

        Packet_AuthChallenge packet = new Packet_AuthChallenge(requireRegistration);
        EconomySystem_NetworkManager.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            packet
        );

        addPendingConnection(player);
    }

    /**
     * 处理客户端的认证响应
     */
    public static void handleAuthResponse(ServerPlayer player, boolean isLogin, String password) {
        PendingConnection connection = pendingConnections.get(player.getUUID());
        if (connection == null) {
            return; // 不是待认证的连接
        }

        AuthSavedData authData = AuthSavedData.getInstance(player.serverLevel());
        boolean success;
        String message;

        if (isLogin) {
            // 登录
            if (!authData.isRegistered(player.getUUID())) {
                success = false;
                message = "你还没有注册！";
            } else {
                success = authData.loginImmediate(player.getUUID(), password, player.serverLevel());
                message = success ? "登录成功！" : "密码错误！";
            }
        } else {
            // 注册
            if (authData.isRegistered(player.getUUID())) {
                success = false;
                message = "你已经注册过了！";
            } else if (password.length() < 4) {
                success = false;
                message = "密码长度至少需要4个字符！";
            } else {
                success = authData.registerImmediate(player.getUUID(), password, player.serverLevel());
                message = success ? "注册成功！请登录。" : "注册失败！";
            }
        }

        // 发送结果
        Packet_AuthResult resultPacket = new Packet_AuthResult(success, message);
        EconomySystem_NetworkManager.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            resultPacket
        );

        if (success && isLogin) {
            // 认证成功，移除待认证连接
            removePendingConnection(player.getUUID());

            // 设置为生存模式
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);

            // 发送左上角登录成功提示
            TipPushHelper.sendTipToPlayer(player, "§a登录成功！欢迎回来");

            // 检查是否需要播放新手教程
            if (!authData.hasCompletedGuide(player.getUUID())) {
                com.mo.economy_system.EconomySystem.LOGGER.info("玩家 " + player.getName().getString() + " 首次登录，播放新手教程");
                NewPlayerGuide.sendNewPlayerGuide(player);
                authData.markGuideCompletedImmediate(player.getUUID(), player.serverLevel());
            }

            com.mo.economy_system.EconomySystem.LOGGER.info("玩家 " + player.getName().getString() + " 登录成功，切换到生存模式");
        }
    }

    /**
     * 待认证的连接信息
     */
    private static class PendingConnection {
        private final ServerPlayer player;
        private final long startTime;

        public PendingConnection(ServerPlayer player) {
            this.player = player;
            this.startTime = System.currentTimeMillis();
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
