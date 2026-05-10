package com.mo.economy_system.core.economy_system.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShopManager {
    public static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile() + File.separator + EconomySystem.MODID, "economy_shop.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()  // 启用格式化
            .disableHtmlEscaping() // 可选：禁用 HTML 转义（如保留 &、< 等符号）
            .create();
    private static final Random RANDOM = new Random();

    private final List<ShopItem> items = new ArrayList<>();

    public ShopManager() {
        loadFromConfig();
    }

    public List<ShopItem> getItems() {
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
        for (ShopItem item : items) {
            int basePrice = item.getBasePrice(); // 基础价格应为整数
            if (basePrice <= 0) {
                EconomySystem.LOGGER.warn("Skip price adjustment for {} because basePrice is {}", item.getItemId(), basePrice);
                continue;
            }
            int currentPrice = item.getCurrentPrice();

            // 计算价格偏离比例（使用浮点计算，最终结果转为int）
            double deviation = (currentPrice - (double)basePrice) / basePrice;

            // 定义波动参数
            double minFactor = 0.5;  // 最小波动系数
            double maxFactor = 3.0;  // 最大波动系数
            double baseFluctuation = 0.1; // 基础波动幅度

            // 根据偏离程度细分波动区间
            if (deviation >= 0.5) {          // 溢价50%以上
                minFactor = 0.3;             // 强抑制上涨
                maxFactor = 0.8;
                baseFluctuation = -0.05;     // 允许小幅回落
            } else if (deviation >= 0.3) {   // 溢价30%-50%
                minFactor = 0.6;
                maxFactor = 1.2;
            } else if (deviation <= -0.5) {  // 折价50%以上
                minFactor = 1.5;             // 强刺激回升
                maxFactor = 2.5;
                baseFluctuation = 0.15;      // 增加回升概率
            } else if (deviation <= -0.3) {  // 折价30%-50%
                minFactor = 1.2;
                maxFactor = 1.8;
            } else {                         // 正常波动区间（-30% ~ +30%）
                minFactor = 0.8;
                maxFactor = 1.2;
            }

            // 生成带趋势的随机因子
            double randomFactor = baseFluctuation +
                    (minFactor + (maxFactor - minFactor) * RANDOM.nextDouble());

            // 应用衰减函数控制极端波动
            randomFactor = 1 + (randomFactor - 1) *
                    Math.exp(-Math.abs(deviation));

            // 计算新价格（保证整数）
            int newPrice = (int) Math.round(currentPrice * randomFactor);

            // 设置价格边界保护
            newPrice = Math.max(1, Math.min(newPrice, basePrice * 5)); // 最高不超过5倍

            // 当接近基础价格时增加稳定性
            if (Math.abs(newPrice - basePrice) <= basePrice * 0.1) {
                newPrice = basePrice + (int)((newPrice - basePrice) * 0.5);
            }

            item.setCurrentPrice(newPrice);

            // 记录波动系数（保留两位小数）
            BigDecimal fluctuation = new BigDecimal(randomFactor - 1)
                    .setScale(2, RoundingMode.HALF_UP);
            item.setFluctuationFactor(fluctuation.doubleValue());
        }

        saveToConfig();
    }
}
