package com.mo.economy_system.server.rank;

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
 * 玩家Rank数据管理器（整合JSON存储+事件监听+工具方法，替代原有Capability系统）
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerRankManager {
    // JSON文件存储路径（.minecraft/config/economy_system/player_ranks.json）
    private static final File RANK_DATA_FILE = FMLPaths.GAMEDIR.get()
            .resolve("config")
            .resolve("economy_system")
            .resolve("player_ranks.json")
            .toFile();

    // Gson实例
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 内存缓存：UUID -> RankID（核心存储，避免频繁IO）
    private static final Map<UUID, String> PLAYER_RANK_CACHE = new HashMap<>();

    // 防止多线程读写文件冲突（如多玩家同时退出）
    private static final ReentrantLock FILE_LOCK = new ReentrantLock();
    private static final Map<UUID, String> CLIENT_PLAYER_RANK_CACHE = new HashMap<>(); // 客户端专用缓存

    //静态初始化（加载JSON数据）
    static {
        loadRankDataFromFile();
    }

    /**
     * 从JSON文件加载所有玩家Rank数据到内存缓存
     */
    private static void loadRankDataFromFile() {
        FILE_LOCK.lock();
        try {
            if (!RANK_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = RANK_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("创建Rank数据存储目录: {}", RANK_DATA_FILE.getParentFile().getPath());
                }
            }

            // 文件不存在则创建空文件
            if (!RANK_DATA_FILE.exists()) {
                boolean fileCreated = RANK_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("创建空的Rank数据文件: {}", RANK_DATA_FILE.getPath());
                }
                PLAYER_RANK_CACHE.clear();
                return;
            }

            // 读取并解析JSON文件
            try (FileReader reader = new FileReader(RANK_DATA_FILE)) {
                Type dataType = new TypeToken<Map<UUID, String>>() {}.getType();
                Map<UUID, String> loadedData = GSON.fromJson(reader, dataType);

                if (loadedData != null && !loadedData.isEmpty()) {
                    PLAYER_RANK_CACHE.putAll(loadedData);
                    EconomySystem.LOGGER.info("成功加载{}条玩家Rank数据", PLAYER_RANK_CACHE.size());
                } else {
                    EconomySystem.LOGGER.info("Rank数据文件为空，初始化空缓存");
                    PLAYER_RANK_CACHE.clear();
                }
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("加载Rank数据文件失败", e);
            PLAYER_RANK_CACHE.clear(); // 加载失败则清空缓存，避免脏数据
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 将内存缓存中的Rank数据写入JSON文件（持久化）
     */
    private static void saveRankDataToFile() {
        FILE_LOCK.lock();
        try {
            // 确保目录存在（防御性检查）
            if (!RANK_DATA_FILE.getParentFile().exists()) {
                RANK_DATA_FILE.getParentFile().mkdirs();
            }

            // 写入JSON文件
            try (FileWriter writer = new FileWriter(RANK_DATA_FILE)) {
                GSON.toJson(PLAYER_RANK_CACHE, writer);
                EconomySystem.LOGGER.info("成功保存{}条玩家Rank数据到文件", PLAYER_RANK_CACHE.size());
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("保存Rank数据文件失败", e);
        } finally {
            FILE_LOCK.unlock();
        }
    }

    /**
     * 获取玩家的Rank（仅支持服务器玩家）
     * @param player 目标玩家
     * @return 玩家的Rank实例，默认返回NO_RANK
     */
    public static Rank getPlayerRank(Player player) {
        if (player.level().isClientSide()) { // 客户端逻辑
            UUID playerUUID = player.getUUID();
            String rankId = CLIENT_PLAYER_RANK_CACHE.getOrDefault(playerUUID, RankRegistry.NO_RANK.getRankName());
            return RankRegistry.getRankByName(rankId);
        } else { // 服务端逻辑（保持不变）
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return RankRegistry.NO_RANK;
            }
            UUID playerUUID = serverPlayer.getUUID();
            String rankId = PLAYER_RANK_CACHE.getOrDefault(playerUUID, RankRegistry.NO_RANK.getRankName());
            return RankRegistry.getRankByName(rankId);
        }
    }

    /**
     * 设置玩家的Rank（仅支持服务器玩家，更新缓存立刻保存）
     * @param player 目标玩家
     * @param rank 要设置的Rank实例
     */
    public static void setPlayerRank(Player player, Rank rank) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            EconomySystem.LOGGER.warn("尝试为客户端玩家设置Rank，操作忽略");
            return;
        }

        // 更新内存缓存
        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_RANK_CACHE.put(playerUUID, rank.getRankName());

        // 立即保存
        saveRankDataToFile();

        // 同步到客户端
        syncRankToClient(serverPlayer);

        EconomySystem.LOGGER.info("设置玩家Rank: {}({}) -> {}",
                serverPlayer.getScoreboardName(), playerUUID, rank.getRankName());
    }

    /**
     * 移除玩家的Rank数据（可选）
     * @param player 目标玩家
     */
    public static void removePlayerRank(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_RANK_CACHE.remove(playerUUID);
        saveRankDataToFile();

        EconomySystem.LOGGER.info("移除玩家Rank数据: {}({})",
                serverPlayer.getScoreboardName(), playerUUID);
    }

    // 事件监听方法（处理玩家关键行为）
    /**
     * 玩家登录：同步Rank数据到客户端（缓存已在静态初始化时加载）
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 同步数据到客户端
        syncRankToClient(serverPlayer);

        EconomySystem.LOGGER.info("玩家{}登录，同步Rank数据: {}",
                serverPlayer.getScoreboardName(),
                getPlayerRank(serverPlayer).getRankName());
    }

    /**
     * 玩家退出：强制保存所有Rank数据（兜底，防止内存数据未落地）
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        // 强制保存缓存到文件
        saveRankDataToFile();

        EconomySystem.LOGGER.info("玩家{}退出，已保存所有Rank数据",
                player.getScoreboardName());
    }

    /**
     * 同步玩家Rank数据到客户端（复用原有网络包）
     * @param player 目标服务器玩家
     */
    private static void syncRankToClient(ServerPlayer player) {
        try {
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new Packet_SyncRankTitle(player)
            );
        } catch (Exception e) {
            EconomySystem.LOGGER.error("同步Rank数据到客户端失败: {}", player.getScoreboardName(), e);
        }
    }

    public static void setClientPlayerRank(Player player, Rank rank) {
        if (player.level().isClientSide()) {
            CLIENT_PLAYER_RANK_CACHE.put(player.getUUID(), rank.getRankName());
        }
    }
}