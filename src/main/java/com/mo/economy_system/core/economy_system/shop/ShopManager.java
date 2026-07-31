package com.mo.economy_system.core.economy_system.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.settings.GameSettingsManager;
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
        items.add(new ShopItem("economy_system:recall_potion", 5, "回忆药水"));
        items.add(new ShopItem("economy_system:wormhole_potion", 10, "虫洞药水"));

        items.add(new ShopItem("minecraft:enchanted_book", 100, "细心 I", "{StoredEnchantments:[{id:\"economy_system:carefully\", lvl:1}]}"));
        items.add(new ShopItem("minecraft:enchanted_book", 200, "赏金猎人 I", "{StoredEnchantments:[{id:\"economy_system:bounty_hunter\", lvl:1}]}"));

        items.add(new ShopItem("minecraft:dirt", 5, "泥土"));
        items.add(new ShopItem("minecraft:grass_block", 5, "草方块"));
        items.add(new ShopItem("minecraft:sand", 5, "沙子"));
        items.add(new ShopItem("minecraft:stone", 5, "石头"));

        // 原木
        items.add(new ShopItem("minecraft:oak_log", 5, "橡木原木"));
        items.add(new ShopItem("minecraft:spruce_log", 5, "云杉原木"));
        items.add(new ShopItem("minecraft:birch_log", 5, "白桦原木"));
        items.add(new ShopItem("minecraft:jungle_log", 5, "丛林原木"));
        items.add(new ShopItem("minecraft:acacia_log", 5, "金合欢原木"));
        items.add(new ShopItem("minecraft:dark_oak_log", 5, "深色橡木原木"));
        items.add(new ShopItem("minecraft:mangrove_log", 5, "红树林原木"));
        items.add(new ShopItem("minecraft:cherry_log", 5, "樱花原木")); // 1.20 新增
        items.add(new ShopItem("minecraft:crimson_stem", 5, "绯红菌柄")); // 下界
        items.add(new ShopItem("minecraft:warped_stem", 5, "诡异菌柄")); // 下界

        // 树苗
        items.add(new ShopItem("minecraft:oak_sapling", 5, "橡树树苗"));
        items.add(new ShopItem("minecraft:spruce_sapling", 5, "云杉树苗"));
        items.add(new ShopItem("minecraft:birch_sapling", 5, "白桦树苗"));
        items.add(new ShopItem("minecraft:jungle_sapling", 5, "丛林树苗"));
        items.add(new ShopItem("minecraft:acacia_sapling", 5, "金合欢树苗"));
        items.add(new ShopItem("minecraft:dark_oak_sapling", 5, "深色橡树树苗"));
        items.add(new ShopItem("minecraft:mangrove_propagule", 5, "红树林胎生苗")); // 1.19 新增
        items.add(new ShopItem("minecraft:cherry_sapling", 5, "樱花树苗")); // 1.20 新增

        // 树叶
        items.add(new ShopItem("minecraft:oak_leaves", 5, "橡树树叶"));
        items.add(new ShopItem("minecraft:spruce_leaves", 5, "云杉树叶"));
        items.add(new ShopItem("minecraft:birch_leaves", 5, "白桦树叶"));
        items.add(new ShopItem("minecraft:jungle_leaves", 5, "丛林树叶"));
        items.add(new ShopItem("minecraft:acacia_leaves", 5, "金合欢树叶"));
        items.add(new ShopItem("minecraft:dark_oak_leaves", 5, "深色橡树树叶"));
        items.add(new ShopItem("minecraft:mangrove_leaves", 5, "红树林树叶")); // 1.19 新增
        items.add(new ShopItem("minecraft:cherry_leaves", 5, "樱花树叶")); // 1.20 新增
        items.add(new ShopItem("minecraft:azalea_leaves", 5, "杜鹃树叶")); // 1.17 新增
        items.add(new ShopItem("minecraft:flowering_azalea_leaves", 5, "开花杜鹃树叶")); // 1.17 新增

        items.add(new ShopItem("minecraft:quartz", 5, "下界石英"));
        // 萤石
        items.add(new ShopItem("minecraft:glowstone", 5, "萤石"));

        // 红石
        items.add(new ShopItem("minecraft:redstone", 5, "红石"));

        // 海晶灯
        items.add(new ShopItem("minecraft:sea_lantern", 5, "海晶灯"));

        // 石砖
        items.add(new ShopItem("minecraft:stone_bricks", 5, "石砖"));
        items.add(new ShopItem("minecraft:mossy_stone_bricks", 5, "苔石砖")); // 苔石砖
        items.add(new ShopItem("minecraft:cracked_stone_bricks", 5, "裂纹石砖")); // 裂纹石砖
        items.add(new ShopItem("minecraft:chiseled_stone_bricks", 5, "雕纹石砖")); // 雕纹石砖

        // 烧好的石头（平滑石头）
        items.add(new ShopItem("minecraft:smooth_stone", 5, "平滑石头"));
        // 混凝土
        items.add(new ShopItem("minecraft:white_concrete", 5, "白色混凝土"));
        items.add(new ShopItem("minecraft:orange_concrete", 5, "橙色混凝土"));
        items.add(new ShopItem("minecraft:magenta_concrete", 5, "品红色混凝土"));
        items.add(new ShopItem("minecraft:light_blue_concrete", 5, "淡蓝色混凝土"));
        items.add(new ShopItem("minecraft:yellow_concrete", 5, "黄色混凝土"));
        items.add(new ShopItem("minecraft:lime_concrete", 5, "黄绿色混凝土"));
        items.add(new ShopItem("minecraft:pink_concrete", 5, "粉红色混凝土"));
        items.add(new ShopItem("minecraft:gray_concrete", 5, "灰色混凝土"));
        items.add(new ShopItem("minecraft:light_gray_concrete", 5, "淡灰色混凝土"));
        items.add(new ShopItem("minecraft:cyan_concrete", 5, "青色混凝土"));
        items.add(new ShopItem("minecraft:purple_concrete", 5, "紫色混凝土"));
        items.add(new ShopItem("minecraft:blue_concrete", 5, "蓝色混凝土"));
        items.add(new ShopItem("minecraft:brown_concrete", 5, "棕色混凝土"));
        items.add(new ShopItem("minecraft:green_concrete", 5, "绿色混凝土"));
        items.add(new ShopItem("minecraft:red_concrete", 5, "红色混凝土"));
        items.add(new ShopItem("minecraft:black_concrete", 5, "黑色混凝土"));
        // 羊毛
        items.add(new ShopItem("minecraft:white_wool", 5, "白色羊毛"));
        items.add(new ShopItem("minecraft:orange_wool", 5, "橙色羊毛"));
        items.add(new ShopItem("minecraft:magenta_wool", 5, "品红色羊毛"));
        items.add(new ShopItem("minecraft:light_blue_wool", 5, "淡蓝色羊毛"));
        items.add(new ShopItem("minecraft:yellow_wool", 5, "黄色羊毛"));
        items.add(new ShopItem("minecraft:lime_wool", 5, "黄绿色羊毛"));
        items.add(new ShopItem("minecraft:pink_wool", 5, "粉红色羊毛"));
        items.add(new ShopItem("minecraft:gray_wool", 5, "灰色羊毛"));
        items.add(new ShopItem("minecraft:light_gray_wool", 5, "淡灰色羊毛"));
        items.add(new ShopItem("minecraft:cyan_wool", 5, "青色羊毛"));
        items.add(new ShopItem("minecraft:purple_wool", 5, "紫色羊毛"));
        items.add(new ShopItem("minecraft:blue_wool", 5, "蓝色羊毛"));
        items.add(new ShopItem("minecraft:brown_wool", 5, "棕色羊毛"));
        items.add(new ShopItem("minecraft:green_wool", 5, "绿色羊毛"));
        items.add(new ShopItem("minecraft:red_wool", 5, "红色羊毛"));
        items.add(new ShopItem("minecraft:black_wool", 5, "黑色羊毛"));
        saveToConfig();
    }

    public void adjustPrices() {
        ShopPricingConfig config = ShopPricingConfig.get();
        String mode = GameSettingsManager.get(GameSettingsManager.SHOP_PRICING_MODE);
        for (ShopItem item : items) {
            int basePrice = item.getBasePrice(); // 基础价格应为整数
            if (basePrice <= 0) {
                EconomySystem.LOGGER.warn("Skip price adjustment for {} because basePrice is {}", item.getItemId(), basePrice);
                continue;
            }
            item.ensurePricingState(config);
            int currentPrice = Math.max(1, item.getCurrentPrice());
            int targetPrice = "stock".equalsIgnoreCase(mode)
                ? calculateStockTargetPrice(item, config)
                : calculateDemandTargetPrice(item, config);
            int newPrice = moveToward(currentPrice, targetPrice, config.maxCycleChangeRate);
            newPrice = clampPrice(newPrice, basePrice, config);
            item.setCurrentPrice(newPrice);
            item.setFluctuationFactor(roundRate((newPrice - currentPrice) / (double) currentPrice));
            item.decayRecentDemand(config.demandDecay);
            item.restockVirtualStock(config.restockRate);
        }

        saveToConfig();
    }

    public synchronized void recordPurchase(ShopItem item, int quantity) {
        if (item == null || quantity <= 0) {
            return;
        }
        item.addRecentDemand(quantity);
        item.consumeVirtualStock(quantity);
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
            item.ensurePricingState(config);
        }
    }

    private static int calculateDemandTargetPrice(ShopItem item, ShopPricingConfig config) {
        int basePrice = item.getBasePrice();
        double demandPressure = Math.log1p(item.getRecentDemand()) * config.demandSensitivity;
        double currentPremium = (item.getCurrentPrice() - (double) basePrice) / basePrice;
        double returnPressure = -currentPremium * config.idleReturnRate;
        double multiplier = 1.0D + demandPressure + returnPressure;
        return clampPrice((int) Math.round(basePrice * multiplier), basePrice, config);
    }

    private static int calculateStockTargetPrice(ShopItem item, ShopPricingConfig config) {
        int basePrice = item.getBasePrice();
        double stockRatio = item.getMaxVirtualStock() <= 0 ? 1.0D : item.getVirtualStock() / (double) item.getMaxVirtualStock();
        double scarcity = 1.0D - Math.max(0.0D, Math.min(1.0D, stockRatio));
        double multiplier = 1.0D + Math.pow(scarcity, 1.35D) * config.stockSensitivity;
        return clampPrice((int) Math.round(basePrice * multiplier), basePrice, config);
    }

    private static int moveToward(int currentPrice, int targetPrice, double maxCycleChangeRate) {
        int maxStep = Math.max(1, (int) Math.ceil(currentPrice * Math.max(0.0D, maxCycleChangeRate)));
        int delta = targetPrice - currentPrice;
        if (Math.abs(delta) <= maxStep) {
            return targetPrice;
        }
        return currentPrice + (delta > 0 ? maxStep : -maxStep);
    }

    private static int clampPrice(int price, int basePrice, ShopPricingConfig config) {
        int min = Math.max(1, (int) Math.floor(basePrice * config.minPriceMultiplier));
        int max = Math.max(min, (int) Math.ceil(basePrice * config.maxPriceMultiplier));
        return Math.max(min, Math.min(max, price));
    }

    private static double roundRate(double rate) {
        return Math.round(rate * 100.0D) / 100.0D;
    }
}
