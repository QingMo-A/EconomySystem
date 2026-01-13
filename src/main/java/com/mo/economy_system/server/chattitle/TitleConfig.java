package com.mo.economy_system.server.chattitle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private static final Gson GSON = new GsonBuilder() // 用GsonBuilder来配置Gson
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // 存储解析后的称号（ID -> Title）
    private static Map<Integer, Title> titleMap = new HashMap<>();
    // 新增：名称 -> Title（反向映射，用于按名称查找，保证名称唯一）
    private static Map<String, Title> nameToTitleMap = new HashMap<>();

    // 加载配置文件的核心方法
    public static void loadConfig() {
        // 1. 若配置文件不存在，生成默认配置
        if (!TITLE_CONFIG_FILE.exists()) {
            createDefaultConfig();
            return;
        }

        // 2. 读取并解析JSON
        try (FileReader reader = new FileReader(TITLE_CONFIG_FILE)) {
            // 解析JSON数组为List<TitleData>（需自定义内部类TitleData接收JSON字段）
            List<TitleData> titleDataList = GSON.fromJson(reader, new TypeToken<List<TitleData>>() {}.getType());

            // 3. 转换为Title对象并存入Map（同步维护两个映射）
            titleMap.clear();
            nameToTitleMap.clear();
            for (TitleData data : titleDataList) {
                // 校验ID唯一性（避免重复ID）
                if (titleMap.containsKey(data.titleId)) {
                    System.err.println("重复的称号ID：" + data.titleId + "，已跳过");
                    continue;
                }
                // 校验名称唯一性（避免重复名称）
                if (nameToTitleMap.containsKey(data.titleName)) {
                    System.err.println("重复的称号名称：" + data.titleName + "，已跳过");
                    continue;
                }
                Title title = new Title(data.titleId, data.titleName, data.color);
                titleMap.put(data.titleId, title);
                nameToTitleMap.put(data.titleName, title); // 同步到名称映射
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
                    new TitleData(0, "萌新鱼友", 0xAAAAAA),  // 灰色（纯RGB）
                    new TitleData(1, "TEST", 0xFF5555),      // 红色（纯RGB）
                    new TitleData(2, "TEST2", 0x55FFFF)     // 青色（纯RGB）
            );

            File parentDir = TITLE_CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileWriter writer = new FileWriter(TITLE_CONFIG_FILE)) {
                GSON.toJson(defaultTitles, writer);
            }

            // 同步初始化两个映射
            titleMap.clear();
            nameToTitleMap.clear();
            for (TitleData data : defaultTitles) {
                Title title = new Title(data.titleId, data.titleName, data.color);
                titleMap.put(data.titleId, title);
                nameToTitleMap.put(data.titleName, title);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("默认称号配置生成失败！");
        }
    }

    // ========== 原有方法：按ID获取 ==========
    public static Title getTitleById(int titleId) {
        // 找不到时返回默认称号（ID=0）
        return titleMap.getOrDefault(titleId, titleMap.get(0));
    }

    // ========== 新增核心方法：按名称获取 ==========
    public static Title getTitleByName(String titleName) {
        // 空值校验 + 找不到返回默认称号
        if (titleName == null || titleName.isEmpty()) {
            return titleMap.get(0);
        }
        // 精确匹配名称（如需忽略大小写，可改为：nameToTitleMap.get(titleName.toLowerCase())）
        return nameToTitleMap.getOrDefault(titleName, titleMap.get(0));
    }

    public static void removeTitleById(int titleId) {
        // 同步删除两个映射中的数据
        Title removedTitle = titleMap.remove(titleId);
        if (removedTitle != null) {
            nameToTitleMap.remove(removedTitle.getTitleName());
        }
    }

    public static void saveConfig() {
        try {
            File parentDir = TITLE_CONFIG_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<TitleData> titleDataList = titleMap.values().stream()
                    .map(title -> new TitleData(title.getTitleID(), title.getTitleName(), title.getColor()))
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
        int color = 0xFFFFFFFF; // 默认白色

        // 新增带参构造器（简化默认配置创建）
        public TitleData(int titleId, String titleName, int color) {
            this.titleId = titleId;
            this.titleName = titleName;
            this.color = color;
        }

        public TitleData() {}
    }
}