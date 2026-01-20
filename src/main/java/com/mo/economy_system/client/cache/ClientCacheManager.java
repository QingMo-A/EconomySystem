package com.mo.economy_system.client.cache;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.server.playerdata.PlayerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientCacheManager {
    private static final Map<UUID, PlayerData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerAttributesData> PLAYER_ATTRIBUTES_DATA_CACHE = new ConcurrentHashMap<>();

    // 额外缓存字段
    private static final Map<UUID, Integer> PLAYER_BALANCE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> EXPLORED_BIOMES_COUNT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> UNLOCKED_RECIPES_COUNT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Territory>> TERRITORIES_CACHE = new ConcurrentHashMap<>();

    // PlayerData
    public static PlayerData getPlayerData(UUID uuid) {
        return PLAYER_DATA_CACHE.get(uuid);
    }

    public static PlayerData getOrCreatePlayerData(UUID uuid) {
        return PLAYER_DATA_CACHE.computeIfAbsent(uuid, k -> new PlayerData());
    }

    public static void setPlayerData(UUID uuid, PlayerData data) {
        if (data != null) {
            PLAYER_DATA_CACHE.put(uuid, data);
        }
    }

    // PlayerAttributesData
    public static PlayerAttributesData getPlayerAttributesData(UUID uuid) {
        return PLAYER_ATTRIBUTES_DATA_CACHE.get(uuid);
    }

    public static PlayerAttributesData getOrCreatePlayerAttributesData(UUID uuid) {
        return PLAYER_ATTRIBUTES_DATA_CACHE.computeIfAbsent(uuid, k -> new PlayerAttributesData());
    }

    public static void setPlayerAttributesData(UUID uuid, PlayerAttributesData data) {
        if (data != null) {
            PLAYER_ATTRIBUTES_DATA_CACHE.put(uuid, data);
        }
    }

    // 金币余额
    public static Integer getPlayerBalance(UUID uuid) {
        return PLAYER_BALANCE_CACHE.getOrDefault(uuid, 0);
    }

    public static void setPlayerBalance(UUID uuid, int balance) {
        PLAYER_BALANCE_CACHE.put(uuid, balance);
    }

    // 已探索群系数量
    public static Integer getExploredBiomesCount(UUID uuid) {
        return EXPLORED_BIOMES_COUNT_CACHE.getOrDefault(uuid, 0);
    }

    public static void setExploredBiomesCount(UUID uuid, int count) {
        EXPLORED_BIOMES_COUNT_CACHE.put(uuid, count);
    }

    // 已解锁蓝图数量
    public static Integer getUnlockedRecipesCount(UUID uuid) {
        return UNLOCKED_RECIPES_COUNT_CACHE.getOrDefault(uuid, 0);
    }

    public static void setUnlockedRecipesCount(UUID uuid, int count) {
        UNLOCKED_RECIPES_COUNT_CACHE.put(uuid, count);
    }

    // 领地列表
    public static List<Territory> getTerritories(UUID uuid) {
        return TERRITORIES_CACHE.getOrDefault(uuid, new ArrayList<>());
    }

    public static void setTerritories(UUID uuid, List<Territory> territories) {
        if (territories != null) {
            TERRITORIES_CACHE.put(uuid, new ArrayList<>(territories));
        }
    }

    // 清理指定UUID的所有缓存
    public static void remove(UUID uuid) {
        PLAYER_DATA_CACHE.remove(uuid);
        PLAYER_ATTRIBUTES_DATA_CACHE.remove(uuid);
        PLAYER_BALANCE_CACHE.remove(uuid);
        EXPLORED_BIOMES_COUNT_CACHE.remove(uuid);
        UNLOCKED_RECIPES_COUNT_CACHE.remove(uuid);
        TERRITORIES_CACHE.remove(uuid);
    }

    // 清空所有缓存
    public static void clear() {
        PLAYER_DATA_CACHE.clear();
        PLAYER_ATTRIBUTES_DATA_CACHE.clear();
        PLAYER_BALANCE_CACHE.clear();
        EXPLORED_BIOMES_COUNT_CACHE.clear();
        UNLOCKED_RECIPES_COUNT_CACHE.clear();
        TERRITORIES_CACHE.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) {
            remove(event.getEntity().getUUID());
        }
    }
}
