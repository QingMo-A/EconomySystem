package com.mo.economy_system.server.chattitle.capability;

import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleConfig;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import net.minecraft.nbt.CompoundTag;

public class TitleCapability implements ITitleCapability {
    private Title currentTitle = TitleRegistry.getDefaultTitle();

    @Override
    public Title getTitle() {
        return currentTitle; // 返回当前称号
    }

    @Override
    public void setTitle(Title title) {
        this.currentTitle = title; // 设置新称号
    }

    @Override
    // 把称号ID存到NBT
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("titleId", currentTitle.getTitleID());
        return nbt;
    }

    // 从NBT读取ID，再通过TitleRegistry获取对应的称号
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("titleId")) {
            int titleId = nbt.getInt("titleId");
            currentTitle = TitleRegistry.getTitleById(titleId);
        } else {
            // 如果没有数据，用默认称号
            currentTitle = TitleRegistry.getDefaultTitle();
        }
    }


}
