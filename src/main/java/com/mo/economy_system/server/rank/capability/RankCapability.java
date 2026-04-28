//package com.mo.economy_system.server.rank.capability;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.server.rank.Rank;
//import com.mo.economy_system.server.rank.RankRegistry;
//import net.minecraft.nbt.CompoundTag;
//
//public class RankCapability implements IRankCapability {
//    // 默认无Rank
//    private Rank currentRank = RankRegistry.NO_RANK;
//
//    @Override
//    public Rank getRank() {
//        return currentRank;
//    }
//
//    @Override
//    public void setRank(Rank rank) {
//        this.currentRank = rank;
//    }
//
//    // NBT序列化：将Rank的ID和等级写入NBT
//    @Override
//    public CompoundTag serializeNBT() {
//        CompoundTag nbt = new CompoundTag();
//        nbt.putString("rankId", currentRank.getRankName());
//        nbt.putInt("rankLevel", currentRank.getRankLevel());
//        EconomySystem.LOGGER.info("Rank序列化: rankId={}, rankLevel={}", currentRank.getRankName(), currentRank.getRankLevel());
//        return nbt;
//    }
//
//    // NBT反序列化：从NBT恢复Rank
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        if (nbt.contains("rankId")) {
//            String rankId = nbt.getString("rankId");
//            currentRank = RankRegistry.getRankByName(rankId);  // 优先 name
//            EconomySystem.LOGGER.info("Rank反序列化 (by name): rankId={}", rankId);
//        } else if (nbt.contains("rankLevel")) {
//            int rankLevel = nbt.getInt("rankLevel");
//            currentRank = RankRegistry.getRankByLevel(rankLevel);  // fallback
//            EconomySystem.LOGGER.info("Rank反序列化 (by level): rankLevel={}", rankLevel);
//        } else {
//            currentRank = RankRegistry.NO_RANK;
//            EconomySystem.LOGGER.info("Rank反序列化: 无数据，默认 NO_RANK");
//        }
//    }
//}