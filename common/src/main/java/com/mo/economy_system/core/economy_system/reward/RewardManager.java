package com.mo.economy_system.core.economy_system.reward;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RewardManager {
    public static final File CONFIG_FILE = EconomyServices.platform()
            .configDirectory()
            .resolve(EconomyConstants.MOD_ID)
            .resolve("economy_rewards.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()  // 启用格式化
            .disableHtmlEscaping() // 可选：禁用 HTML 转义（如保留 &、< 等符号）
            .create();

    private static final List<RewardEntry> rewards = new ArrayList<>();

    public RewardManager() {
        loadFromConfig();
    }

    public List<RewardEntry> getRewards() {
        return new ArrayList<>(rewards); // 返回副本保护内部列表
    }

    public Optional<RewardEntry> getRewardForEntity(ResourceLocation entityType) {
        return rewards.stream()
                .filter(entry -> entry.type.equals(entityType.toString()))
                .findFirst();
    }

    public void saveToConfig() {
        ensureConfigDirectory();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(rewards, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureConfigDirectory() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

    public void loadFromConfig() {
        if (!CONFIG_FILE.exists()) {
            saveDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type listType = new TypeToken<List<RewardEntry>>() {}.getType();
            List<RewardEntry> loadedRewards = GSON.fromJson(reader, listType);
            rewards.clear();
            if (loadedRewards != null) {
                rewards.addAll(loadedRewards);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultConfig() {
        rewards.add(new RewardEntry("minecraft:zombie", 0.1, 1, 5));
        rewards.add(new RewardEntry("minecraft:skeleton", 0.2, 2, 8));
        rewards.add(new RewardEntry("minecraft:creeper", 0.15, 5, 10));
        rewards.add(new RewardEntry("minecraft:spider", 0.18, 1, 4));
        rewards.add(new RewardEntry("minecraft:witch", 0.25, 3, 7));
        rewards.add(new RewardEntry("minecraft:enderman", 0.12, 1, 3));
        rewards.add(new RewardEntry("minecraft:slime", 0.2, 2, 5));
        rewards.add(new RewardEntry("minecraft:blaze", 0.3, 2, 6));
        rewards.add(new RewardEntry("minecraft:ghast", 0.15, 1, 2));
        rewards.add(new RewardEntry("minecraft:magma_cube", 0.2, 3, 6));
        rewards.add(new RewardEntry("minecraft:phantom", 0.1, 1, 2));
        rewards.add(new RewardEntry("minecraft:piglin", 0.15, 1, 4));
        rewards.add(new RewardEntry("minecraft:piglin_brute", 0.2, 2, 6));
        rewards.add(new RewardEntry("minecraft:hoglin", 0.18, 1, 3));
        rewards.add(new RewardEntry("minecraft:zombified_piglin", 0.1, 1, 4));
        rewards.add(new RewardEntry("minecraft:vindicator", 0.2, 2, 5));
        rewards.add(new RewardEntry("minecraft:evoker", 0.25, 3, 8));
        rewards.add(new RewardEntry("minecraft:illusioner", 0.25, 2, 6));
        rewards.add(new RewardEntry("minecraft:pillager", 0.15, 1, 4));
        rewards.add(new RewardEntry("minecraft:ravager", 0.35, 5, 12));
        rewards.add(new RewardEntry("minecraft:drowned", 0.2, 1, 4));
        rewards.add(new RewardEntry("minecraft:guardian", 0.2, 2, 5));
        rewards.add(new RewardEntry("minecraft:elder_guardian", 0.3, 4, 8));
        rewards.add(new RewardEntry("minecraft:shulker", 0.2, 2, 4));
        rewards.add(new RewardEntry("minecraft:wither_skeleton", 0.22, 1, 3));
        rewards.add(new RewardEntry("minecraft:wither", 0.5, 1, 1));
        rewards.add(new RewardEntry("minecraft:ender_dragon", 1.0, 1, 1));

        // 保存到配置文件
        saveToConfig();
    }


    public static class RewardEntry {
        public String type; // 实体类型
        public double dropChance; // 掉落几率
        public int dropMin; // 最小掉落值
        public int dropMax; // 最大掉落值

        public RewardEntry(String type, double dropChance, int dropMin, int dropMax) {
            this.type = type;
            this.dropChance = dropChance;
            this.dropMin = dropMin;
            this.dropMax = dropMax;
        }
    }
}
