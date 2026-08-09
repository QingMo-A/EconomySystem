package com.mo.economy_system.core.economy_system.shop;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.economy.ShopPricingPolicy;
import com.mo.economy_system.common.economy.ShopItemIdentity;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.utils.ItemStackDataHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.UUID;

public class ShopItem {
    private String shopItemId;
    private final String itemId;       // 物品 ID
    private final String description; // 商品描述
    private final int basePrice;       // 初始价格
    private int currentPrice;          // 当前价格
    private int lastPrice;             // 上次的价格
    private double fluctuationFactor; // 涨幅系数（用于动态调整价格）
    private final String nbt;
    private final String itemData;
    private int recentDemand;
    private int virtualStock;
    private int maxVirtualStock;

    public ShopItem(String itemId, int basePrice, String description) {
        this(UUID.randomUUID().toString(), itemId, basePrice, description, null, null);
    }

    public ShopItem(String itemId, int basePrice, String description, String nbt) {
        this(UUID.randomUUID().toString(), itemId, basePrice, description, nbt, null);
    }

    public ShopItem(String itemId, int basePrice, String description, String nbt, String itemData) {
        this(UUID.randomUUID().toString(), itemId, basePrice, description, nbt, itemData);
    }

    public ShopItem(String shopItemId, String itemId, int basePrice, String description, String nbt, String itemData) {
        this.shopItemId = normalizeShopItemId(shopItemId, itemId, nbt, itemData);
        this.itemId = itemId;
        this.basePrice = basePrice;
        this.currentPrice = basePrice; // 初始化时当前价格等于基础价格
        this.lastPrice = basePrice;   // 上次的价格初始化为基础价格
        this.description = description;
        this.fluctuationFactor = 1.0;  // 默认涨幅系数为 1.0（无变化）
        this.nbt = nbt;
        this.itemData = itemData;
        this.recentDemand = 0;
        this.maxVirtualStock = 0;
        this.virtualStock = 0;
    }

    private static String normalizeShopItemId(
            String shopItemId, String itemId, String nbt, String itemData) {
        return ShopItemIdentity.existingOrDeterministic(shopItemId, itemId, nbt, itemData);
    }

