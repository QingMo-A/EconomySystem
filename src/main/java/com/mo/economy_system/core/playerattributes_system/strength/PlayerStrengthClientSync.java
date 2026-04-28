package com.mo.economy_system.core.playerattributes_system.strength;

import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class PlayerStrengthClientSync {

    public static void setCurrentStrength(Player player, int currentStrength) {
        if (player == null) return;
        PlayerAttributesData data = ClientCacheManager.getOrCreatePlayerAttributesData(player.getUUID());
        data.setCurrentStrength(Math.max(0, currentStrength));
        ClientCacheManager.setPlayerAttributesData(player.getUUID(), data);
    }

    public static void setCurrentStrength(int currentStrength) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        setCurrentStrength(player, currentStrength);
    }

    public static void setMaxStrength(Player player, int maxStrength) {
        if (player == null) return;
        PlayerAttributesData data = ClientCacheManager.getOrCreatePlayerAttributesData(player.getUUID());
        data.setMaxStrength(Math.max(1, maxStrength));
        ClientCacheManager.setPlayerAttributesData(player.getUUID(), data);
    }

    public static void setMaxStrength(int maxStrength) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        setMaxStrength(player, maxStrength);
    }

    public static int getCurrentStrengthClient(Player player) {
        if (player == null) return 0;
        PlayerAttributesData data = ClientCacheManager.getPlayerAttributesData(player.getUUID());
        return data != null ? data.getCurrentStrength() : 50;
    }

    public static int getMaxStrengthClient(Player player) {
        if (player == null) return 0;
        PlayerAttributesData data = ClientCacheManager.getPlayerAttributesData(player.getUUID());
        return data != null ? data.getMaxStrength() : 100;
    }
}
