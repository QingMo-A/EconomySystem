package com.mo.economy_system.core.login_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录数据管理器
 * 负责玩家登录/注册数据的持久化存储（JSON格式）
 */
public class PlayerLoginDataManager {
    private static final File LOGIN_DATA_FILE = new File("config/economy_system/data/login_data.json");
    private static final Map<UUID, PlayerLoginData> LOGIN_DATA_CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    /**
     * 静态初始化块
     * 创建必要的目录和文件
     */
    static {
        try {
            // 创建目录
            if (!LOGIN_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = LOGIN_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("登录数据目录创建成功：{}", LOGIN_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.error("登录数据目录创建失败：{}", LOGIN_DATA_FILE.getParentFile().getPath());
                }
            }
            // 创建文件
            if (!LOGIN_DATA_FILE.exists()) {
                boolean fileCreated = LOGIN_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("登录数据文件创建成功：{}", LOGIN_DATA_FILE.getPath());
                    // 创建空文件时写入空对象
                    saveAllLoginDataToFile(new ConcurrentHashMap<>());
                } else {
                    EconomySystem.LOGGER.error("登录数据文件创建失败：{}", LOGIN_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("登录数据文件已存在：{}", LOGIN_DATA_FILE.getPath());
                // 启动时加载数据到缓存
                loadAllDataToCache();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化登录数据文件失败", e);
        }
    }

    /**
     * 从文件加载所有登录数据到缓存
     */
    private static void loadAllDataToCache() {
        try (FileReader reader = new FileReader(LOGIN_DATA_FILE)) {
            Type type = new TypeToken<Map<UUID, PlayerLoginData>>(){}.getType();
            Map<UUID, PlayerLoginData> data = GSON.fromJson(reader, type);
            if (data != null) {
                LOGIN_DATA_CACHE.clear();
                LOGIN_DATA_CACHE.putAll(data);
                EconomySystem.LOGGER.info("成功加载 {} 条登录数据到缓存", data.size());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载登录数据到缓存失败", e);
        }
    }

    /**
     * 从文件加载所有登录数据
     * @return 所有玩家的登录数据
     */
    public static Map<UUID, PlayerLoginData> loadAllLoginDataFromFile() {
        try (FileReader reader = new FileReader(LOGIN_DATA_FILE)) {
            Type type = new TypeToken<Map<UUID, PlayerLoginData>>(){}.getType();
            Map<UUID, PlayerLoginData> data = GSON.fromJson(reader, type);
            return data != null ? data : new ConcurrentHashMap<>();
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载登录数据文件失败", e);
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * 保存所有登录数据到文件
     * @param data 要保存的数据
     */
    public static void saveAllLoginDataToFile(Map<UUID, PlayerLoginData> data) {
        try (FileWriter writer = new FileWriter(LOGIN_DATA_FILE)) {
            GSON.toJson(data, writer);
            EconomySystem.LOGGER.debug("登录数据已保存到文件");
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存登录数据到文件失败", e);
        }
    }

    /**
     * 检查玩家是否有登录数据
     * @param playerUUID 玩家UUID
     * @return 是否存在登录数据
     */
    public static boolean hasLoginData(UUID playerUUID) {
        // 优先查缓存
        if (LOGIN_DATA_CACHE.containsKey(playerUUID)) {
            return true;
        }
        // 缓存没有则查文件
        Map<UUID, PlayerLoginData> allData = loadAllLoginDataFromFile();
        return allData.containsKey(playerUUID);
    }

    /**
     * 获取玩家登录数据
     * @param playerUUID 玩家UUID
     * @return 玩家的登录数据，如果不存在则返回 null
     */
    public static PlayerLoginData getLoginData(UUID playerUUID) {
        // 先查缓存
        PlayerLoginData cached = LOGIN_DATA_CACHE.get(playerUUID);
        if (cached != null) {
            return cached;
        }

        // 缓存没有，从文件加载
        Map<UUID, PlayerLoginData> allData = loadAllLoginDataFromFile();
        PlayerLoginData data = allData.get(playerUUID);

        if (data != null) {
            // 加入缓存
            LOGIN_DATA_CACHE.put(playerUUID, data);
        }

        return data;
    }

    /**
     * 保存玩家登录数据
     * @param playerUUID 玩家UUID
     * @param data 登录数据
     */
    public static void saveLoginData(UUID playerUUID, PlayerLoginData data) {
        // 更新缓存
        LOGIN_DATA_CACHE.put(playerUUID, data);

        // 从文件加载所有数据
        Map<UUID, PlayerLoginData> allData = loadAllLoginDataFromFile();
        allData.put(playerUUID, data);

        // 保存到文件
        saveAllLoginDataToFile(allData);

        EconomySystem.LOGGER.debug("已保存玩家 {} 的登录数据", playerUUID);
    }

    /**
     * 删除玩家登录数据
     * @param playerUUID 玩家UUID
     */
    public static void deleteLoginData(UUID playerUUID) {
        // 从缓存移除
        LOGIN_DATA_CACHE.remove(playerUUID);

        // 从文件移除
        Map<UUID, PlayerLoginData> allData = loadAllLoginDataFromFile();
        allData.remove(playerUUID);
        saveAllLoginDataToFile(allData);

        EconomySystem.LOGGER.info("已删除玩家 {} 的登录数据", playerUUID);
    }

    /**
     * 获取所有登录数据（只读，包含缓存和文件的所有数据）
     * @return 所有登录数据
     */
    public static Map<UUID, PlayerLoginData> getAllLoginData() {
        // 合并缓存和文件数据
        Map<UUID, PlayerLoginData> result = new ConcurrentHashMap<>(loadAllLoginDataFromFile());
        result.putAll(LOGIN_DATA_CACHE);
        return result;
    }

    /**
     * 清空缓存（内存中的缓存）
     */
    public static void clearCache() {
        LOGIN_DATA_CACHE.clear();
        EconomySystem.LOGGER.info("登录数据缓存已清空");
    }

    /**
     * 重新加载所有数据到缓存
     */
    public static void reloadCache() {
        clearCache();
        loadAllDataToCache();
        EconomySystem.LOGGER.info("登录数据缓存已重新加载");
    }

    /**
     * 获取缓存的登录数据数量
     * @return 缓存中的数据条数
     */
    public static int getCacheSize() {
        return LOGIN_DATA_CACHE.size();
    }
}
