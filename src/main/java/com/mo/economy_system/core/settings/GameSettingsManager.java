package com.mo.economy_system.core.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.platform.EconomyServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GameSettingsManager {
    public static final String SHOP_PRICING_MODE = "shop.pricing.mode";
    private static final File CONFIG_FILE = EconomyServices.platform()
            .configDirectory()
            .resolve(EconomySystem.MODID)
            .resolve("game_settings.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, GameSetting<?>> REGISTRY = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();

    static {
        register(new StringGameSetting(
            SHOP_PRICING_MODE,
            "系统商店涨价机制: demand=交易需求驱动, stock=虚拟库存驱动",
            "demand",
            Set.of("demand", "stock")
        ));
        load();
    }

    private GameSettingsManager() {
    }

    public static synchronized void register(GameSetting<?> setting) {
        REGISTRY.put(setting.key(), setting);
        VALUES.putIfAbsent(setting.key(), setting.serializeRaw(setting.defaultValue()));
    }

    public static synchronized Map<String, String> getAll() {
        return new LinkedHashMap<>(VALUES);
    }

    public static synchronized String get(String key) {
        GameSetting<?> setting = REGISTRY.get(key);
        if (setting == null) {
            return VALUES.get(key);
        }
        return VALUES.getOrDefault(key, setting.serializeRaw(setting.defaultValue()));
    }

    public static synchronized boolean set(String key, String rawValue) {
        GameSetting<?> setting = REGISTRY.get(key);
        if (setting == null) {
            return false;
        }
        VALUES.put(key, parseToString(setting, rawValue));
        save();
        return true;
    }

    public static synchronized String description(String key) {
        GameSetting<?> setting = REGISTRY.get(key);
        return setting == null ? "" : setting.description();
    }

    public static synchronized void load() {
        ensureDefaults();
        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    GameSetting<?> setting = REGISTRY.get(entry.getKey());
                    if (setting != null) {
                        VALUES.put(entry.getKey(), parseToString(setting, entry.getValue()));
                    }
                }
            }
            save();
        } catch (Exception e) {
            EconomySystem.LOGGER.error("Failed to load game settings {}, keeping defaults", CONFIG_FILE, e);
        }
    }

    public static synchronized void save() {
        ensureConfigDirectory();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(VALUES, writer);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("Failed to save game settings {}", CONFIG_FILE, e);
        }
    }

    private static void ensureDefaults() {
        for (GameSetting<?> setting : REGISTRY.values()) {
            VALUES.putIfAbsent(setting.key(), setting.serializeRaw(setting.defaultValue()));
        }
    }

    private static void ensureConfigDirectory() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String parseToString(GameSetting setting, String rawValue) {
        Object value = setting.parse(rawValue);
        return setting.serialize(value);
    }
}
