package com.mo.economy_system.core.auth_system.network;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.auth_system.Packet_AuthResponse;
import net.minecraft.client.Minecraft;

/**
 * 客户端认证处理器
 */
public class ClientAuthHandler {

    private static boolean awaitingAuth = false;
    private static boolean requireRegistration = false;
    private static com.mo.economy_system.core.auth_system.Screen_AuthLogin authScreen;
    private static String lastPassword = "";  // 保存最后输入的密码，用于注册后自动登录

    /**
     * 处理服务端发送的认证挑战
     */
    public static void handleAuthChallenge(boolean requireRegistration) {
        awaitingAuth = true;
        ClientAuthHandler.requireRegistration = requireRegistration;

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            authScreen = new com.mo.economy_system.core.auth_system.Screen_AuthLogin(requireRegistration);
            minecraft.setScreen(authScreen);
        });
    }

    /**
     * 处理服务端发送的认证结果
     */
    public static void handleAuthResult(boolean success, String message) {
        Minecraft minecraft = Minecraft.getInstance();

        minecraft.execute(() -> {
            if (authScreen != null) {
                if (success) {
                    if (requireRegistration) {
                        // 注册成功，自动使用相同密码登录
                        requireRegistration = false;
                        authScreen.setStatusMessage("§a" + message + " 正在自动登录...", false);
                        // 使用保存的密码自动登录
                        sendLoginRequest(lastPassword);
                    } else {
                        // 登录成功，关闭界面并继续游戏
                        authScreen.setStatusMessage("§a登录成功！", false);
                        awaitingAuth = false;
                        authScreen = null;
                        lastPassword = "";  // 清除密码
                        minecraft.setScreen(null);
                    }
                } else {
                    // 认证失败，显示错误消息
                    authScreen.setStatusMessage("§c" + message, true);
                    // 如果是注册失败，重置状态
                    if (requireRegistration) {
                        lastPassword = "";
                    }
                }
            }
        });
    }

    /**
     * 发送登录请求
     */
    public static void sendLoginRequest(String password) {
        lastPassword = password;  // 保存密码
        Packet_AuthResponse packet = new Packet_AuthResponse(true, password);
        EconomySystem_NetworkManager.INSTANCE.sendToServer(packet);
    }

    /**
     * 发送注册请求
     */
    public static void sendRegisterRequest(String password) {
        lastPassword = password;  // 保存密码
        Packet_AuthResponse packet = new Packet_AuthResponse(false, password);
        EconomySystem_NetworkManager.INSTANCE.sendToServer(packet);
    }

    public static boolean isAwaitingAuth() {
        return awaitingAuth;
    }

    public static void setAwaitingAuth(boolean awaitingAuth) {
        ClientAuthHandler.awaitingAuth = awaitingAuth;
    }
}
