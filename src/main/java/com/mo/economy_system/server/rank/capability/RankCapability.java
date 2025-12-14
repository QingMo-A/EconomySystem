package com.mo.economy_system.server.rank.capability;

import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.nbt.CompoundTag;

public class RankCapability implements IRankCapability {
    // 默认无Rank
    private Rank currentRank = RankRegistry.NO_RANK;

    @Override
    public Rank getRank() {
        return currentRank;
    }

    @Override
    public void setRank(Rank rank) {
        this.currentRank = rank;
    }

    // NBT序列化：将Rank的ID和等级写入NBT
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("rankId", currentRank.getRankName());
        nbt.putInt("rankLevel", currentRank.getRankLevel());
        return nbt;
    }

    // NBT反序列化：从NBT恢复Rank
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("rankId") && nbt.contains("rankLevel")) {
            String rankId = nbt.getString("rankId");
            int rankLevel = nbt.getInt("rankLevel");
            // 从注册表获取匹配的Rank（避免无效Rank）
            if (rankLevel == 0) {
                currentRank = RankRegistry.NO_RANK;
            } else if (rankLevel == 1) {
                currentRank = RankRegistry.FISH;
            } else if (rankLevel == 2) {
                currentRank = RankRegistry.FISH_PLUS;
            } else if (rankLevel == 3) {
                currentRank = RankRegistry.FISH_PLUS_PLUS;
            } else if (rankLevel == 4) {
                currentRank = RankRegistry.OPERATOR;
            }
        } else {
            currentRank = RankRegistry.NO_RANK;
        }
    }
}