package com.mo.economy_system.server.chattitle.capability;

import com.mo.economy_system.server.chattitle.Title;
import net.minecraft.nbt.CompoundTag;

public interface ITitleCapability {
    Title getTitle();
    void setTitle(Title title);

    // NBT序列化
    CompoundTag serializeNBT();
    // NBT反序列化
    void deserializeNBT(CompoundTag nbt);
}
