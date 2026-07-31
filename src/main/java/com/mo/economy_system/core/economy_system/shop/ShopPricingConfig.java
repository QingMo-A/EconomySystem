package com.mo.economy_system.core.economy_system.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
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
        minPriceMultiplier = clamp(minPriceMultiplier, 0.01D, 100.0D);
        maxPriceMultiplier = Math.max(minPriceMultiplier, clamp(maxPriceMultiplier, 0.01D, 100.0D));
        maxCycleChangeRate = clamp(maxCycleChangeRate, 0.0D, 1.0D);
        demandSensitivity = clamp(demandSensitivity, 0.0D, 10.0D);
        demandDecay = clamp(demandDecay, 0.0D, 1.0D);
        idleReturnRate = clamp(idleReturnRate, 0.0D, 1.0D);
        defaultMaxStock = Math.max(1, defaultMaxStock);
        minMaxStock = Math.max(1, minMaxStock);
        restockRate = clamp(restockRate, 0.0D, 1.0D);
        stockSensitivity = clamp(stockSensitivity, 0.0D, 10.0D);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
