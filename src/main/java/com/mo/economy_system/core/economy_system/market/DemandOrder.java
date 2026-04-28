package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.utils.ItemStackCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class DemandOrder extends MarketItem {
    private boolean delivered;

    public DemandOrder(UUID tradeID, String itemID, ItemStack itemStack, int basePrice, String sellerName, UUID sellerID, long listingTime, boolean delivered) {
        super(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime);
        this.delivered = delivered;
    }

    @Override
    public CompoundTag toNBT() {
        CompoundTag tag = super.toNBT(); // 复用父类逻辑
        tag.putBoolean("delivered", delivered); // 添加子类字段
        return tag;
    }

    public static DemandOrder fromNBT(CompoundTag tag) {
        UUID tradeID = tag.getUUID("tradeID");
        String itemID = tag.getString("itemID");
        ItemStack itemStack = ItemStackCompat.loadSimple(tag.getCompound("itemStack"));
        int basePrice = tag.getInt("basePrice");
        String sellerName = tag.getString("sellerName");
        UUID sellerID = tag.getUUID("sellerID");
        long listingTime = tag.getLong("listingTime");
        boolean delivered = tag.getBoolean("delivered");
        return new DemandOrder(tradeID, itemID, itemStack, basePrice, sellerName, sellerID, listingTime, delivered);
    }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
}
