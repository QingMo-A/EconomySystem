package com.mo.economy_system.core.playerattributes_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.strength.StrengthSyncManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家属性数据管理器
 * 负责属性数据的初始化、加载、保存、缓存管理，适配登录/登出事件
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerAttributesDataManager {
    // 属性数据文件路径
    private static final File PLAYER_ATTRIBUTES_FILE = new File("config/economy_system/player_attributes_data.json");
    // 内存缓存
    private static final Map<UUID, PlayerAttributesData> ATTRIBUTES_CACHE = new ConcurrentHashMap<>();
    // Gson实例
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    static {
        try {
            // 创建父目录
            if (!PLAYER_ATTRIBUTES_FILE.getParentFile().exists()) {
                boolean dirCreated = PLAYER_ATTRIBUTES_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("玩家属性数据目录创建成功：{}", PLAYER_ATTRIBUTES_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.error("玩家属性数据目录创建失败：{}", PLAYER_ATTRIBUTES_FILE.getParentFile().getPath());
                }
            }
            // 创建属性文件
            if (!PLAYER_ATTRIBUTES_FILE.exists()) {
                boolean fileCreated = PLAYER_ATTRIBUTES_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("玩家属性数据文件创建成功：{}", PLAYER_ATTRIBUTES_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("玩家属性数据文件创建失败：{}", PLAYER_ATTRIBUTES_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("玩家属性数据文件已存在：{}", PLAYER_ATTRIBUTES_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化玩家属性数据文件失败", e);
        }
    }

    /**
     * 判断玩家是否已有属性数据
     */
    public static boolean hasPlayerAttributesData(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        // 优先查缓存
        if (ATTRIBUTES_CACHE.containsKey(playerUUID)) {
            return true;
        }
        // 缓存无则查文件
        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        return allAttributes.containsKey(playerUUID);
    }

    /**
     * 初始化玩家属性数据（新玩家/首次登录）
     */
    public static void initPlayerAttributesData(ServerPlayer player, int realLevel) {
        UUID playerUUID = player.getUUID();
        // 避免重复初始化
        if (hasPlayerAttributesData(player)) {
//            EconomySystem.LOGGER.info("玩家 {} 已有属性数据，同步等级为{}", player.getScoreboardName(), realLevel);
            PlayerAttributesData existingData = getPlayerAttributesData(playerUUID);
            // 同步最新等级
            existingData.setLevel(realLevel, player);
            ATTRIBUTES_CACHE.put(playerUUID, existingData);
            // 保存到文件
            Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
            allAttributes.put(playerUUID, existingData);
            saveAllAttributesToFile(allAttributes);
            return;
        }

        PlayerAttributesData newAttributesData = new PlayerAttributesData(playerUUID, player.getScoreboardName(), realLevel);
        ATTRIBUTES_CACHE.put(playerUUID, newAttributesData);

        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        allAttributes.put(playerUUID, newAttributesData);
        saveAllAttributesToFile(allAttributes);

        StrengthSyncManager.syncStrengthToClient(player);

//        EconomySystem.LOGGER.info("玩家 {} 属性数据初始化完成（同步真实等级{}）", player.getScoreboardName(), realLevel);
    }

    /**
     * 获取玩家属性数据（优先缓存，次文件）
     */
    public static PlayerAttributesData getPlayerAttributesData(UUID playerUUID) {
        // 优先查缓存
        if (ATTRIBUTES_CACHE.containsKey(playerUUID)) {
            return ATTRIBUTES_CACHE.get(playerUUID);
        }

        // 缓存无则查文件
        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        PlayerAttributesData attributesData = allAttributes.get(playerUUID);

        // 文件也无则返回默认数据
        if (attributesData == null) {
            EconomySystem.LOGGER.warn("玩家 {} 无属性数据，返回默认数据", playerUUID);
            attributesData = new PlayerAttributesData();
        } else {
            // 同步到缓存
            ATTRIBUTES_CACHE.put(playerUUID, attributesData);
        }
        return attributesData;
    }

    /**
     * 更新玩家等级（同步更新所有属性最大值）
     */
    public static void updatePlayerLevel(ServerPlayer player, int newLevel) {
        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = getPlayerAttributesData(playerUUID);

        // 更新等级（自动同步属性最大值）
        attributesData.setLevel(newLevel, player);
        attributesData.setPlayerName(player.getScoreboardName()); // 同步最新名称

        // 更新缓存+文件
        ATTRIBUTES_CACHE.put(playerUUID, attributesData);
        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        allAttributes.put(playerUUID, attributesData);
        saveAllAttributesToFile(allAttributes);

        EconomySystem.LOGGER.info("玩家 {} 等级更新为{}，属性数据已保存", player.getScoreboardName(), newLevel);
    }

    /**
     * 手动更新属性数据（外部调用，比如修改感染值/体力）
     */
    public static void updatePlayerAttributesData(ServerPlayer player, PlayerAttributesData newData) {
        UUID playerUUID = player.getUUID();
        // 更新缓存
        ATTRIBUTES_CACHE.put(playerUUID, newData);
        // 更新文件
        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        allAttributes.put(playerUUID, newData);
        saveAllAttributesToFile(allAttributes);

//        EconomySystem.LOGGER.info("玩家 {} 属性数据手动更新完成", player.getScoreboardName());
    }

    /**
     * 从文件加载所有玩家属性数据
     */
    private static Map<UUID, PlayerAttributesData> loadAllAttributesFromFile() {
        Map<UUID, PlayerAttributesData> allAttributes = new HashMap<>();
        try (FileReader reader = new FileReader(PLAYER_ATTRIBUTES_FILE)) {
            // 处理空文件（避免Gson解析报错）
            if (PLAYER_ATTRIBUTES_FILE.length() == 0) {
                return allAttributes;
            }
            // Gson解析Map<UUID, PlayerAttributesData>
            Type mapType = new TypeToken<Map<UUID, PlayerAttributesData>>() {}.getType();
            allAttributes = GSON.fromJson(reader, mapType);
            // 兜底：解析失败返回空Map
            if (allAttributes == null) {
                allAttributes = new HashMap<>();
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.warn("读取玩家属性数据文件失败，返回空数据", e);
        }
        return allAttributes;
    }

    /**
     * 保存所有玩家属性数据到文件
     */
    private static void saveAllAttributesToFile(Map<UUID, PlayerAttributesData> allAttributes) {
        try (FileWriter writer = new FileWriter(PLAYER_ATTRIBUTES_FILE)) {
            GSON.toJson(allAttributes, writer);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("写入玩家属性数据文件失败", e);
        }
    }

    /**
     * 玩家登出事件：保存数据+清理缓存
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        UUID playerUUID = player.getUUID();
        //获取当前属性数据并保存到文件
        PlayerAttributesData attributesData = getPlayerAttributesData(playerUUID);
        Map<UUID, PlayerAttributesData> allAttributes = loadAllAttributesFromFile();
        allAttributes.put(playerUUID, attributesData);
        saveAllAttributesToFile(allAttributes);

        //清理缓存
        ATTRIBUTES_CACHE.remove(playerUUID);

        EconomySystem.LOGGER.info("玩家 {} 登出，属性数据已保存，缓存已清理", player.getScoreboardName());
    }
}