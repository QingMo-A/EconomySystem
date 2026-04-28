package com.mo.economy_system.server.playerbiomes;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家生物群系探索数据管理器
 * 负责管理玩家已探索的生物群系数据
 */
@EventBusSubscriber(modid = EconomySystem.MODID)
public class PlayerBiomesDataManager {
    private static final File BIOMES_DATA_FILE = new File("config/economy_system/data/player_biomes_data.json");
    private static final Map<UUID, Set<String>> BIOMES_CACHE = new ConcurrentHashMap<>();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    static {
        try {
            // 创建目录
            if (!BIOMES_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = BIOMES_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("生物群系数据目录创建成功：{}", BIOMES_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.error("生物群系数据目录创建失败：{}", BIOMES_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!BIOMES_DATA_FILE.exists()) {
                boolean fileCreated = BIOMES_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("生物群系数据文件创建成功：{}", BIOMES_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("生物群系数据文件创建失败：{}", BIOMES_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("生物群系数据文件已存在：{}", BIOMES_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化生物群系数据文件失败", e);
        }
    }

    /**
     * 获取玩家已探索的生物群系列表
     */
    public static Set<String> getExploredBiomes(UUID playerUUID) {
        // 优先查缓存
        if (BIOMES_CACHE.containsKey(playerUUID)) {
            return BIOMES_CACHE.get(playerUUID);
        }

        // 缓存无则查文件
        Map<UUID, Set<String>> allBiomesData = loadAllBiomesDataFromFile();
        Set<String> biomes = allBiomesData.get(playerUUID);

        // 文件也无则返回空集合
        if (biomes == null) {
            biomes = new ConcurrentHashMap().newKeySet();
        } else {
            // 同步到缓存
            BIOMES_CACHE.put(playerUUID, biomes);
        }

        return biomes;
    }

    /**
     * 添加新的生物群系到玩家探索记录
     * @return 如果是新生物群系返回 true，否则返回 false
     */
    public static boolean addExploredBiome(UUID playerUUID, String biomeKey) {
        Set<String> exploredBiomes = getExploredBiomes(playerUUID);

        // 检查是否已探索
        if (exploredBiomes.contains(biomeKey)) {
            return false;
        }

        // 添加新生物群系
        exploredBiomes.add(biomeKey);

        // 更新缓存
        BIOMES_CACHE.put(playerUUID, exploredBiomes);

        // 保存到文件
        saveAllBiomesDataToFile();

        return true;
    }

    /**
     * 检查玩家是否探索过指定生物群系
     */
    public static boolean hasExploredBiome(UUID playerUUID, String biomeKey) {
        return getExploredBiomes(playerUUID).contains(biomeKey);
    }

    /**
     * 获取玩家已探索的生物群系数量
     */
    public static int getExploredBiomeCount(UUID playerUUID) {
        return getExploredBiomes(playerUUID).size();
    }

    /**
     * 从文件加载所有玩家的生物群系数据
     */
    private static Map<UUID, Set<String>> loadAllBiomesDataFromFile() {
        Map<UUID, Set<String>> allBiomesData = new ConcurrentHashMap<>();
        try (FileReader reader = new FileReader(BIOMES_DATA_FILE)) {
            // 处理空文件
            if (BIOMES_DATA_FILE.length() == 0) {
                return allBiomesData;
            }
            Type mapType = new TypeToken<Map<UUID, Set<String>>>() {}.getType();
            allBiomesData = GSON.fromJson(reader, mapType);
            // 兜底：Gson解析失败返回空Map
            if (allBiomesData == null) {
                allBiomesData = new HashMap<>();
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.warn("读取生物群系数据文件失败，返回空数据", e);
        }
        return allBiomesData;
    }

    /**
     * 保存所有玩家的生物群系数据到文件
     */
    private static void saveAllBiomesDataToFile() {
        try (FileWriter writer = new FileWriter(BIOMES_DATA_FILE)) {
            GSON.toJson(BIOMES_CACHE, writer);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("写入生物群系数据文件失败", e);
        }
    }

    /**
     * 玩家登录时加载数据到缓存
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerUUID = player.getUUID();
        if (!BIOMES_CACHE.containsKey(playerUUID)) {
            Set<String> biomes = getExploredBiomes(playerUUID);
            BIOMES_CACHE.put(playerUUID, biomes);
            EconomySystem.LOGGER.info("玩家 {} 生物群系数据已加载，已探索 {} 个生物群系",
                    player.getScoreboardName(), biomes.size());
        }
    }

    /**
     * 玩家登出时清理缓存
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            Set<String> biomes = BIOMES_CACHE.remove(playerUUID);
            EconomySystem.LOGGER.info("玩家 {} 登出，生物群系缓存已清理（已探索 {} 个）",
                    player.getScoreboardName(), biomes != null ? biomes.size() : 0);
        }
    }
}
