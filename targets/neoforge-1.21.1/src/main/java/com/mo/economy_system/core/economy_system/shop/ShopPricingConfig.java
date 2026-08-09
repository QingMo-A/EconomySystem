package com.mo.economy_system.core.economy_system.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.economy.ShopPricingPolicy;
import com.mo.economy_system.platform.EconomyServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ShopPricingConfig {
    public static final File CONFIG_FILE = EconomyServices.platform()
            .configDirectory()
            .resolve(EconomySystem.MODID)
            .resolve("shop_pricing.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ShopPricingConfig INSTANCE;

    public double minPriceMultiplier = 0.5D;
    public double maxPriceMultiplier = 3.0D;
    public double maxCycleChangeRate = 0.08D;

    public double demandSensitivity = 0.035D;
    public double demandDecay = 0.65D;
    public double idleReturnRate = 0.04D;

    public int defaultMaxStock = 512;
    public int minMaxStock = 64;
    public double restockRate = 0.18D;
    public double stockSensitivity = 1.8D;

    public static ShopPricingConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static ShopPricingConfig reload() {
        INSTANCE = load();
        return INSTANCE;
    }

    private static ShopPricingConfig load() {
        if (!CONFIG_FILE.exists()) {
            ShopPricingConfig config = new ShopPricingConfig();
            config.save();
            return config;
        }
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            ShopPricingConfig config = GSON.fromJson(reader, ShopPricingConfig.class);
            if (config == null) {
                config = new ShopPricingConfig();
            }
            config.clamp();
            config.save();
            return config;
        } catch (Exception e) {
            EconomySystem.LOGGER.error("Failed to load shop pricing config {}, using defaults", CONFIG_FILE, e);
            return new ShopPricingConfig();
        }
    }

    public void save() {
        File parent = CONFIG_FILE.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        clamp();
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("Failed to save shop pricing config {}", CONFIG_FILE, e);
        }
    }

    private void clamp() {
        ShopPricingPolicy.Config normalized = toPolicyConfig();
        minPriceMultiplier = normalized.minPriceMultiplier();
        maxPriceMultiplier = normalized.maxPriceMultiplier();
        maxCycleChangeRate = normalized.maxCycleChangeRate();
        demandSensitivity = normalized.demandSensitivity();
        demandDecay = normalized.demandDecay();
        idleReturnRate = normalized.idleReturnRate();
        defaultMaxStock = normalized.defaultMaxStock();
        minMaxStock = normalized.minMaxStock();
        restockRate = normalized.restockRate();
        stockSensitivity = normalized.stockSensitivity();
    }

    public ShopPricingPolicy.Config toPolicyConfig() {
        return new ShopPricingPolicy.Config(minPriceMultiplier, maxPriceMultiplier, maxCycleChangeRate,
            demandSensitivity, demandDecay, idleReturnRate, defaultMaxStock, minMaxStock,
            restockRate, stockSensitivity);
    }
}
