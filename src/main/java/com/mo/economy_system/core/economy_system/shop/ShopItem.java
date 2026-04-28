package com.mo.economy_system.core.economy_system.shop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ShopItem {
    private final String itemId;       // 物品 ID
    private final String description; // 商品描述
    private final int basePrice;       // 初始价格
    private int currentPrice;          // 当前价格
    private int lastPrice;             // 上次的价格
    private double fluctuationFactor; // 涨幅系数（用于动态调整价格）
    private final String nbt;

    public ShopItem(String itemId, int basePrice, String description) {
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = basePrice; // 初始化时当前价格等于基础价格
        this.lastPrice = basePrice;   // 上次的价格初始化为基础价格
        this.description = description;
        this.fluctuationFactor = 1.0;  // 默认涨幅系数为 1.0（无变化）
        this.nbt = null;
    }

    public ShopItem(String itemId, int basePrice, String description, String nbt) {
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = basePrice; // 初始化时当前价格等于基础价格
        this.lastPrice = basePrice;   // 上次的价格初始化为基础价格
        this.description = description;
        this.fluctuationFactor = 1.0;  // 默认涨幅系数为 1.0（无变化）
        this.nbt = nbt;
    }

    public String getItemId() {
        return itemId;
    }

    public int getBasePrice() {
        return basePrice;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public String getDescription() {
        return description;
    }

    public double getFluctuationFactor() {
        return fluctuationFactor;
    }

    public int getLastPrice() {
        return lastPrice;
    }

    public void setCurrentPrice(int currentPrice) {
        this.lastPrice = this.currentPrice; // 在更新当前价格之前，先将当前价格保存为上次的价格
        this.currentPrice = currentPrice;
    }

    public void setFluctuationFactor(double fluctuationFactor) {
        this.fluctuationFactor = fluctuationFactor;
    }

    public String getNbt() { return nbt; }

    public ItemStack getItemStack() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);

        // 如果有自定义 NBT，则解析并写入
        if (nbt != null && !nbt.isEmpty() && nbt != "null") {
            stack = applyEnchantmentNBT(stack, nbt);
        }
        return stack;
    }

    public static ItemStack applyEnchantmentNBT(ItemStack itemStack, String nbtString) {
        // 解析NBT字符串
        CompoundTag userNbt;
        try {
            userNbt = TagParser.parseTag(nbtString);
        } catch (CommandSyntaxException e) {
            System.err.println("NBT格式错误: " + e.getMessage());
            return null;
        }

        // 应用NBT
        if (userNbt != null) {
            com.mo.economy_system.utils.ItemStackCompat.setTag(itemStack, userNbt);
        }

        return itemStack;
    }

    // 保存到 NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("itemId", itemId);
        tag.putInt("basePrice", basePrice);
        tag.putInt("currentPrice", currentPrice);
        tag.putInt("lastPrice", lastPrice);  // 保存上次的价格
        tag.putString("description", description);
        tag.putDouble("fluctuationFactor", fluctuationFactor);

        // **保存 nbtData**（如果有的话）
        if (nbt != null) {
            tag.putString("nbt", nbt);
        }
        return tag;
    }

    // 从 NBT 加载
    public static ShopItem fromNBT(CompoundTag tag) {
        // 先取出必要字段
        String itemId = tag.getString("itemId");
        int basePrice = tag.getInt("basePrice");
        String description = tag.getString("description");
        String nbtData = tag.getString("nbt");  // 可能为空

        // 创建 ShopItem
        ShopItem shopItem = new ShopItem(itemId, basePrice, description, nbtData);

        // 其他可写回对象的字段
        shopItem.setCurrentPrice(tag.getInt("currentPrice"));
        shopItem.lastPrice = tag.getInt("lastPrice");
        shopItem.setFluctuationFactor(tag.getDouble("fluctuationFactor"));

        return shopItem;
    }
}
