package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.utils.ItemStackDataHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class SalesOrder extends MarketItem {
    public SalesOrder(UUID tradeID, String itemID, ItemStack itemStack, int basePrice, String sellerName, UUID sellerID, long listingTime) {
        super(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime);
    }

    public SalesOrder(UUID tradeID, String itemID, ItemStack itemStack, int basePrice, String sellerName, UUID sellerID,
                      long listingTime, long expirationTime) {
        super(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime, expirationTime);
    }

    @Override
    public CompoundTag toNBT() {
        return super.toNBT(); // 复用父类逻辑（已包含 type 字段）
    }

    public static SalesOrder fromNBT(CompoundTag tag) {
        return fromNBT(tag, null);
    }

    public static SalesOrder fromNBT(CompoundTag tag, HolderLookup.Provider registries) {
        UUID tradeID = tag.getUUID("tradeID");
        String itemID = tag.getString("itemID");
        ItemStack itemStack = registries == null
                ? ItemStackDataHelper.loadSimple(tag.getCompound("itemStack"))
                : ItemStackDataHelper.loadFullTag(tag.getCompound("itemStack"), registries);
        if (tag.contains("listedCount")) {
            itemStack.setCount(Math.max(1, tag.getInt("listedCount")));
        }
        int basePrice = tag.getInt("basePrice");
        String sellerName = tag.getString("sellerName");
        UUID sellerID = tag.getUUID("sellerID");
        long listingTime = tag.getLong("listingTime");
        long expirationTime = tag.contains("expirationTime") ? tag.getLong("expirationTime") : listingTime + 3L * 24L * 60L * 60L * 1000L;
        return new SalesOrder(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime, expirationTime);
    }
}
