package com.mo.economy_system.core.territory_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.territory.TerritoryBuffCatalogPolicy;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import com.mo.economy_system.platform.EconomyServices;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared configuration/catalog facade for territory buffs. */
public final class TerritoryBuffManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, TerritoryBuffConfig> BUFF_CONFIGS = new LinkedHashMap<>();
    private static boolean catalogAvailable;

    private TerritoryBuffManager() {}

    /**
     * Loads a complete, validated catalog. A failed load is never published as an empty catalog.
     *
     * @return {@code true} when callers may safely synchronize persisted territories
     */
    public static synchronized boolean initConfig() {
        File configFile = configFile();
        if (!configFile.isFile() && !initDefaultConfig(configFile)) {
            invalidateCatalog();
            return false;
        }
        return loadConfig(configFile);
    }

    private static boolean initDefaultConfig(File configFile) {
        List<TerritoryBuffConfig> defaultBuffs = new ArrayList<>();

        TerritoryBuffConfig speedBuff = new TerritoryBuffConfig();
        speedBuff.setId("speed_boost");
        speedBuff.setDisplayText("\u901f\u5ea6++");
        speedBuff.setEffectId("minecraft:speed");
        speedBuff.setInitialUnlockState(false);
        speedBuff.setInitialLevel(0);
        speedBuff.setSingleUpgradeLevel(1);
        speedBuff.setMaxLevel(3);

        TerritoryBuffConfig.BuffUpgradeCost cost = new TerritoryBuffConfig.BuffUpgradeCost();
        cost.items = List.of(
                new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:emerald", 5),
                new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:diamond", 2),
                new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement("minecraft:gold_ingot", 10));
        cost.xp = 5;
        cost.df_coin = 10;
        speedBuff.setUpgradeCost(List.of(cost));
        defaultBuffs.add(speedBuff);

        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            System.err.println("Unable to create territory buff config directory: " + parent);
            return false;
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(Map.of("buffs", defaultBuffs), writer);
            return true;
        } catch (IOException failure) {
            System.err.println("Unable to create default territory buff config: "
                    + failure.getMessage());
            return false;
        }
    }

    public static synchronized boolean loadConfig() {
        return loadConfig(configFile());
    }

    private static boolean loadConfig(File configFile) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            Map<String, TerritoryBuffConfig> loaded = parseConfig(reader);
            BUFF_CONFIGS.clear();
            BUFF_CONFIGS.putAll(loaded);
            catalogAvailable = true;
            return true;
        } catch (Exception failure) {
            invalidateCatalog();
            System.err.println("Unable to load territory buff config: " + failure.getMessage());
            return false;
        }
    }

    static Map<String, TerritoryBuffConfig> parseConfig(Reader reader) {
        Type type = new TypeToken<Map<String, List<TerritoryBuffConfig>>>() {}.getType();
        Map<String, List<TerritoryBuffConfig>> data = GSON.fromJson(reader, type);
        List<TerritoryBuffConfig> buffs = data == null ? null : data.get("buffs");
        if (buffs == null) throw new IllegalArgumentException("missing buffs list");

        Map<String, TerritoryBuffConfig> loaded = new LinkedHashMap<>();
        for (TerritoryBuffConfig buff : buffs) {
            if (buff == null || buff.getId() == null || buff.getId().isBlank()) {
                throw new IllegalArgumentException("territory buff has missing id");
            }
            // Validate before publishing so both targets see the same bounded catalog.
            toDefinition(buff);
            if (loaded.put(buff.getId(), buff) != null) {
                throw new IllegalArgumentException("duplicate territory buff id: " + buff.getId());
            }
        }
        return loaded;
    }

    public static synchronized boolean isCatalogAvailable() {
        return catalogAvailable;
    }

    public static synchronized TerritoryBuffConfig getBuffConfig(String id) {
        return BUFF_CONFIGS.get(id);
    }

    public static synchronized Set<String> getAllBuffIDs() {
        return Set.copyOf(BUFF_CONFIGS.keySet());
    }

    /** Returns a deterministic, validated catalog for persistence adapters. */
    public static synchronized List<TerritoryBuffCatalogPolicy.Definition> catalog() {
        if (!catalogAvailable) {
            throw new IllegalStateException("territory buff catalog is unavailable");
        }
        return BUFF_CONFIGS.values().stream()
                .sorted(Comparator.comparing(TerritoryBuffConfig::getId))
                .map(TerritoryBuffManager::toDefinition)
                .toList();
    }

    private static void invalidateCatalog() {
        BUFF_CONFIGS.clear();
        catalogAvailable = false;
    }

    private static File configFile() {
        return EconomyServices.platform()
                .configDirectory()
                .resolve(EconomyConstants.MOD_ID)
                .resolve("territory_buffs.json")
                .toFile();
    }

    private static TerritoryBuffCatalogPolicy.Definition toDefinition(TerritoryBuffConfig config) {
        List<BuffUpgradeCost> costs = new ArrayList<>();
        for (TerritoryBuffConfig.BuffUpgradeCost configured : safeCosts(config)) {
            if (configured == null) throw new IllegalArgumentException("null upgrade cost");
            List<ItemRequirement> items = new ArrayList<>();
            if (configured.items != null) {
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement item : configured.items) {
                    if (item == null) throw new IllegalArgumentException("null cost item");
                    items.add(new ItemRequirement(item.item, item.count));
                }
            }
            costs.add(new BuffUpgradeCost(items, configured.xp, configured.df_coin));
        }
        return new TerritoryBuffCatalogPolicy.Definition(
                config.getId(),
                config.getDisplayText(),
                config.getEffectId(),
                config.isInitialUnlockState(),
                config.getInitialLevel(),
                config.getSingleUpgradeLevel(),
                config.getMaxLevel(),
                costs);
    }

    private static List<TerritoryBuffConfig.BuffUpgradeCost> safeCosts(TerritoryBuffConfig config) {
        return config.getUpgradeCost() == null ? List.of() : config.getUpgradeCost();
    }
}
