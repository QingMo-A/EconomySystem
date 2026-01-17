package com.mo.economy_system.core.blueprint_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蓝图配置管理器
 * 负责加载和管理蓝图配置文件
 */
public class BlueprintConfigManager {
    // 配置文件名
    private static final String CONFIG_FILE_NAME = "blueprint_config.json";

    // Gson实例
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    // 当前配置
    private BlueprintConfig currentConfig;

    // 服务端标识（用于多人联机模式）
    private String serverIdentifier = null;

    // 是否为多人联机模式
    private boolean isMultiplayer = false;

    // 单例实例
    private static BlueprintConfigManager instance;

    private BlueprintConfigManager() {
        currentConfig = new BlueprintConfig();
    }

    public static BlueprintConfigManager getInstance() {
        if (instance == null) {
            instance = new BlueprintConfigManager();
        }
        return instance;
    }

    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */
    private File getConfigFile() {
        if (isMultiplayer && serverIdentifier != null) {
            // 多人联机模式：保存到 blueprint config/{server_identifier}/ 目录
            Path basePath = FMLPaths.GAMEDIR.get().toFile().toPath();
            Path configPath = Paths.get(basePath.toString(), "blueprint config", serverIdentifier);
            File configDir = configPath.toFile();

            // 确保目录存在
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            return new File(configDir, CONFIG_FILE_NAME);
        } else {
            // 本地单人模式：保存到根目录
            Path basePath = FMLPaths.GAMEDIR.get().toFile().toPath();
            return new File(basePath.toFile(), CONFIG_FILE_NAME);
        }
    }

    /**
     * 加载配置文件
     */
    public synchronized void loadConfig() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            EconomySystem.LOGGER.info("蓝图配置文件不存在，创建默认配置: {}", configFile.getPath());
            currentConfig = createDefaultConfig();
            saveConfig();
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            BlueprintConfig loadedConfig = GSON.fromJson(isr, BlueprintConfig.class);
            if (loadedConfig != null) {
                currentConfig = loadedConfig;
                EconomySystem.LOGGER.info("成功加载蓝图配置文件: {}", configFile.getPath());
            } else {
                EconomySystem.LOGGER.warn("蓝图配置文件为空，使用默认配置");
                currentConfig = createDefaultConfig();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载蓝图配置文件失败，使用默认配置", e);
            currentConfig = createDefaultConfig();
        }
    }

    /**
     * 保存配置文件
     */
    public synchronized void saveConfig() {
        File configFile = getConfigFile();

        try (FileOutputStream fos = new FileOutputStream(configFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(currentConfig, osw);
            EconomySystem.LOGGER.info("成功保存蓝图配置文件: {}", configFile.getPath());
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存蓝图配置文件失败", e);
        }
    }

    /**
     * 创建默认配置
     */
    private BlueprintConfig createDefaultConfig() {
        BlueprintConfig config = new BlueprintConfig();

        // 默认解锁的基础物品
        config.addDefaultUnlockedItem("minecraft:stick");
        config.addDefaultUnlockedItem("minecraft:oak_planks");
        config.addDefaultUnlockedItem("minecraft:torch");
        config.addDefaultUnlockedItem("minecraft:crafting_table");
        config.addDefaultUnlockedItem("minecraft:wooden_axe");
        config.addDefaultUnlockedItem("minecraft:wooden_shovel");
        config.addDefaultUnlockedItem("minecraft:wooden_pickaxe");
        config.addDefaultUnlockedItem("minecraft:wooden_sword");
        config.addDefaultUnlockedItem("minecraft:wooden_hoe");

        // 排除关键字（不需要蓝图的物品类型）
        config.addExcludedKeyword("*_wood");
        config.addExcludedKeyword("*_planks");
        config.addExcludedKeyword("*_log");
        config.addExcludedKeyword("*_terracotta");
        config.addExcludedKeyword("*_bed");
        config.addExcludedKeyword("*_candle");
        config.addExcludedKeyword("*_glass_pane");
        config.addExcludedKeyword("*_glass");
        config.addExcludedKeyword("*_sign");
        config.addExcludedKeyword("*_carpet");
        config.addExcludedKeyword("*_stairs");
        config.addExcludedKeyword("*_slab");
        config.addExcludedKeyword("raw_*");
        config.addExcludedKeyword("*_wool");
        config.addExcludedKeyword("*_copper");
        config.addExcludedKeyword("*_block");
        config.addExcludedKeyword("*_button");
        config.addExcludedKeyword("*_fence_gate");
        config.addExcludedKeyword("*_door");
        config.addExcludedKeyword("*_boat");
        config.addExcludedKeyword("*_banner");
        config.addExcludedKeyword("*_fence");
        config.addExcludedKeyword("*_ingot");
        config.addExcludedKeyword("*_dye");
        config.addExcludedKeyword("*_plate");
        config.addExcludedKeyword("*_concrete_powder");
        config.addExcludedKeyword("minecraft:snow");
        config.addExcludedKeyword("*_trapdoor");

        return config;
    }

    /**
     * 获取当前配置
     */
    public BlueprintConfig getCurrentConfig() {
        return currentConfig;
    }

    /**
     * 设置服务端标识（用于多人联机模式）
     * @param serverIdentifier 服务端标识
     */
    public void setServerIdentifier(String serverIdentifier) {
        this.serverIdentifier = sanitizeServerIdentifier(serverIdentifier);
        this.isMultiplayer = true;
        EconomySystem.LOGGER.info("设置服务端标识: {}, 切换到多人联机模式", this.serverIdentifier);
    }

    /**
     * 设置为本地单人模式
     */
    public void setSinglePlayerMode() {
        this.serverIdentifier = null;
        this.isMultiplayer = false;
        EconomySystem.LOGGER.info("切换到本地单人模式");
    }

    /**
     * 获取服务端标识
     */
    public String getServerIdentifier() {
        return serverIdentifier;
    }

    /**
     * 是否为多人联机模式
     */
    public boolean isMultiplayer() {
        return isMultiplayer;
    }

    /**
     * 清理服务端标识中的非法字符
     */
    private String sanitizeServerIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "unknown_server";
        }
        // 移除或替换非法字符
        return identifier.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 检查物品是否在默认解锁列表中
     */
    public boolean isDefaultUnlocked(String itemId) {
        return currentConfig.getDefaultUnlockedItems().contains(itemId);
    }

    /**
     * 检查物品是否匹配排除关键字
     */
    public boolean isExcludedByKeyword(String itemId) {
        for (String keyword : currentConfig.getExcludedKeywords()) {
            if (matchesKeyword(itemId, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品ID是否匹配关键字
     */
    private boolean matchesKeyword(String itemId, String keyword) {
        if (keyword.startsWith("*")) {
            // 后缀匹配，例如 *_wood 匹配 oak_planks
            String suffix = keyword.substring(1);
            return itemId.endsWith(suffix);
        } else if (keyword.endsWith("*")) {
            // 前缀匹配，例如 raw_* 匹配 raw_iron
            String prefix = keyword.substring(0, keyword.length() - 1);
            return itemId.startsWith(prefix);
        } else {
            // 精确匹配
            return itemId.equals(keyword);
        }
    }

    /**
     * 获取默认解锁物品列表
     */
    public Set<String> getDefaultUnlockedItemSet() {
        Set<String> keySet = ConcurrentHashMap.newKeySet();
        keySet.addAll(currentConfig.getDefaultUnlockedItems());
        return keySet;
    }

    /**
     * 获取排除关键字列表
     */
    public List<String> getExcludedKeywordList() {
        return new ArrayList<>(currentConfig.getExcludedKeywords());
    }
}