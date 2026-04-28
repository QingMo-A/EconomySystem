package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.utils.ItemStackCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class SalesOrder extends MarketItem {
    public SalesOrder(UUID tradeID, String itemID, ItemStack itemStack, int basePrice, String sellerName, UUID sellerID, long listingTime) {
        super(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime);
    }

    @Override
    public CompoundTag toNBT() {
        return super.toNBT(); // 复用父类逻辑（已包含 type 字段）
    }

    public static SalesOrder fromNBT(CompoundTag tag) {
        UUID tradeID = tag.getUUID("tradeID");
        String itemID = tag.getString("itemID");
        ItemStack itemStack = ItemStackCompat.loadSimple(tag.getCompound("itemStack"));
        int basePrice = tag.getInt("basePrice");
        String sellerName = tag.getString("sellerName");
        UUID sellerID = tag.getUUID("sellerID");
        long listingTime = tag.getLong("listingTime");
        return new SalesOrder(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime);
    }
}
