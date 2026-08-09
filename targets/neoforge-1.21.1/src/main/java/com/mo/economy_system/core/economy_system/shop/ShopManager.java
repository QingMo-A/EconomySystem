package com.mo.economy_system.core.economy_system.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.economy.ShopCatalogDefaults;
import com.mo.economy_system.common.economy.ShopPricingPolicy;
import com.mo.economy_system.common.settings.CommonSettingsStore;
import com.mo.economy_system.common.settings.EconomySettings;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ShopManager {
    public static final File CONFIG_FILE = EconomyServices.platform()
            .configDirectory()
            .resolve(EconomySystem.MODID)
            .resolve("economy_shop.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()  // 启用格式化
            .disableHtmlEscaping() // 可选：禁用 HTML 转义（如保留 &、< 等符号）
            .create();
    private final List<ShopItem> items = new ArrayList<>();

    public ShopManager() {
        loadFromConfig();
    }

    public synchronized List<ShopItem> getItems() {
        return new ArrayList<>(items); // 返回副本以保护内部列表
    }

    public synchronized ShopItem findByShopItemId(String shopItemId) {
        for (ShopItem item : items) {
            if (item.getShopItemId().equals(shopItemId)) {
                return item;
            }
        }
        return null;
    }

    public synchronized void addItem(ShopItem item) {
        items.add(item);
        saveToConfig();
    }

    public synchronized ShopItem addItemFromStack(ItemStack stack, int basePrice, String description, RegistryAccess registryAccess) {
        ItemStack savedStack = stack.copy();
        savedStack.setCount(1);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(savedStack.getItem());
        String itemData = com.mo.economy_system.utils.ItemStackDataHelper.saveFull(savedStack, registryAccess);
        ShopItem shopItem = new ShopItem(itemId.toString(), basePrice, description, null, itemData);
        addItem(shopItem);
        return shopItem;
    }

    public synchronized void loadFromConfig() {
        if (!CONFIG_FILE.exists()) {
            saveDefaultConfig();
        }

        try (FileInputStream fis = new FileInputStream(CONFIG_FILE);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<ShopItem>>() {}.getType();
            List<ShopItem> loadedItems = GSON.fromJson(isr, listType);
            if (loadedItems != null) {
                items.clear();
                items.addAll(loadedItems);
                ensurePricingState();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("Failed to load shop config {}", CONFIG_FILE, e);
        } catch (RuntimeException e) {
            EconomySystem.LOGGER.error("Failed to parse shop config {}, keeping previous shop items", CONFIG_FILE, e);
        }
    }

    public synchronized void saveToConfig() {
        ensureConfigDirectory();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(items, osw);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("Failed to save shop config {}", CONFIG_FILE, e);
        }
    }

    private static void ensureConfigDirectory() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }


    private void saveDefaultConfig() {
        items.clear();
        items.addAll(ShopCatalogDefaults.snapshots().stream()
                .map(ShopItem::fromBridgeSnapshot)
                .toList());
        saveToConfig();
    }

    public void adjustPrices() {
        ShopPricingConfig config = ShopPricingConfig.get();
        ShopPricingPolicy.Mode mode = ShopPricingPolicy.Mode.parse(
            EconomySettings.get(CommonSettingsStore.SHOP_PRICING_MODE));
        for (ShopItem item : items) {
            if (item.getBasePrice() <= 0) {
                EconomySystem.LOGGER.warn("Skip price adjustment for {} because basePrice is {}",
                    item.getItemId(), item.getBasePrice());
                continue;
            }
            item.applyPricingSnapshot(ShopPricingPolicy.adjust(
                item.toBridgeSnapshot(), config.toPolicyConfig(), mode));
        }

        saveToConfig();
    }

    public synchronized void recordPurchase(ShopItem item, int quantity) {
        if (item == null || quantity <= 0) {
            return;
        }
        item.applyPricingSnapshot(ShopPricingPolicy.recordPurchase(
            item.toBridgeSnapshot(), quantity, ShopPricingConfig.get().toPolicyConfig()));
        saveToConfig();
    }

    public synchronized void reloadPricingConfig() {
        ShopPricingConfig.reload();
        ensurePricingState();
        saveToConfig();
    }

    private void ensurePricingState() {
        ShopPricingConfig config = ShopPricingConfig.get();
        for (ShopItem item : items) {
            item.applyPricingSnapshot(ShopPricingPolicy.initialize(
                item.toBridgeSnapshot(), config.toPolicyConfig()));
        }
    }
}
