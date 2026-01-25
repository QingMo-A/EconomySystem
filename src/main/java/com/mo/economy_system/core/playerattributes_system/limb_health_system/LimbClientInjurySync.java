package com.mo.economy_system.core.playerattributes_system.limb_health_system;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端肢体受伤数据同步
 * 存储客户端收到的受伤部位信息，用于GUI渲染
 */
@OnlyIn(Dist.CLIENT)
public class LimbClientInjurySync {
    // 玩家UUID -> (部位名称 -> 受伤时间戳)
    private static final Map<UUID, Map<String, Long>> PLAYER_INJURIES = new ConcurrentHashMap<>();

    // 感叹号显示持续时间（毫秒）
    public static final long INJURY_ICON_DURATION = 3000; // 3秒

    /**
     * 记录玩家受伤部位
     */
    public static void recordInjury(Player player, String limbTypeName, long injuryTime) {
        if (player == null) return;
        UUID uuid = player.getUUID();

        PLAYER_INJURIES.computeIfAbsent(uuid, k -> new HashMap<>()).put(limbTypeName, injuryTime);
    }

    /**
     * 检查玩家指定部位是否正在显示受伤图标
     */
    public static boolean isInjuryVisible(Player player, LimbType limbType) {
        if (player == null) return false;
        UUID uuid = player.getUUID();

        Map<String, Long> injuries = PLAYER_INJURIES.get(uuid);
        if (injuries == null) return false;

        Long injuryTime = injuries.get(limbType.name());
        if (injuryTime == null) return false;

        long timeSinceInjury = System.currentTimeMillis() - injuryTime;
        return timeSinceInjury < INJURY_ICON_DURATION;
    }

    /**
     * 获取最新受伤的部位（用于闪烁效果）
     * @return 最新受伤的部位，如果没有则返回null
     */
    public static LimbType getLatestInjuredLimb(Player player) {
        if (player == null) return null;
        UUID uuid = player.getUUID();

        Map<String, Long> injuries = PLAYER_INJURIES.get(uuid);
        if (injuries == null || injuries.isEmpty()) return null;

        // 找到时间戳最大的部位（最新的受伤）
        long latestTime = Long.MIN_VALUE;
        String latestLimbName = null;

        for (Map.Entry<String, Long> entry : injuries.entrySet()) {
            if (entry.getValue() > latestTime) {
                latestTime = entry.getValue();
                latestLimbName = entry.getKey();
            }
        }

        if (latestLimbName != null) {
            try {
                return LimbType.valueOf(latestLimbName);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取最新受伤的时间戳
     */
    public static long getLatestInjuryTime(Player player) {
        if (player == null) return 0;
        UUID uuid = player.getUUID();

        Map<String, Long> injuries = PLAYER_INJURIES.get(uuid);
        if (injuries == null || injuries.isEmpty()) return 0;

        long latestTime = 0;
        for (Long time : injuries.values()) {
            if (time > latestTime) {
                latestTime = time;
            }
        }
        return latestTime;
    }

    /**
     * 清理过期的受伤记录（每帧调用）
     */
    public static void cleanupExpiredInjuries(Player player) {
        if (player == null) return;
        UUID uuid = player.getUUID();

        Map<String, Long> injuries = PLAYER_INJURIES.get(uuid);
        if (injuries == null) return;

        long currentTime = System.currentTimeMillis();
        injuries.entrySet().removeIf(entry ->
            currentTime - entry.getValue() >= INJURY_ICON_DURATION
        );

        // 如果没有受伤记录了，移除玩家的entry
        if (injuries.isEmpty()) {
            PLAYER_INJURIES.remove(uuid);
        }
    }

    /**
     * 清理指定玩家的所有受伤记录（玩家断开连接时）
     */
    public static void clearPlayerInjuries(Player player) {
        if (player == null) return;
        PLAYER_INJURIES.remove(player.getUUID());
    }
}
