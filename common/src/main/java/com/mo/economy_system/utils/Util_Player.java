package com.mo.economy_system.utils;

import com.mojang.authlib.GameProfile;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Player lookup helpers shared by both loader targets. */
public final class Util_Player {
    private Util_Player() {
    }

    /** Returns the online player's name, or {@code null} when they are offline. */
    public static String getPlayerNameByUUID(MinecraftServer server, UUID playerUUID) {
        PlayerList playerList = server.getPlayerList();
        ServerPlayer player = playerList.getPlayer(playerUUID);
        return player == null ? null : player.getName().getString();
    }

    public static boolean isOP(Player player) {
        return player.hasPermissions(2) || player.hasPermissions(3) || player.hasPermissions(4);
    }

    public static boolean isOP(ServerPlayer player) {
        return player.hasPermissions(2) || player.hasPermissions(3) || player.hasPermissions(4);
    }

    /** Resolves cached profiles too, returning {@code Unknown} when none exists. */
    public static String getPlayerNameFromUUID(MinecraftServer server, UUID uuid) {
        GameProfile profile = server.getProfileCache().get(uuid).orElse(null);
        return profile == null ? "Unknown" : profile.getName();
    }

    public static List<Map.Entry<UUID, String>> getAllPlayerName(
            EconomySavedData data,
            MinecraftServer server
    ) {
        List<Map.Entry<UUID, String>> players = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : data.getAllPlayers()) {
            GameProfile profile = server.getProfileCache().get(entry.getKey()).orElse(null);
            if (profile != null) {
                players.add(new AbstractMap.SimpleEntry<>(entry.getKey(), profile.getName()));
            }
        }
        return players;
    }
}
