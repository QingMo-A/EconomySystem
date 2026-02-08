package com.mo.economy_system.core.realtime_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 实时时间系统保存数据
 * 用于存储实时时间系统的开关状态
 */
public class RealTimeSavedData extends SavedData {
    private static final String DATA_NAME = "realtime_data";
    private boolean enabled = true; // 默认开启

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.setDirty();
    }

    public void toggle() {
        this.enabled = !this.enabled;
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("enabled", enabled);
        return tag;
    }

    public static RealTimeSavedData load(CompoundTag tag) {
        RealTimeSavedData data = new RealTimeSavedData();
        if (tag.contains("enabled")) {
            data.enabled = tag.getBoolean("enabled");
        }
        return data;
    }

    public static RealTimeSavedData getInstance(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(RealTimeSavedData::load, RealTimeSavedData::new, DATA_NAME);
    }
}
