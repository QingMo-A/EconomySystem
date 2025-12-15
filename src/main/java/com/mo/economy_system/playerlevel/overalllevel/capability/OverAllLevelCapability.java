//package com.mo.economy_system.playerlevel.overalllevel.capability;
//
//import net.minecraft.nbt.CompoundTag;
//
//public class OverAllLevelCapability implements IOverAllLevelCapability {
//    //初始等级为 0
//    private int currentLevel = 0;
//
//    @Override
//    public int getLevel() {
//        return currentLevel;
//    }
//
//    @Override
//    public void setLevel(int level) {
//        this.currentLevel = level;
//    }
//
//    @Override
//    public CompoundTag serializeNBT() {
//        CompoundTag nbt = new CompoundTag();
//        nbt.putInt("overallLevel", currentLevel);
//        return nbt;
//    }
//
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        // 从NBT读取等级，默认0
//        currentLevel = nbt.getInt("overallLevel");
//    }
//}