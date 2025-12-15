package com.mo.economy_system.playerlevel.overalllevel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.level_system.Packet_SyncLevel;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 玩家等级数据管理器（整合JSON存储+事件监听+工具方法，替代原有Capability系统）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLevelManager {
    // JSON文件存储路径（.minecraft/config/economy_system/player_levels.json）
    private static final File LEVEL_DATA_FILE = FMLPaths.GAMEDIR.get()
            .resolve("config")
            .resolve("economy_system")
            .resolve("player_levels.json")
            .toFile();

    // Gson实例（格式化输出，便于手动编辑和调试）
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 内存缓存：UUID -> 等级值（核心存储，避免频繁IO）
    private static final Map<UUID, Integer> PLAYER_LEVEL_CACHE = new HashMap<>();

    // 线程锁：防止多线程读写文件冲突
    private static final ReentrantLock FILE_LOCK = new ReentrantLock();

    // 客户端专用缓存
    private static final Map<UUID, Integer> CLIENT_PLAYER_LEVEL_CACHE = new HashMap<>();

    // 静态初始化（加载JSON数据）
    static {
        loadLevelDataFromFile();
    }

    /**
     * 从JSON文件加载所有玩家等级数据到内存缓存
     */
    private static void loadLevelDataFromFile() {
        FILE_LOCK.lock();
        try {
            // 确保存储目录存在
            if (!LEVEL_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = LEVEL_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("创建Level数据存储目录: {}", LEVEL_DATA_FILE.getParentFile().getPath());
                }
            }

            // 文件不存在则创建空文件
            if (!LEVEL_DATA_FILE.exists()) {
                boolean fileCreated = LEVEL_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("创建空的Level数据文件: {}", LEVEL_DATA_FILE.getPath());
                }
                PLAYER_LEVEL_CACHE.clear();
                return;
            }

            // 读取并解析JSON文件
            try (FileReader reader = new FileReader(LEVEL_DATA_FILE)) {
                Type dataType = new TypeToken<Map<UUID, Integer>>() {}.getType();
                Map<UUID, Integer> loadedData = GSON.fromJson(reader, dataType);

                if (loadedData != null && !loadedData.isEmpty()) {
                    PLAYER_LEVEL_CACHE.putAll(loadedData);
                    EconomySystem.LOGGER.info("成功加载{}条玩家Level数据", PLAYER_LEVEL_CACHE.size());
                } else {
                    EconomySystem.LOGGER.info("Level数据文件为空，初始化空缓存");
                    PLAYER_LEVEL_CACHE.clear();
                }
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("加载Level数据文件失败", e);
            PLAYER_LEVEL_CACHE.clear(); // 加载失败则清空缓存，避免脏数据
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 将内存缓存中的等级数据写入JSON文件（持久化）
     */
    private static void saveLevelDataToFile() {
        FILE_LOCK.lock();
        try {
            // 确保目录存在（防御性检查）
            if (!LEVEL_DATA_FILE.getParentFile().exists()) {
                LEVEL_DATA_FILE.getParentFile().mkdirs();
            }

            // 写入JSON文件
            try (FileWriter writer = new FileWriter(LEVEL_DATA_FILE)) {
                GSON.toJson(PLAYER_LEVEL_CACHE, writer);
                EconomySystem.LOGGER.info("成功保存{}条玩家Level数据到文件", PLAYER_LEVEL_CACHE.size());
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("保存Level数据文件失败", e);
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 获取玩家的等级（区分客户端/服务器）
     * @param player 目标玩家
     * @return 玩家的等级值，默认为0
     */
    public static int getPlayerLevel(Player player) {
        if (player.level().isClientSide()) { // 客户端逻辑
            UUID playerUUID = player.getUUID();
            return CLIENT_PLAYER_LEVEL_CACHE.getOrDefault(playerUUID, 0);
        } else { // 服务端逻辑
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            UUID playerUUID = serverPlayer.getUUID();
            return PLAYER_LEVEL_CACHE.getOrDefault(playerUUID, 0);
        }
    }

    /**
     * 设置玩家的等级（仅支持服务器玩家，更新缓存+立即持久化）
     * @param player 目标玩家
     * @param level 要设置的等级值
     */
    public static void setPlayerLevel(Player player, int level) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            EconomySystem.LOGGER.warn("尝试为客户端玩家设置Level，操作忽略");
            return;
        }

        // 确保等级不为负数
        int actualLevel = Math.max(0, level);

        // 更新内存缓存
        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_LEVEL_CACHE.put(playerUUID, actualLevel);

        // 立即持久化到文件
        saveLevelDataToFile();

        // 同步到客户端
        syncLevelToClient(serverPlayer);

        EconomySystem.LOGGER.info("设置玩家Level: {}({}) -> {}",
                serverPlayer.getScoreboardName(), playerUUID, actualLevel);
    }

    /**
     * 增加玩家等级（支持负数，可用于减少等级）
     * @param player 目标玩家
     * @param amount 增加的数量（可为负数）
     */
    public static void addPlayerLevel(Player player, int amount) {
        int currentLevel = getPlayerLevel(player);
        setPlayerLevel(player, currentLevel + amount);
    }

    /**
     * 重置玩家等级数据
     * @param player 目标玩家
     */
    public static void resetPlayerLevel(Player player) {
        setPlayerLevel(player, 0);
    }

    // 事件监听方法
    /**
     * 玩家登录：同步等级数据到客户端
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 同步数据到客户端
        syncLevelToClient(serverPlayer);

        EconomySystem.LOGGER.info("玩家{}登录，同步Level数据: {}",
                serverPlayer.getScoreboardName(),
                getPlayerLevel(serverPlayer));
    }

    /**
     * 玩家退出：强制保存所有等级数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        // 强制保存缓存到文件
        saveLevelDataToFile();

        EconomySystem.LOGGER.info("玩家{}退出，已保存所有Level数据",
                player.getScoreboardName());
    }

    /**
     * 同步玩家等级数据到客户端
     * @param player 目标服务器玩家
     */
    private static void syncLevelToClient(ServerPlayer player) {
        try {
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new Packet_SyncLevel(player)
            );
        } catch (Exception e) {
            EconomySystem.LOGGER.error("同步Level数据到客户端失败: {}", player.getScoreboardName(), e);
        }
    }

    /**
     * 客户端设置玩家等级缓存
     * @param player 目标玩家
     * @param level 等级值
     */
    public static void setClientPlayerLevel(Player player, int level) {
        if (player.level().isClientSide()) {
            CLIENT_PLAYER_LEVEL_CACHE.put(player.getUUID(), level);
        }
        if (player.getUUID().equals(Minecraft.getInstance().player.getUUID())) {
            ServerInformationDisplay.PLAYER_OVERALL_LEVEL = level;
        }
    }
}