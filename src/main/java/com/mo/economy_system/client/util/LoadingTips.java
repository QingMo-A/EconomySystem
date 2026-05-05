package com.mo.economy_system.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mo.economy_system.EconomySystem;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class LoadingTips {

    private static final List<String> tips = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static boolean loaded = false;
    private static String lastTip = "";

    private LoadingTips() {}

    public static String getRandomTip() {
        if (!loaded) {
            load();
            loaded = true;
        }
        if (tips.isEmpty()) {
            return "欢迎来到梦鱼服";
        }
        if (tips.size() == 1) {
            return tips.get(0);
        }
        String tip;
        do {
            tip = tips.get(RANDOM.nextInt(tips.size()));
        } while (tip.equals(lastTip) && tips.size() > 1);
        lastTip = tip;
        return tip;
    }

    private static void load() {
        // 优先从 config 目录加载，找不到则从 jar 内置资源加载
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(EconomySystem.MODID).resolve("loading_tips.json");
        File configFile = configPath.toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                parse(JsonParser.parseReader(reader).getAsJsonObject());
                return;
            } catch (Exception e) {
                EconomySystem.LOGGER.warn("Failed to load loading_tips.json from config, trying built-in", e);
            }
        }

        // 回退到 jar 内置资源
        try {
            var resource = net.minecraft.client.Minecraft.getInstance().getResourceManager()
                .getResource(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("economy_system", "loading_tips.json"));
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    parse(JsonParser.parseReader(reader).getAsJsonObject());
                    return;
                }
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.warn("Failed to load built-in loading_tips.json", e);
        }

        tips.add("欢迎来到梦鱼服");
    }

    private static void parse(JsonObject json) {
        tips.clear();
        JsonArray arr = json.getAsJsonArray("tips");
        for (int i = 0; i < arr.size(); i++) {
            tips.add(arr.get(i).getAsString());
        }
    }
}
