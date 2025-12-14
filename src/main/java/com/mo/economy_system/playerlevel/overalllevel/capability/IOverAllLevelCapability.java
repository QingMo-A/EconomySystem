package com.mo.economy_system.playerlevel.overalllevel.capability;

import net.minecraft.nbt.CompoundTag;

public interface IOverAllLevelCapability {
    // 获取玩家总等级
    int getLevel();
    // 设置玩家总等级
    void setLevel(int level);
    // NBT序列化
    CompoundTag serializeNBT();
    // NBT反序列化
    void deserializeNBT(CompoundTag nbt);
}