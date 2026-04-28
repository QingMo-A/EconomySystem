package com.mo.economy_system.server.notice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家已读公告数据管理器
 * 负责管理玩家已读的公告ID
 */
public class PlayerNoticeDataManager {

    private static final File DATA_FILE = new File(
        FMLPaths.CONFIGDIR.get().toFile() + File.separator + EconomySystem.MODID + File.separator + "data",
        "player_notice_data.json"
    );

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    // 缓存：玩家UUID -> 已读公告ID集合
    private static final Map<UUID, Set<Integer>> READ_NOTICES_CACHE = new ConcurrentHashMap<>();

    /**
     * 初始化数据文件
     */
    public static void init() {
        // 确保数据目录存在
        File dataDir = DATA_FILE.getParentFile();
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // 如果文件不存在，创建空文件
        if (!DATA_FILE.exists()) {
            saveAllDataToFile(new HashMap<>());
        }

        // 加载数据到缓存
        loadAllDataToCache();
    }

    /**
     * 获取玩家的已读公告ID集合
     */
    public static Set<Integer> getReadNoticeIds(UUID playerUUID) {
        return READ_NOTICES_CACHE.computeIfAbsent(playerUUID, k -> new HashSet<>());
    }

    /**
     * 检查玩家是否已读某条公告
     */
    public static boolean hasReadNotice(UUID playerUUID, int noticeId) {
        Set<Integer> readIds = getReadNoticeIds(playerUUID);
        return readIds.contains(noticeId);
    }

    /**
     * 标记公告为已读
     */
    public static void markAsRead(UUID playerUUID, int noticeId) {
        Set<Integer> readIds = getReadNoticeIds(playerUUID);
        if (readIds.add(noticeId)) {
            // 添加成功（之前未读），保存到文件
            saveAllDataToFile(new HashMap<>(READ_NOTICES_CACHE));
        }
    }

    /**
     * 批量标记公告为已读
     */
    public static void markMultipleAsRead(UUID playerUUID, Set<Integer> noticeIds) {
        Set<Integer> readIds = getReadNoticeIds(playerUUID);
        if (readIds.addAll(noticeIds)) {
            saveAllDataToFile(new HashMap<>(READ_NOTICES_CACHE));
        }
    }

    /**
     * 从文件加载数据到缓存
     */
    private static void loadAllDataToCache() {
        try (FileInputStream fis = new FileInputStream(DATA_FILE);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            Type mapType = new TypeToken<Map<String, Set<Integer>>>() {}.getType();
            Map<String, Set<Integer>> data = GSON.fromJson(isr, mapType);

            if (data != null) {
                READ_NOTICES_CACHE.clear();
                for (Map.Entry<String, Set<Integer>> entry : data.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    READ_NOTICES_CACHE.put(uuid, entry.getValue());
                }
            }

            EconomySystem.LOGGER.info("已加载 {} 个玩家的公告阅读记录", READ_NOTICES_CACHE.size());

        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载玩家公告数据失败", e);
        }
    }

    /**
     * 保存所有数据到文件
     */
    private static void saveAllDataToFile(Map<UUID, Set<Integer>> data) {
        try (FileOutputStream fos = new FileOutputStream(DATA_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            // 转换 UUID 为字符串以便序列化
            Map<String, Set<Integer>> stringKeyData = new HashMap<>();
            for (Map.Entry<UUID, Set<Integer>> entry : data.entrySet()) {
                stringKeyData.put(entry.getKey().toString(), entry.getValue());
            }

            GSON.toJson(stringKeyData, osw);

        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存玩家公告数据失败", e);
        }
    }
}