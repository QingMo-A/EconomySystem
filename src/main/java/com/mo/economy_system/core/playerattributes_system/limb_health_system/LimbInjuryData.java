package com.mo.economy_system.core.playerattributes_system.limb_health_system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 肢体受伤状态数据
 * 跟踪玩家各部位的受伤状态和受伤时间
 */
public class LimbInjuryData {
    private UUID playerUUID;
    // 部位 -> 受伤时间戳（毫秒）
    private Map<String, Long> limbInjuryTimes;

    // 感叹号显示持续时间（毫秒）
    public static final long INJURY_ICON_DURATION = 3000; // 3秒

    public LimbInjuryData() {
        this.limbInjuryTimes = new HashMap<>();
    }

    public LimbInjuryData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.limbInjuryTimes = new HashMap<>();
    }

    /**
     * 记录部位受伤
     */
    public void recordInjury(LimbType limbType) {
        limbInjuryTimes.put(limbType.name(), System.currentTimeMillis());
    }

    /**
     * 检查部位是否正在显示受伤图标（在有效时间内）
     */
    public boolean isInjuryVisible(LimbType limbType) {
        Long injuryTime = limbInjuryTimes.get(limbType.name());
        if (injuryTime == null) {
            return false;
        }
        long timeSinceInjury = System.currentTimeMillis() - injuryTime;
        return timeSinceInjury < INJURY_ICON_DURATION;
    }

    /**
     * 清理过期的受伤记录
     */
    public void cleanupExpiredInjuries() {
        long currentTime = System.currentTimeMillis();
        limbInjuryTimes.entrySet().removeIf(entry ->
            currentTime - entry.getValue() >= INJURY_ICON_DURATION
        );
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public Map<String, Long> getLimbInjuryTimes() {
        return limbInjuryTimes;
    }

    public void setLimbInjuryTimes(Map<String, Long> limbInjuryTimes) {
        this.limbInjuryTimes = limbInjuryTimes;
    }
}
