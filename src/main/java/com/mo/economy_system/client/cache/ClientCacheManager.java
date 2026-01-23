package com.mo.economy_system.client.cache;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.task_system.TaskPlayerData;
import com.mo.economy_system.core.story_system.StoryStageData;
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

    // 复活点数缓存 (只存储当前本地玩家，UUID为key)
    private static final Map<UUID, Float> RESPAWN_POINT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> INFECTED_CACHE = new ConcurrentHashMap<>();

    // ==================== 任务系统缓存 ====================
    private static Map<Integer, TaskPlayerData> PLAYER_TASK_CACHE = new ConcurrentHashMap<>();
    private static Map<Integer, StoryStageData> STORY_STAGE_CACHE = new ConcurrentHashMap<>();

    // 获取故事阶段列表
    public static Map<Integer, StoryStageData> getStoryStages() {
        return STORY_STAGE_CACHE;
    }

    // 设置故事阶段列表
    public static void setStoryStages(Map<Integer, StoryStageData> stages) {
        STORY_STAGE_CACHE = stages != null ? new ConcurrentHashMap<>(stages) : new ConcurrentHashMap<>();
    }

    // 获取个人任务列表
    public static Map<Integer, TaskPlayerData> getPlayerTasks() {
        return PLAYER_TASK_CACHE;
    }

    // 设置个人任务列表
    public static void setPlayerTasks(Map<Integer, TaskPlayerData> tasks) {
        PLAYER_TASK_CACHE = tasks != null ? new ConcurrentHashMap<>(tasks) : new ConcurrentHashMap<>();
    }

    // 获取单个故事阶段
    public static StoryStageData getStoryStage(int stageId) {
        return STORY_STAGE_CACHE.get(stageId);
    }

    // 获取单个个人任务
    public static TaskPlayerData getPlayerTask(int taskId) {
        return PLAYER_TASK_CACHE.get(taskId);
    }

    // 检查是否有未完成任务
    public static boolean hasUnfinishedTasks() {
        // 检查故事阶段中的任务
        for (StoryStageData stage : STORY_STAGE_CACHE.values()) {
            if (stage != null && stage.getTasks() != null) {
                for (com.mo.economy_system.core.story_system.StoryTaskData task : stage.getTasks()) {
                    if (task != null && !task.isClientPlayerFinished()) {
                        return true;
                    }
                }
            }
        }
        // 检查个人任务
        for (TaskPlayerData task : PLAYER_TASK_CACHE.values()) {
            if (task != null && !task.isClientPlayerFinished()) {
                return true;
            }
        }
        return false;
    }

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

    // ==================== 复活点数缓存 ====================
    /**
     * 获取复活点数
     */
    public static float getRespawnPoint(UUID uuid) {
        return RESPAWN_POINT_CACHE.getOrDefault(uuid, 100.0f);
    }

    /**
     * 设置复活点数
     */
    public static void setRespawnPoint(UUID uuid, float respawnPoint) {
        RESPAWN_POINT_CACHE.put(uuid, respawnPoint);
    }

    /**
     * 获取感染状态
     */
    public static boolean isInfected(UUID uuid) {
        return INFECTED_CACHE.getOrDefault(uuid, false);
    }

    /**
     * 设置感染状态
     */
    public static void setInfected(UUID uuid, boolean infected) {
        INFECTED_CACHE.put(uuid, infected);
    }

    /**
     * 获取正常复活消耗
     */
    public static float getNormalRespawnCost(UUID uuid) {
        return isInfected(uuid) ? 20.0f : 5.0f;
    }

    /**
     * 获取保留物品复活消耗
     */
    public static float getKeepInventoryCost(UUID uuid) {
        return getNormalRespawnCost(uuid) + 30.0f;
    }

    /**
     * 计算重生剩余次数
     */
    public static int getRespawnTimes(UUID uuid) {
        float cost = getNormalRespawnCost(uuid);
        float respawnPoint = getRespawnPoint(uuid);
        return cost > 0 ? (int) (respawnPoint / cost) : 0;
    }

    // 清理指定UUID的所有缓存
    public static void remove(UUID uuid) {
        PLAYER_DATA_CACHE.remove(uuid);
        PLAYER_ATTRIBUTES_DATA_CACHE.remove(uuid);
        PLAYER_BALANCE_CACHE.remove(uuid);
        EXPLORED_BIOMES_COUNT_CACHE.remove(uuid);
        UNLOCKED_RECIPES_COUNT_CACHE.remove(uuid);
        TERRITORIES_CACHE.remove(uuid);
        RESPAWN_POINT_CACHE.remove(uuid);
        INFECTED_CACHE.remove(uuid);
    }

    // 清空所有缓存
    public static void clear() {
        PLAYER_DATA_CACHE.clear();
        PLAYER_ATTRIBUTES_DATA_CACHE.clear();
        PLAYER_BALANCE_CACHE.clear();
        EXPLORED_BIOMES_COUNT_CACHE.clear();
        UNLOCKED_RECIPES_COUNT_CACHE.clear();
        TERRITORIES_CACHE.clear();
        RESPAWN_POINT_CACHE.clear();
        INFECTED_CACHE.clear();
        PLAYER_TASK_CACHE.clear();
        STORY_STAGE_CACHE.clear();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) {
            remove(event.getEntity().getUUID());
        }
    }
}
