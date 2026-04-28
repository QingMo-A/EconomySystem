package com.mo.economy_system.core.realtime_system;

import net.minecraft.server.level.ServerLevel;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 实时时间管理器
 * 管理现实时间同步系统的开关和时间计算
 */
public class RealTimeManager {

    private static final ZoneId EAST_8_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 检查实时时间系统是否启用
     */
    public static boolean isEnabled(ServerLevel level) {
        try {
            RealTimeSavedData data = RealTimeSavedData.getInstance(level);
            return data.isEnabled();
        } catch (Exception e) {
            return true; // 默认开启
        }
    }

    /**
     * 设置实时时间系统启用状态
     */
    public static void setEnabled(ServerLevel level, boolean enabled) {
        RealTimeSavedData data = RealTimeSavedData.getInstance(level);
        data.setEnabled(enabled);
    }

    /**
     * 切换实时时间系统状态
     */
    public static boolean toggle(ServerLevel level) {
        RealTimeSavedData data = RealTimeSavedData.getInstance(level);
        data.toggle();
        return data.isEnabled();
    }

    /**
     * 获取东八区当前时间对应的游戏时间（以 tick 为单位）
     */
    public static long getRealWorldTimeInGameTicks() {
        ZonedDateTime now = ZonedDateTime.now(EAST_8_ZONE);
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        // 现实时间下午2点，映射为游戏内的白天时间段
        long gameTime;

        if (hour >= 6 && hour < 18) {
            // 白天：游戏时间映射到白天时段 0-12000
            gameTime = (hour - 6) * 1000 + (minute * 1000 / 60) + (second * 1000 / 3600);
            gameTime = gameTime % 12000;
        } else {
            // 夜晚：游戏时间映射到夜晚时段 12000-24000
            gameTime = ((hour - 18) + 24) * 1000 + (minute * 1000 / 60) + (second * 1000 / 3600);
            gameTime = (12000 + gameTime % 12000) % 24000;
        }

        return gameTime;
    }
}
