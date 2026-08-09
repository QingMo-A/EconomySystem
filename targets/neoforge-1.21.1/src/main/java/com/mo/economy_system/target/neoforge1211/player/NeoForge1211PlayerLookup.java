package com.mo.economy_system.target.neoforge1211.player;

import com.mojang.authlib.GameProfile;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** NeoForge-native player and profile lookup operations. */
public final class NeoForge1211PlayerLookup {
    private NeoForge1211PlayerLookup() {
    }

    public static String profileName(MinecraftServer server, UUID playerId) {
        GameProfile profile = server.getProfileCache().get(playerId).orElse(null);
        return profile == null ? "Unknown" : profile.getName();
    }

    public static boolean isOperator(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    public static List<Map.Entry<UUID, String>> knownPlayers(
            EconomySavedData data,
            MinecraftServer server
    ) {
        List<Map.Entry<UUID, String>> players = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : data.getAllPlayers()) {
            GameProfile profile = server.getProfileCache().get(entry.getKey()).orElse(null);
            if (profile != null) {
                players.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), profile.getName()));
            }
        }
        return players;
    }
}
