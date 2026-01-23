package com.mo.economy_system.core.story_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事阶段管理器
 * 负责加载和管理故事阶段数据（包含任务列表和怪物数值调整）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StoryStageManager {
    private static final File STORY_STAGE_DATA_FILE = new File("config/economy_system/story_stage_data.json");

    // 阶段缓存：stageId -> StoryStageData
    public static Map<Integer, StoryStageData> STAGE_CACHE = new ConcurrentHashMap<>();

    // 任务索引：taskId -> StoryTaskData（用于快速查找任务）
    public static Map<Integer, StoryTaskData> TASK_INDEX = new ConcurrentHashMap<>();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private static boolean dirCreated = false;
    private static boolean fileCreated = false;

    static Type stageMapType = new TypeToken<Map<Integer, StoryStageData>>() {}.getType();

    static {
        // 初始化目录和文件
        try {
            if (!STORY_STAGE_DATA_FILE.getParentFile().exists()) {
                dirCreated = STORY_STAGE_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("故事阶段数据目录创建成功：{}", STORY_STAGE_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.error("故事阶段数据目录创建失败：{}", STORY_STAGE_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!STORY_STAGE_DATA_FILE.exists()) {
                fileCreated = STORY_STAGE_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("故事阶段数据文件创建成功：{}", STORY_STAGE_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("故事阶段数据文件创建失败：{}", STORY_STAGE_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("故事阶段数据文件已存在：{}", STORY_STAGE_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化故事阶段数据文件失败", e);
        }
    }

    @SubscribeEvent
    public static void loadingStoryStageData(ServerStartingEvent event) {
        loadStageData();
    }

    /**
     * 加载阶段数据
     */
    public static void loadStageData() {
        try (FileReader reader = new FileReader(STORY_STAGE_DATA_FILE)) {
            Map<Integer, StoryStageData> stageData = GSON.fromJson(reader, stageMapType);
            STAGE_CACHE = stageData != null ? stageData : new ConcurrentHashMap<>();
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载故事阶段数据失败", e);
            STAGE_CACHE = new ConcurrentHashMap<>();
        }

        // 重建任务索引
        rebuildTaskIndex();

        EconomySystem.LOGGER.info("故事阶段数据加载完成，共 {} 个阶段，{} 个任务",
            STAGE_CACHE.size(), TASK_INDEX.size());
    }

    /**
     * 重建任务索引
     */
    private static void rebuildTaskIndex() {
        TASK_INDEX.clear();
        for (StoryStageData stage : STAGE_CACHE.values()) {
            if (stage.getTasks() != null) {
                for (StoryTaskData task : stage.getTasks()) {
                    TASK_INDEX.put(task.getTaskId(), task);
                }
            }
        }
    }

    /**
     * 保存阶段数据
     */
    public static void saveStageData() {
        try (FileWriter writer = new FileWriter(STORY_STAGE_DATA_FILE)) {
            GSON.toJson(STAGE_CACHE, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存故事阶段数据失败", e);
        }
    }

    // ==================== 阶段相关方法 ====================

    /**
     * 根据阶段ID获取阶段数据
     */
    public static StoryStageData getStage(int stageId) {
        return STAGE_CACHE.get(stageId);
    }

    /**
     * 获取所有阶段
     */
    public static Map<Integer, StoryStageData> getAllStages() {
        return STAGE_CACHE;
    }

    /**
     * 获取阶段数量
     */
    public static int getStageCount() {
        return STAGE_CACHE.size();
    }

    // ==================== 任务相关方法 ====================

    /**
     * 根据任务ID获取任务数据
     */
    public static StoryTaskData getTask(int taskId) {
        return TASK_INDEX.get(taskId);
    }

    /**
     * 获取指定阶段的所有任务
     */
    public static List<StoryTaskData> getTasksByStage(int stageId) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        return stage != null ? stage.getTasks() : null;
    }

    /**
     * 获取所有任务
     */
    public static Map<Integer, StoryTaskData> getAllTasks() {
        return TASK_INDEX;
    }

    // ==================== 任务完成相关方法 ====================

    /**
     * 玩家完成任务
     */
    public static boolean playerCompleteTask(int taskId, String playerName, UUID playerUUID) {
        StoryTaskData task = TASK_INDEX.get(taskId);
        if (task == null) {
            EconomySystem.LOGGER.warn("任务ID不存在：{}", taskId);
            return false;
        }

        if (task.isPlayerFinished(playerUUID)) {
            EconomySystem.LOGGER.info("玩家 {} 已经完成任务 {}", playerName, taskId);
            return false;
        }

        task.addFinishedPlayer(playerName, playerUUID);
        saveStageData();

        EconomySystem.LOGGER.info("玩家 {} 完成任务 {}", playerName, taskId);
        return true;
    }

    /**
     * 判断玩家是否已完成指定任务
     */
    public static boolean isPlayerFinishedTask(int taskId, UUID playerUUID) {
        StoryTaskData task = TASK_INDEX.get(taskId);
        return task != null && task.isPlayerFinished(playerUUID);
    }

    /**
     * 判断玩家是否已完成指定阶段的所有任务
     */
    public static boolean isPlayerFinishedStage(int stageId, UUID playerUUID) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        if (stage == null || stage.getTasks() == null) return false;

        for (StoryTaskData task : stage.getTasks()) {
            if (!task.isPlayerFinished(playerUUID)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取玩家在指定阶段的已完成任务数
     */
    public static int getPlayerCompletedTaskCount(int stageId, UUID playerUUID) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        if (stage == null || stage.getTasks() == null) return 0;

        int count = 0;
        for (StoryTaskData task : stage.getTasks()) {
            if (task.isPlayerFinished(playerUUID)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 怪物数值相关方法 ====================

    /**
     * 获取指定阶段的怪物数值调整器
     */
    public static StoryStageData.MonsterModifier getMonsterModifier(int stageId) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        return stage != null ? stage.getMonsterModifier() : null;
    }

    /**
     * 应用怪物数值调整
     * 返回调整后的属性数组：[生命值, 伤害, 速度, 击退抗性]
     */
    public static float[] applyMonsterModifier(int stageId, float baseHealth, float baseDamage, float baseSpeed, float baseKnockbackResist) {
        StoryStageData.MonsterModifier modifier = getMonsterModifier(stageId);
        if (modifier == null) {
            return new float[]{baseHealth, baseDamage, baseSpeed, baseKnockbackResist};
        }

        return new float[]{
            baseHealth * modifier.getHealthMultiplier(),
            baseDamage * modifier.getDamageMultiplier(),
            baseSpeed * modifier.getSpeedMultiplier(),
            baseKnockbackResist + modifier.getKnockbackResistance()
        };
    }
}
