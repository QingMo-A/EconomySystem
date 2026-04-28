package com.mo.economy_system.core.login_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.login_system.Packet_PlayerLoginResponse;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 客户端登录处理器
 * 处理登录界面的网络请求发送
 */
@OnlyIn(Dist.CLIENT)
public class ClientLoginHandler {
    /**
     * 发送注册请求
     */
    public static void sendRegisterRequest(String password) {
        EconomySystem.LOGGER.info("准备发送注册请求，密码长度: {}", password.length());

        if (Minecraft.getInstance().player == null) {
            EconomySystem.LOGGER.error("玩家实例为空，无法发送注册请求");
            return;
        }

        EconomySystem.LOGGER.info("玩家实例不为空，UUID: {}", Minecraft.getInstance().player.getUUID());

        Packet_PlayerLoginResponse packet = new Packet_PlayerLoginResponse(
            true,  // true = 注册
            password,
            Minecraft.getInstance().player.getUUID()
        );

        EconomySystem_NetworkManager.INSTANCE.send(
            null,
            packet
        );

        EconomySystem.LOGGER.info("注册请求包已发送");
    }

    /**
     * 发送登录请求
     */
    public static void sendLoginRequest(String password) {
        EconomySystem.LOGGER.info("准备发送登录请求，密码长度: {}", password.length());

        if (Minecraft.getInstance().player == null) {
            EconomySystem.LOGGER.error("玩家实例为空，无法发送登录请求");
            return;
        }

        EconomySystem.LOGGER.info("玩家实例不为空，UUID: {}", Minecraft.getInstance().player.getUUID());

        Packet_PlayerLoginResponse packet = new Packet_PlayerLoginResponse(
            false,  // false = 登录
            password,
            Minecraft.getInstance().player.getUUID()
        );

        EconomySystem_NetworkManager.INSTANCE.send(
            null,
            packet
        );

        EconomySystem.LOGGER.info("登录请求包已发送");
    }
}
