package com.mo.economy_system.core.territory_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import com.mo.economy_system.EconomySystem;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class TerritoryBuffManager {
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), EconomySystem.MODID + "/territory_buffs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, TerritoryBuffConfig> BUFF_CONFIGS = new HashMap<>();

    public static void initConfig() {
        if (!CONFIG_FILE.exists()) {
            initDefaultConfig();
        }
        loadConfig();
    }

    private static void initDefaultConfig() {
        List<TerritoryBuffConfig> defaultBuffs = new ArrayList<>();

        TerritoryBuffConfig speedBuff = new TerritoryBuffConfig();
        speedBuff.setId("speed_boost");
        speedBuff.setDisplayText("速度++");
        speedBuff.setEffectId("minecraft:speed");
        speedBuff.setInitialUnlockState(false);
        speedBuff.setInitialLevel(0);
        speedBuff.setSingleUpgradeLevel(1);
        speedBuff.setMaxLevel(3);

        // 创建升级消耗
        List<TerritoryBuffConfig.BuffUpgradeCost> upgradeCostList = new ArrayList<>();
        TerritoryBuffConfig.BuffUpgradeCost cost = new TerritoryBuffConfig.BuffUpgradeCost();

        // 添加多个物品需求
        TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement item1 = new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:emerald", 5);
        TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement item2 = new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:diamond", 2);
        TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement item3 = new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:gold_ingot", 10);

        cost.items = List.of(item1, item2, item3);
        cost.xp = 5;
        cost.df_coin = 10;
        upgradeCostList.add(cost);

        speedBuff.setUpgradeCost(upgradeCostList);
        defaultBuffs.add(speedBuff);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), "UTF-8")) {
            GSON.toJson(Map.of("buffs", defaultBuffs), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void loadConfig() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), "UTF-8")) {
            Type type = new TypeToken<Map<String, List<TerritoryBuffConfig>>>() {}.getType();
            Map<String, List<TerritoryBuffConfig>> data = GSON.fromJson(reader, type);
            List<TerritoryBuffConfig> buffs = data.get("buffs");

            BUFF_CONFIGS.clear();

            System.out.println("📥 正在加载 Buff 配置...");
            for (TerritoryBuffConfig buff : buffs) {
                BUFF_CONFIGS.put(buff.getId(), buff);
                System.out.println("✅ 已加载 Buff: " + buff.getId() + "（" + buff.getDisplayText() + "）");
                System.out.println("   ➡ 效果 ID: " + buff.getEffectId());
                System.out.println("   🔹 初始解锁状态: " + buff.isInitialUnlockState());
                System.out.println("   🔹 初始等级: " + buff.getInitialLevel() + "/" + buff.getMaxLevel());
                System.out.println("   🔹 单次升级增加: " + buff.getSingleUpgradeLevel());
                System.out.println("   🔹 升级成本:");

                for (TerritoryBuffConfig.BuffUpgradeCost cost : buff.getUpgradeCost()) {
                    for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemReq : cost.items) {
                        System.out.println("  - 物品: " + itemReq.item + " x " + itemReq.count);
                    }
                    System.out.println("     - 经验: " + cost.xp);
                    System.out.println("     - 货币: " + cost.df_coin);
                }
                System.out.println("-----------------------------");
            }
        } catch (Exception e) {
            System.err.println("❌ 读取 Buff 配置文件失败！");
            e.printStackTrace();
        }
    }

    public static TerritoryBuffConfig getBuffConfig(String id) {
        return BUFF_CONFIGS.get(id);
    }

    public static Set<String> getAllBuffIDs() {
        return BUFF_CONFIGS.keySet();
    }
}