package com.mo.economy_system.utils;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class Util_Player {

    /**
     * 根据玩家 UUID 获取玩家名称
     *
     * @param server    Minecraft 服务端对象
     * @param playerUUID 玩家 UUID
     * @return 玩家名称（如果玩家在线）；否则返回 null
     */
    public static String getPlayerNameByUUID(MinecraftServer server, UUID playerUUID) {
        // 获取玩家列表
        PlayerList playerList = server.getPlayerList();

        // 查找玩家
        ServerPlayer player = playerList.getPlayer(playerUUID);
        if (player != null) {
            return player.getName().getString(); // 返回玩家名称
        }

        return null; // 玩家不在线
    }

    public static boolean isOP(Player player) {
        return player.hasPermissions(2) || player.hasPermissions(3) || player.hasPermissions(4);
    }

    public static boolean isOP(ServerPlayer player) {
        return player.hasPermissions(2) || player.hasPermissions(3) || player.hasPermissions(4);
    }

    /**
     * 通过UUID获取玩家名称，支持离线玩家。
     * @param server MinecraftServer 实例
     * @param uuid 玩家 UUID
     * @return 玩家名称
     */
    public static String getPlayerNameFromUUID(MinecraftServer server, UUID uuid) {
        // 获取玩家的 GameProfileCache
        GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
        if (profile != null) {
            return profile.getName(); // 离线玩家通过 GameProfile 获取名称
        }

        // 如果没有找到玩家信息，返回 "Unknown"
        return "Unknown";
    }

    public static List<Map.Entry<UUID, String>> getAllPlayerName(EconomySavedData data, MinecraftServer server) {
        List<Map.Entry<UUID, String>> player = new ArrayList<>();

        // 遍历 list
        for (Map.Entry<UUID, Integer> entry : data.getAllPlayers()) {
            UUID uuid = entry.getKey();  // 获取玩家 UUID

            // 获取玩家的 GameProfile
            GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
            if (profile != null) {
                // 创建 Map.Entry 并添加到列表
                player.add(new AbstractMap.SimpleEntry<>(uuid, profile.getName()));
            }
        }
        return player;
    }
}
