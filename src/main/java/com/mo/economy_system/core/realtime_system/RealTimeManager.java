package com.mo.economy_system.core.realtime_system;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 实时时间管理器
 * 管理现实时间同步系统的开关和时间计算
 */
public class RealTimeManager {

    private static final ZoneId EAST_8_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long DAY_LENGTH = 24000L;
    private static final float VANILLA_DAY_TICKS_PER_SERVER_TICK = 1.0F;
    private static final float REAL_DAY_TICKS_PER_SERVER_TICK = (float) ((double) DAY_LENGTH / 86400.0D / 20.0D);
    private static final Set<ServerLevel> INITIALIZED_LEVELS = Collections.newSetFromMap(new WeakHashMap<>());

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
        applyDayTimeSpeed(level, enabled);
        if (enabled) {
            alignToRealTimeOnce(level);
        }
    }

    /**
     * 切换实时时间系统状态
     */
    public static boolean toggle(ServerLevel level) {
        RealTimeSavedData data = RealTimeSavedData.getInstance(level);
        data.toggle();
        applyDayTimeSpeed(level, data.isEnabled());
        if (data.isEnabled()) {
            alignToRealTimeOnce(level);
        }
        return data.isEnabled();
    }

    /**
     * Uses Minecraft's dayTimePerTick clock instead of repeatedly setting
     * dayTime. This keeps gameTime and server tick logic vanilla, while the
     * sky clock moves at real-world day speed without visual pullback.
     */
    public static void tickDayTime(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        boolean enabled = isEnabled(level);
        applyDayTimeSpeed(level, enabled);
        if (enabled && INITIALIZED_LEVELS.add(level)) {
            alignToRealTimeOnce(level);
        }
    }

    private static void applyDayTimeSpeed(ServerLevel level, boolean realtimeEnabled) {
        float desiredSpeed = realtimeEnabled ? REAL_DAY_TICKS_PER_SERVER_TICK : VANILLA_DAY_TICKS_PER_SERVER_TICK;
        if (Math.abs(level.getDayTimePerTick() - desiredSpeed) > 0.000001F) {
            level.setDayTimePerTick(desiredSpeed);
        }
        if (!realtimeEnabled) {
            INITIALIZED_LEVELS.remove(level);
        }
    }

    private static void alignToRealTimeOnce(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        level.setDayTime(getRealWorldTimeInGameTicks());
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
