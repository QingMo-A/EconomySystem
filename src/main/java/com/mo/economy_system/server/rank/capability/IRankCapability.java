package com.mo.economy_system.server.rank.capability;

import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.nbt.CompoundTag;

public interface IRankCapability {
    Rank getRank();
    void setRank(Rank rank);
    default void clearRank() {
        setRank(RankRegistry.NO_RANK);
    }
    // NBT序列化
    CompoundTag serializeNBT();
    // NBT反序列化
    void deserializeNBT(CompoundTag nbt);
}