    public String getShopItemId() {
        if (shopItemId == null || shopItemId.isBlank()) {
            shopItemId = normalizeShopItemId(shopItemId, itemId, nbt, itemData);
        }
        return shopItemId;
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

    public String getItemData() { return itemData; }

    public int getRecentDemand() { return recentDemand; }

    public int getVirtualStock() { return virtualStock; }

    public int getMaxVirtualStock() { return maxVirtualStock; }

    public ShopItemSnapshot toBridgeSnapshot() {
        return new ShopItemSnapshot(
                getShopItemId(), itemId, basePrice, currentPrice, lastPrice, description,
                fluctuationFactor, nbt, itemData, recentDemand, virtualStock, maxVirtualStock
        );
    }

    public static ShopItem fromBridgeSnapshot(ShopItemSnapshot snapshot) {
        String legacyNbt = snapshot.nbt().isBlank() ? null : snapshot.nbt();
        String fullItemData = snapshot.itemData().isBlank() ? null : snapshot.itemData();
        ShopItem item = new ShopItem(
                snapshot.shopItemId(), snapshot.itemId(), snapshot.basePrice(),
                snapshot.description(), legacyNbt, fullItemData
        );
        item.currentPrice = snapshot.currentPrice();
        item.lastPrice = snapshot.lastPrice();
        item.fluctuationFactor = snapshot.fluctuationFactor();
        item.recentDemand = snapshot.recentDemand();
        item.virtualStock = snapshot.virtualStock();
        item.maxVirtualStock = snapshot.maxVirtualStock();
        return item;
    }

    public void applyPricingSnapshot(ShopItemSnapshot snapshot) {
        if (!getShopItemId().equals(snapshot.shopItemId()) || !itemId.equals(snapshot.itemId())
                || basePrice != snapshot.basePrice()) {
            throw new IllegalArgumentException("pricing snapshot does not match shop item identity");
        }
        currentPrice = snapshot.currentPrice();
        lastPrice = snapshot.lastPrice();
        fluctuationFactor = snapshot.fluctuationFactor();
        recentDemand = snapshot.recentDemand();
        virtualStock = snapshot.virtualStock();
        maxVirtualStock = snapshot.maxVirtualStock();
    }

    public ItemStack getItemStack() {
        return getItemStack(null);
    }

    public ItemStack getItemStack(RegistryAccess registryAccess) {
        if (registryAccess != null && itemData != null && !itemData.isBlank() && !"null".equals(itemData)) {
            try {
                ItemStack stack = ItemStackDataHelper.loadFull(itemData, registryAccess);
                if (!stack.isEmpty()) {
                    return stack;
                }
            } catch (Exception e) {
                EconomySystem.LOGGER.warn("Failed to load shop itemData for {}: {}", itemId, e.getMessage());
                return ItemStack.EMPTY;
            }
        }

        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);

        // 如果有自定义 NBT，则解析并写入
        if (nbt != null && !nbt.isBlank() && !"null".equals(nbt)) {
            stack = registryAccess == null ? applyEnchantmentNBT(stack, nbt) : applyShopNBT(stack, nbt, registryAccess);
            if (stack == null) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public static ItemStack applyEnchantmentNBT(ItemStack itemStack, String nbtString) {
        // 解析NBT字符串
        CompoundTag userNbt;
        try {
            userNbt = TagParser.parseTag(nbtString);
        } catch (CommandSyntaxException e) {
            EconomySystem.LOGGER.warn("NBT格式错误: {}", e.getMessage());
            return null;
        }

        // 应用NBT
        if (userNbt != null) {
            ItemStackDataHelper.setTag(itemStack, userNbt);
        }

        return itemStack;
    }

    private static ItemStack applyShopNBT(ItemStack itemStack, String nbtString, RegistryAccess registryAccess) {
        CompoundTag userNbt;
        try {
            userNbt = TagParser.parseTag(nbtString);
        } catch (CommandSyntaxException e) {
            EconomySystem.LOGGER.warn("NBT格式错误: {}", e.getMessage());
            return ItemStack.EMPTY;
        }

        if (userNbt.contains("StoredEnchantments", Tag.TAG_LIST)) {
            applyStoredEnchantmentsComponent(itemStack, userNbt.getList("StoredEnchantments", Tag.TAG_COMPOUND), registryAccess);
            userNbt.remove("StoredEnchantments");
        }
        if (userNbt.contains("Enchantments", Tag.TAG_LIST)) {
            applyEnchantmentsComponent(itemStack, userNbt.getList("Enchantments", Tag.TAG_COMPOUND), registryAccess);
            userNbt.remove("Enchantments");
        }
        if (!userNbt.isEmpty()) {
            ItemStackDataHelper.setTag(itemStack, userNbt);
        }
        return itemStack;
    }

    private static void applyStoredEnchantmentsComponent(ItemStack stack, ListTag enchantmentList, RegistryAccess registryAccess) {
        ItemEnchantments enchantments = buildEnchantments(enchantmentList, registryAccess);
        if (!enchantments.isEmpty()) {
            stack.set(DataComponents.STORED_ENCHANTMENTS, enchantments);
        }
    }

    private static void applyEnchantmentsComponent(ItemStack stack, ListTag enchantmentList, RegistryAccess registryAccess) {
        ItemEnchantments enchantments = buildEnchantments(enchantmentList, registryAccess);
        if (!enchantments.isEmpty()) {
            stack.set(DataComponents.ENCHANTMENTS, enchantments);
        }
    }

    private static ItemEnchantments buildEnchantments(ListTag enchantmentList, RegistryAccess registryAccess) {
        Registry<Enchantment> registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (int i = 0; i < enchantmentList.size(); i++) {
            CompoundTag enchantmentTag = enchantmentList.getCompound(i);
            String id = enchantmentTag.getString("id");
            int level = enchantmentTag.contains("lvl", Tag.TAG_ANY_NUMERIC) ? enchantmentTag.getInt("lvl") : 1;
            try {
                ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id));
                Holder.Reference<Enchantment> holder = registry.getHolder(key).orElse(null);
                if (holder == null) {
                    EconomySystem.LOGGER.warn("Unknown shop enchantment id {}, skipped", id);
                    continue;
                }
                mutable.set(holder, Math.max(1, level));
            } catch (Exception e) {
                EconomySystem.LOGGER.warn("Invalid shop enchantment id {}, skipped: {}", id, e.getMessage());
            }
        }
        return mutable.toImmutable();
    }

    // 保存到 NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("shopItemId", getShopItemId());
        tag.putString("itemId", itemId);
        tag.putInt("basePrice", basePrice);
        tag.putInt("currentPrice", currentPrice);
        tag.putInt("lastPrice", lastPrice);  // 保存上次的价格
        tag.putString("description", description);
        tag.putDouble("fluctuationFactor", fluctuationFactor);
        tag.putInt("recentDemand", recentDemand);
        tag.putInt("virtualStock", virtualStock);
        tag.putInt("maxVirtualStock", maxVirtualStock);

        // **保存 nbtData**（如果有的话）
        if (nbt != null) {
            tag.putString("nbt", nbt);
        }
        if (itemData != null) {
            tag.putString("itemData", itemData);
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
        String itemData = tag.getString("itemData");
        String shopItemId = tag.getString("shopItemId");

        // 创建 ShopItem
        ShopItem shopItem = new ShopItem(shopItemId, itemId, basePrice, description, nbtData, itemData);

        // 其他可写回对象的字段
        shopItem.setCurrentPrice(tag.getInt("currentPrice"));
        shopItem.lastPrice = tag.getInt("lastPrice");
        shopItem.setFluctuationFactor(tag.getDouble("fluctuationFactor"));
        shopItem.recentDemand = tag.getInt("recentDemand");
        shopItem.virtualStock = tag.getInt("virtualStock");
        shopItem.maxVirtualStock = tag.getInt("maxVirtualStock");
        shopItem.applyPricingSnapshot(ShopPricingPolicy.initialize(
            shopItem.toBridgeSnapshot(), ShopPricingConfig.get().toPolicyConfig()));

        return shopItem;
    }
}
