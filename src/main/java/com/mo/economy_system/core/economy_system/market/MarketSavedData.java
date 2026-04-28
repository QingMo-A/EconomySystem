package com.mo.economy_system.core.economy_system.market;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class MarketSavedData extends SavedData {
    private static final String DATA_NAME = "market_data";
    private final List<MarketItem> marketItems = new ArrayList<>();

    public List<MarketItem> getMarketItems() { return new ArrayList<>(marketItems); }

    public void addMarketItem(MarketItem item) {
        marketItems.add(item);
        setDirty();
    }

    public void removeMarketItem(MarketItem item) {
        marketItems.remove(item);
        setDirty();
    }

    public void clearMarketItems() {
        marketItems.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (MarketItem item : marketItems) {
            listTag.add(item.toNBT());
        }
        tag.put("marketItems", listTag);
        return tag;
    }

    public static MarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketSavedData data = new MarketSavedData();
        if (tag.contains("marketItems")) {
            ListTag listTag = tag.getList("marketItems", CompoundTag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                MarketItem item = MarketItem.fromNBT(itemTag); // 动态创建子类对象
                data.marketItems.add(item);
            }
        }
        return data;
    }

    public static MarketSavedData getInstance(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MarketSavedData::new, MarketSavedData::load), DATA_NAME);
    }
}
