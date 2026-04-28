package com.mo.economy_system.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class Util_Message {
    public static void sendGlobalMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Component chatMessage = Component.literal(message);
            server.getPlayerList().broadcastSystemMessage(chatMessage, false);
        }
    }

    public static void sendDebugMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Component chatMessage = Component.literal("[Debug] " + message);
            server.getPlayerList().broadcastSystemMessage(chatMessage, false);
        }
    }

    // 发送服务器日志消息
    public static void log(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("[Server]: " + message), false);
        }
        System.out.println("[Server]: " + message); // 同时打印到控制台
    }
}
