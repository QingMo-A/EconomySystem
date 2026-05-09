package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.utils.ItemStackDataHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public abstract class MarketItem {
    protected final UUID tradeID;
    protected final String itemID;
    protected final ItemStack itemStack;
    protected final int basePrice;
    protected final String sellerName;
    protected final UUID sellerID;
    protected final long listingTime;
    protected final long expirationTime; // 商品过期时间（以毫秒为单位）

    // 假设过期时间为上架后 3天
    private static final long EXPIRATION_DURATION = 3 * 24 * 60 * 60 * 1000L; // 3天
    // 假设过期时间为上架后 1 分钟
    // private static final long EXPIRATION_DURATION = 1 * 60 * 1000L; // 1分钟


    public MarketItem(UUID tradeID, String itemID, ItemStack itemStack, int basePrice, String sellerName, UUID sellerID, long listingTime) {
        this.tradeID = tradeID;
        this.itemID = itemID;
        this.itemStack = itemStack.copy();
        this.basePrice = basePrice;
        this.sellerName = sellerName;
        this.sellerID = sellerID;
        this.listingTime = listingTime;
        this.expirationTime = listingTime + EXPIRATION_DURATION; // 设置过期时间
    }

    // 公共方法（Getters）...

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", this.getClass().getName()); // 关键：保存子类类型
        tag.putUUID("tradeID", tradeID);
        tag.putString("itemID", itemID);
        tag.put("itemStack", ItemStackDataHelper.saveSimple(itemStack));
        tag.putInt("basePrice", basePrice);
        tag.putString("sellerName", sellerName);
        tag.putUUID("sellerID", sellerID);
        tag.putLong("listingTime", listingTime);
        tag.putLong("expirationTime", expirationTime); // 保存过期时间
        return tag;
    }

    public static MarketItem fromNBT(CompoundTag tag) {
        String type = tag.getString("type");
        try {
            return switch (type) { // 根据类型分发到子类
                case "com.mo.economy_system.core.economy_system.market.SalesOrder" -> SalesOrder.fromNBT(tag);
                case "com.mo.economy_system.core.economy_system.market.DemandOrder" -> DemandOrder.fromNBT(tag);
                default -> throw new IllegalArgumentException("未知的 MarketItem 类型: " + type);
            };
        } catch (Exception e) {
            throw new RuntimeException("反序列化 MarketItem 失败", e);
        }
    }

    // 判断商品是否过期
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    // Getters...
    public UUID getTradeID() { return tradeID; }
    public String getItemID() { return itemID; }
    public ItemStack getItemStack() { return itemStack.copy(); }
    public int getBasePrice() { return basePrice; }
    public String getSellerName() { return sellerName; }
    public UUID getSellerID() { return sellerID; }
    public long getListingTime() { return listingTime; }
    public long getExpirationTime() { return expirationTime; }
}
