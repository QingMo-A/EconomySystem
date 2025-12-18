package com.mo.economy_system.core.task_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mo.economy_system.server.GetServerInstance.SERVER_INSTANCE;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaskDataManager {
    private static final File TASK_PLAYER_DATA_FILE = new File("config/economy_system/task_player_data.json");
    private static final File TASK_SERVER_DATA_FILE = new File("config/economy_system/task_server_data.json");
    public static Map<Integer, TaskServerData> TASK_SERVER_DATA_CACHE = new ConcurrentHashMap<>();
    public static Map<Integer, TaskPlayerData> TASK_PLAYER_DATA_CACHE = new ConcurrentHashMap<>();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();  //创建带缩进的配置json文件

    private static int maxServerTaskID = 0;
    private static int maxPlayerTaskID = 0;

    private static boolean dirCreated = false;
    private static boolean fileCreated = false;

    static Type playerMapType = new TypeToken<Map<Integer, TaskPlayerData>>() {}.getType();
    static Type serverMapType = new TypeToken<Map<Integer, TaskServerData>>() {}.getType();

    static {
        try {
            if (!TASK_PLAYER_DATA_FILE.getParentFile().exists()) {
                dirCreated = TASK_PLAYER_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("玩家任务数据目录创建成功：{}", TASK_PLAYER_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.info("玩家任务数据目录创建失败：{}", TASK_PLAYER_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!TASK_PLAYER_DATA_FILE.exists()) {
                fileCreated = TASK_PLAYER_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("玩家任务数据文件创建成功：{}", TASK_PLAYER_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("玩家任务数据文件创建失败：{}", TASK_PLAYER_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("玩家任务数据文件已存在：{}", TASK_PLAYER_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("玩家初始化任务数据文件失败", e);
        }

        try {
            if (!TASK_SERVER_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = TASK_SERVER_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("服务器任务数据目录创建成功：{}", TASK_SERVER_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.info("服务器任务数据目录创建失败：{}", TASK_SERVER_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!TASK_SERVER_DATA_FILE.exists()) {
                boolean fileCreated = TASK_SERVER_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("服务器任务数据文件创建成功：{}", TASK_SERVER_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("服务器任务数据文件创建失败：{}", TASK_SERVER_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("服务器任务数据文件已存在：{}", TASK_SERVER_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("服务器初始化任务数据文件失败", e);
        }
    }

    private static void calculateMaxTaskIDs() {
        //计算服务器任务最大ID
        for (int taskId : TASK_SERVER_DATA_CACHE.keySet()) {
            if (taskId > maxServerTaskID) {
                maxServerTaskID = taskId;
            }
        }
        //计算玩家任务最大ID
        for (int taskId : TASK_PLAYER_DATA_CACHE.keySet()) {
            if (taskId > maxPlayerTaskID) {
                maxPlayerTaskID = taskId;
            }
        }
    }

    @SubscribeEvent
    public static void loadingTaskData(ServerStartingEvent event) {
        //加载服务器任务数据
        try (FileReader reader = new FileReader(TASK_SERVER_DATA_FILE)) {
            Map<Integer, TaskServerData> serverData = GSON.fromJson(reader, serverMapType);
            TASK_SERVER_DATA_CACHE = serverData != null ? serverData : new ConcurrentHashMap<>();
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载服务器任务数据失败", e);
            TASK_SERVER_DATA_CACHE = new ConcurrentHashMap<>(); //失败时初始化空缓存
        }
        //加载玩家任务数据
        try (FileReader reader = new FileReader(TASK_PLAYER_DATA_FILE)) {
            Map<Integer, TaskPlayerData> loadedData = GSON.fromJson(reader, playerMapType);
            TASK_PLAYER_DATA_CACHE = loadedData != null ? loadedData : new ConcurrentHashMap<>();
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载玩家任务数据失败", e);
            TASK_PLAYER_DATA_CACHE = new ConcurrentHashMap<>();
        }
        calculateMaxTaskIDs();
    }

    //写入文件
    private static void saveServerTaskData() {
        try (FileWriter writer = new FileWriter(TASK_SERVER_DATA_FILE)) {
            GSON.toJson(TASK_SERVER_DATA_CACHE, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存服务器任务数据失败", e);
        }
    }
    private static void savePlayerTaskData() {
        try (FileWriter writer = new FileWriter(TASK_PLAYER_DATA_FILE)) {
            GSON.toJson(TASK_PLAYER_DATA_CACHE, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存玩家任务数据失败", e);
        }
    }

    //创建一个服务器任务
    public static void createServerTask (String taskName, String taskContent, long endTime) {
        int newTaskId = maxServerTaskID + 1;
        long startTime = System.currentTimeMillis();
        TaskServerData newTask = new TaskServerData(newTaskId, taskName, taskContent, startTime, endTime, 0.0f);
        TASK_SERVER_DATA_CACHE.put(newTaskId, newTask);
        maxServerTaskID = newTaskId;
        saveServerTaskData();
    }
    //添加一个所有玩家的个人任务
    public static void createPlayerTask (String taskName, String taskContent, long endTime) {
        int newTaskId = maxPlayerTaskID + 1;
        long startTime = System.currentTimeMillis();
        TaskPlayerData newTask = new TaskPlayerData(newTaskId, taskName, taskContent, startTime, endTime);
        TASK_PLAYER_DATA_CACHE.put(newTaskId, newTask); //存入缓存
        maxPlayerTaskID = newTaskId; //更新最大ID
        savePlayerTaskData();
    }
    //给专属玩家添加一个任务
    public static void createOnlyOnePlayerTask(String taskName, String taskContent, long endTime, String playerName, UUID playerUUID) {
        int newTaskId = maxPlayerTaskID + 1;
        long startTime = System.currentTimeMillis();
        TaskPlayerData newTask = new TaskPlayerData(newTaskId, taskName, taskContent, startTime, endTime);
        //还有特殊逻辑
        TASK_PLAYER_DATA_CACHE.put(newTaskId, newTask);
        maxPlayerTaskID = newTaskId;
        savePlayerTaskData();
    }

    //玩家完成个人任务
    public static void playerCompleteOwnTask(int taskId, String playerName, UUID playerUUID) {
        TaskPlayerData task = TASK_PLAYER_DATA_CACHE.get(taskId);
        if (task != null) {
            if (!task.isPlayerFinished(playerUUID)) {
                task.addFinishedPlayer(playerName, playerUUID); // 添加到完成列表
                savePlayerTaskData(); // 立即保存到文件
                // 触发全服广播（后续步骤实现）
                broadcastFullTaskDataToAllPlayers();
            } else {
                EconomySystem.LOGGER.warn("玩家任务ID不存在：{}", taskId);
            }
            }
    }
    //玩家完成服务器集体任务
    public static void playerCompleteServerTask(int taskId, String playerName, UUID playerUUID) {
        TaskServerData task = TASK_SERVER_DATA_CACHE.get(taskId);
        if (task != null) {
            if (!task.isPlayerFinished(playerUUID)) {
                task.addFinishedPlayer(playerName, playerUUID);
                // 此处可补充服务器任务的进度计算逻辑（例如按完成人数更新百分比）
                // task.setTaskCompletePercentage(calculateProgress());
                saveServerTaskData(); // 保存到文件
                broadcastFullTaskDataToAllPlayers();
            }
        }
    }
    //对全服玩家更新任务数据
    public static void broadcastFullTaskDataToAllPlayers() {
        // 获取服务器实例
        MinecraftServer server = SERVER_INSTANCE;
        if (server == null) return;

        // 遍历所有在线玩家
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 为每个玩家创建全量数据包（包含该玩家的UUID，用于判断其是否完成任务）
            Packet_SyncFullTaskData packet = new Packet_SyncFullTaskData(
                    player.getUUID(),
                    TASK_PLAYER_DATA_CACHE, // 最新的玩家任务缓存
                    TASK_SERVER_DATA_CACHE  // 最新的服务器任务缓存
            );
            // 发送数据包给单个玩家
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    packet
            );
        }
        EconomySystem.LOGGER.info("已向全服玩家广播最新任务数据");
    }
}
