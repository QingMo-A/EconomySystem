package com.mo.economy_system.server.chattitle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TitleConfig {
    // 配置文件路径（mod根目录/config/titles.json）
    private static final File TITLE_CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("economy_system/economy_titles.json").toFile();
    private static final Gson GSON = new Gson(); // 用Gson解析JSON（Forge自带Gson，无需额外依赖）

    // 存储解析后的称号（ID -> Title）
    private static Map<Integer, Title> titleMap = new HashMap<>();

    // 加载配置文件的核心方法
    public static void loadConfig() {
        // 1. 若配置文件不存在，生成默认配置
        if (!TITLE_CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        // 2. 读取并解析JSON
        try (FileReader reader = new FileReader(TITLE_CONFIG_FILE)) {
            // 解析JSON数组为List<TitleData>（需自定义内部类TitleData接收JSON字段）
            List<TitleData> titleDataList = GSON.fromJson(reader, new TypeToken<List<TitleData>>() {}.getType());

            // 3. 转换为Title对象并存入Map
            titleMap.clear();
            for (TitleData data : titleDataList) {
                // 校验ID唯一性（避免重复ID）
                if (titleMap.containsKey(data.titleId)) {
                    System.err.println("重复的称号ID：" + data.titleId + "，已跳过");
                    continue;
                }
                titleMap.put(data.titleId, new Title(data.titleId, data.titleName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 解析失败时回退到默认配置
            createDefaultConfig();
        }
    }

    // 生成默认配置文件（当配置缺失/解析失败时调用）
    private static void createDefaultConfig() {
        try {
            List<TitleData> defaultTitles = List.of(
                    new TitleData() {{ titleId = 0; titleName = "默认称号"; }},
                    new TitleData() {{ titleId = 1; titleName = "123"; }},
                    new TitleData() {{ titleId = 2; titleName = "456"; }}
            );

            File parentDir = TITLE_CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(TITLE_CONFIG_FILE)) {
                GSON.toJson(defaultTitles, writer);
            }

            titleMap.clear();
            for (TitleData data : defaultTitles) {
                titleMap.put(data.titleId, new Title(data.titleId, data.titleName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("默认称号配置生成失败！");
        }
    }

    // 对外提供：根据ID获取称号
    public static Title getTitleById(int titleId) {
        // 找不到时返回默认称号（ID=0）
        return titleMap.getOrDefault(titleId, titleMap.get(0));
    }

    public static void removeTitleById(int titleId) {
        titleMap.remove(titleId);
    }

    public static void saveConfig() {
        try {
            File parentDir = TITLE_CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<TitleData> titleDataList = titleMap.values().stream()
                    .map(title -> {
                        TitleData data = new TitleData();
                        data.titleId = title.getTitleID();
                        data.titleName = title.getTitleName();
                        return data;
                    })
                    .toList();

            try (FileWriter writer = new FileWriter(TITLE_CONFIG_FILE)) {
                GSON.toJson(titleDataList, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("称号配置保存失败！");
        }
    }

    // 内部类：接收JSON解析的临时数据（字段名要和JSON一致）
    private static class TitleData {
        int titleId;
        String titleName;

        public TitleData(int titleId, String titleName) {
            this.titleId = titleId;
            this.titleName = titleName;
        }

        public TitleData() {

        }
    }
}