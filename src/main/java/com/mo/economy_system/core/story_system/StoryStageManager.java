package com.mo.economy_system.core.story_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

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
@EventBusSubscriber(modid = EconomySystem.MODID)
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

    // ==================== 全服统计相关方法 ====================

    /**
     * 获取指定任务的全服完成人数
     */
    public static int getTaskFinishedCount(int taskId) {
        StoryTaskData task = TASK_INDEX.get(taskId);
        return task != null ? task.getFinishedPlayerCount() : 0;
    }

    /**
     * 获取指定阶段的全服完成人数统计
     * @return int[] 数组，每个元素对应该阶段每个任务的完成人数
     */
    public static int[] getStageTaskFinishedCounts(int stageId) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        if (stage == null || stage.getTasks() == null) {
            return new int[0];
        }

        int[] counts = new int[stage.getTasks().size()];
        for (int i = 0; i < stage.getTasks().size(); i++) {
            counts[i] = stage.getTasks().get(i).getFinishedPlayerCount();
        }
        return counts;
    }

    /**
     * 获取指定阶段中完成指定任务的所有玩家名单
     */
    public static java.util.List<String> getTaskFinishedPlayers(int taskId) {
        StoryTaskData task = TASK_INDEX.get(taskId);
        if (task == null || task.getFinishedPlayers() == null) {
            return new java.util.ArrayList<>();
        }

        return task.getFinishedPlayers().stream()
                .map(StoryTaskData.FinishedPlayer::getPlayerName)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定阶段的全服总完成人数（去重，玩家完成该阶段任意任务即计数）
     */
    public static int getStageUniquePlayerCount(int stageId) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        if (stage == null || stage.getTasks() == null) {
            return 0;
        }

        java.util.Set<UUID> uniquePlayers = new java.util.HashSet<>();
        for (StoryTaskData task : stage.getTasks()) {
            if (task.getFinishedPlayers() != null) {
                for (StoryTaskData.FinishedPlayer fp : task.getFinishedPlayers()) {
                    if (fp.getPlayerUUID() != null) {
                        uniquePlayers.add(fp.getPlayerUUID());
                    }
                }
            }
        }
        return uniquePlayers.size();
    }

    /**
     * 获取全服总任务完成次数（跨所有任务，不区分玩家）
     */
    public static int getTotalTaskCompletions() {
        int total = 0;
        for (StoryTaskData task : TASK_INDEX.values()) {
            total += task.getFinishedPlayerCount();
        }
        return total;
    }

    /**
     * 获取全服总参与玩家数（至少完成一个任务的玩家）
     */
    public static int getTotalUniquePlayers() {
        java.util.Set<UUID> uniquePlayers = new java.util.HashSet<>();
        for (StoryTaskData task : TASK_INDEX.values()) {
            if (task.getFinishedPlayers() != null) {
                for (StoryTaskData.FinishedPlayer fp : task.getFinishedPlayers()) {
                    if (fp.getPlayerUUID() != null) {
                        uniquePlayers.add(fp.getPlayerUUID());
                    }
                }
            }
        }
        return uniquePlayers.size();
    }

    /**
     * 获取任务完成度统计信息
     * @return 字符串格式的统计信息
     */
    public static String getTaskStatisticsString(int taskId) {
        StoryTaskData task = TASK_INDEX.get(taskId);
        if (task == null) {
            return "任务不存在";
        }

        int finishedCount = task.getFinishedPlayerCount();
        return String.format("任务 [%d] %s: %d 人已完成", task.getTaskId(), task.getTaskName(), finishedCount);
    }

    /**
     * 获取阶段完成度统计信息
     * @return 字符串格式的统计信息
     */
    public static String getStageStatisticsString(int stageId) {
        StoryStageData stage = STAGE_CACHE.get(stageId);
        if (stage == null) {
            return "阶段不存在";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== 阶段 %d: %s ===\n", stage.getStageId(), stage.getStageName()));

        for (StoryTaskData task : stage.getTasks()) {
            int finishedCount = task.getFinishedPlayerCount();
            sb.append(String.format("  [%d] %s: %d 人完成\n", task.getTaskId(), task.getTaskName(), finishedCount));
        }

        int uniquePlayers = getStageUniquePlayerCount(stageId);
        sb.append(String.format("阶段参与人数: %d 人", uniquePlayers));

        return sb.toString();
    }
}
