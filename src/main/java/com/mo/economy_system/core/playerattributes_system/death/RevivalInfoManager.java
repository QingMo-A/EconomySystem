package com.mo.economy_system.core.playerattributes_system.death;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.server_screen.tips.TipPushHelper;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复活信息管理器
 * 用于存储被复活道具复活的玩家的复活信息
 * 玩家登录后根据这些信息发送提示消息
 */
public class RevivalInfoManager {

    private static final String FILE_NAME = "revival_info.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File REVIVAL_INFO_FILE = new File("config/economy_system/data/" + FILE_NAME);

    // 内存缓存：UUID -> 复活信息
    private static final Map<UUID, RevivalInfo> revivalInfoMap = new ConcurrentHashMap<>();

    // 静态初始化块：类加载时自动加载数据
    static {
        try {
            // 创建父目录
            if (!REVIVAL_INFO_FILE.getParentFile().exists()) {
                REVIVAL_INFO_FILE.getParentFile().mkdirs();
            }
            // 加载数据
            loadFromFile();
        } catch (Exception e) {
            EconomySystem.LOGGER.error("初始化复活信息管理器失败", e);
        }
    }

    /**
     * 复活信息记录
     */
    public static class RevivalInfo {
        private final String reviverName;      // 复活者名称
        private final boolean reviverIsInfected; // 复活者是否是感染者

        public RevivalInfo(String reviverName, boolean reviverIsInfected) {
            this.reviverName = reviverName;
            this.reviverIsInfected = reviverIsInfected;
        }

        public String getReviverName() {
            return reviverName;
        }

        public boolean isReviverInfected() {
            return reviverIsInfected;
        }
    }

    /**
     * 设置复活信息
     */
    public static void setRevivalInfo(UUID playerUUID, String reviverName, boolean reviverIsInfected) {
        revivalInfoMap.put(playerUUID, new RevivalInfo(reviverName, reviverIsInfected));
        saveToFile();
        EconomySystem.LOGGER.info("记录复活信息: 玩家 {} 被 {} ({}) 复活",
                playerUUID, reviverName, reviverIsInfected ? "感染者" : "幸存者");
    }

    /**
     * 获取复活信息
     */
    public static RevivalInfo getRevivalInfo(UUID playerUUID) {
        return revivalInfoMap.get(playerUUID);
    }

    /**
     * 移除复活信息（登录发送提示后调用）
     */
    public static void removeRevivalInfo(UUID playerUUID) {
        RevivalInfo removed = revivalInfoMap.remove(playerUUID);
        if (removed != null) {
            saveToFile();
            EconomySystem.LOGGER.info("清除复活信息: 玩家 {}", playerUUID);
        }
    }

    /**
     * 检查是否有待显示的复活信息
     */
    public static boolean hasRevivalInfo(UUID playerUUID) {
        return revivalInfoMap.containsKey(playerUUID);
    }

    /**
     * 保存到文件
     */
    private static void saveToFile() {
        try {
            try (FileWriter writer = new FileWriter(REVIVAL_INFO_FILE)) {
                GSON.toJson(revivalInfoMap, writer);
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存复活信息失败", e);
        }
    }

    /**
     * 从文件加载
     */
    public static void loadFromFile() {
        if (!REVIVAL_INFO_FILE.exists()) {
            EconomySystem.LOGGER.info("复活信息文件不存在，跳过加载");
            return;
        }

        try (FileReader reader = new FileReader(REVIVAL_INFO_FILE)) {
            Type type = new TypeToken<Map<UUID, RevivalInfo>>() {}.getType();
            Map<UUID, RevivalInfo> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                revivalInfoMap.clear();
                revivalInfoMap.putAll(loaded);
                EconomySystem.LOGGER.info("已加载 {} 条复活信息", revivalInfoMap.size());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载复活信息失败", e);
        }
    }

    /**
     * 清空所有复活信息
     */
    public static void clearAll() {
        revivalInfoMap.clear();
        saveToFile();
    }

    /**
     * 检查并发送复活提示消息
     * @param player 服务端玩家实例
     */
    public static void checkAndSendRevivalTip(ServerPlayer player) {
        if (hasRevivalInfo(player.getUUID())) {
            RevivalInfo info = getRevivalInfo(player.getUUID());
            if (info != null) {
                String reviverIdentity = info.isReviverInfected() ? "§c感染者" : "§a幸存者";
                String message = String.format("§d§l✦ 复活通知 ✦\n§f%s §e牺牲了自己一半的重生点数复活了您\n§7您现在的身份为：%s",
                        info.getReviverName(), reviverIdentity);
                TipPushHelper.sendTipToPlayer(player, message, 15000); // 显示15秒
                // 清除复活信息，避免重复提示
                removeRevivalInfo(player.getUUID());
                EconomySystem.LOGGER.info("已向玩家 {} 发送复活提示", player.getScoreboardName());
            }
        }
    }
}
