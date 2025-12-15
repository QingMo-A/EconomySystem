package com.mo.economy_system.server.chattitle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.ranktitle_system.Packet_SyncRankTitle;
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
 * 玩家Title数据管理器（整合JSON存储+事件监听+工具方法，替代原有Capability系统）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerTitleManager {
    // JSON文件存储路径（.minecraft/config/economy_system/player_titles.json）
    private static final File TITLE_DATA_FILE = FMLPaths.GAMEDIR.get()
            .resolve("config")
            .resolve("economy_system")
            .resolve("player_titles.json")
            .toFile();

    // Gson实例（格式化输出，便于手动编辑和调试）
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 内存缓存：UUID -> TitleID（核心存储，避免频繁IO）
    private static final Map<UUID, Integer> PLAYER_TITLE_CACHE = new HashMap<>();

    // 线程锁：防止多线程读写文件冲突（如多玩家同时退出）
    private static final ReentrantLock FILE_LOCK = new ReentrantLock();

    // 客户端专用缓存
    private static final Map<UUID, Integer> CLIENT_PLAYER_TITLE_CACHE = new HashMap<>();

    // 静态初始化（加载JSON数据）
    static {
        loadTitleDataFromFile();
    }

    /**
     * 从JSON文件加载所有玩家Title数据到内存缓存
     */
    private static void loadTitleDataFromFile() {
        FILE_LOCK.lock();
        try {
            // 确保存储目录存在
            if (!TITLE_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = TITLE_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("创建Title数据存储目录: {}", TITLE_DATA_FILE.getParentFile().getPath());
                }
            }

            // 文件不存在则创建空文件
            if (!TITLE_DATA_FILE.exists()) {
                boolean fileCreated = TITLE_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("创建空的Title数据文件: {}", TITLE_DATA_FILE.getPath());
                }
                PLAYER_TITLE_CACHE.clear();
                return;
            }

            // 读取并解析JSON文件
            try (FileReader reader = new FileReader(TITLE_DATA_FILE)) {
                Type dataType = new TypeToken<Map<UUID, Integer>>() {}.getType();
                Map<UUID, Integer> loadedData = GSON.fromJson(reader, dataType);

                if (loadedData != null && !loadedData.isEmpty()) {
                    PLAYER_TITLE_CACHE.putAll(loadedData);
                    EconomySystem.LOGGER.info("成功加载{}条玩家Title数据", PLAYER_TITLE_CACHE.size());
                } else {
                    EconomySystem.LOGGER.info("Title数据文件为空，初始化空缓存");
                    PLAYER_TITLE_CACHE.clear();
                }
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("加载Title数据文件失败", e);
            PLAYER_TITLE_CACHE.clear(); // 加载失败则清空缓存，避免脏数据
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 将内存缓存中的Title数据写入JSON文件（持久化）
     */
    private static void saveTitleDataToFile() {
        FILE_LOCK.lock();
        try {
            // 确保目录存在（防御性检查）
            if (!TITLE_DATA_FILE.getParentFile().exists()) {
                TITLE_DATA_FILE.getParentFile().mkdirs();
            }

            // 写入JSON文件
            try (FileWriter writer = new FileWriter(TITLE_DATA_FILE)) {
                GSON.toJson(PLAYER_TITLE_CACHE, writer);
                EconomySystem.LOGGER.info("成功保存{}条玩家Title数据到文件", PLAYER_TITLE_CACHE.size());
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("保存Title数据文件失败", e);
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 获取玩家的Title（区分客户端/服务器）
     * @param player 目标玩家
     * @return 玩家的Title实例，默认返回空头衔
     */
    public static Title getPlayerTitle(Player player) {
        if (player.level().isClientSide()) { // 客户端逻辑
            UUID playerUUID = player.getUUID();
            int titleId = CLIENT_PLAYER_TITLE_CACHE.getOrDefault(playerUUID, 0);
            return TitleRegistry.getTitleById(titleId);
        } else { // 服务端逻辑
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return null;
            }
            UUID playerUUID = serverPlayer.getUUID();
            int titleId = PLAYER_TITLE_CACHE.getOrDefault(playerUUID, 0);
            return TitleRegistry.getTitleById(titleId);
        }
    }

    /**
     * 设置玩家的Title（仅支持服务器玩家，更新缓存+立即持久化）
     * @param player 目标玩家
     * @param title 要设置的Title实例
     */
    public static void setPlayerTitle(Player player, Title title) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            EconomySystem.LOGGER.warn("尝试为客户端玩家设置Title，操作忽略");
            return;
        }

        // 更新内存缓存
        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_TITLE_CACHE.put(playerUUID, title.getTitleID());

        // 立即持久化到文件
        saveTitleDataToFile();

        // 同步到客户端
        syncTitleToClient(serverPlayer);

        EconomySystem.LOGGER.info("设置玩家Title: {}({}) -> {}",
                serverPlayer.getScoreboardName(), playerUUID, title.getTitleName());
    }

    /**
     * 移除玩家的Title数据
     * @param player 目标玩家
     */
    public static void removePlayerTitle(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_TITLE_CACHE.remove(playerUUID);
        saveTitleDataToFile();

        EconomySystem.LOGGER.info("移除玩家Title数据: {}({})",
                serverPlayer.getScoreboardName(), playerUUID);
    }

    // 事件监听方法
    /**
     * 玩家登录：同步Title数据到客户端
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 同步数据到客户端
        syncTitleToClient(serverPlayer);

        EconomySystem.LOGGER.info("玩家{}登录，同步Title数据: {}",
                serverPlayer.getScoreboardName(),
                getPlayerTitle(serverPlayer).getTitleName());
    }

    /**
     * 玩家退出：强制保存所有Title数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        // 强制保存缓存到文件
        saveTitleDataToFile();

        EconomySystem.LOGGER.info("玩家{}退出，已保存所有Title数据",
                player.getScoreboardName());
    }

    /**
     * 同步玩家Title数据到客户端
     * @param player 目标服务器玩家
     */
    private static void syncTitleToClient(ServerPlayer player) {
        try {
            for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                EconomySystem_NetworkManager.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> onlinePlayer),
                        new Packet_SyncRankTitle(player) // 发送包含最新称号的数据包
                );
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("同步Title数据到客户端失败: {}", player.getScoreboardName(), e);
        }
    }

    /**
     * 客户端设置玩家Title缓存
     * @param player 目标玩家
     * @param title 头衔实例
     */
    public static void setClientPlayerTitle(Player player, Title title) {
        if (player.level().isClientSide()) {
            CLIENT_PLAYER_TITLE_CACHE.put(player.getUUID(), title.getTitleID());
        }
    }
}