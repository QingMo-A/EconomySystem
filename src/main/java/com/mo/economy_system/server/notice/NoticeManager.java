package com.mo.economy_system.server.notice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mo.economy_system.EconomySystem;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 公告管理器
 * 负责从配置文件读取公告数据
 */
public class NoticeManager {

    private static final File CONFIG_FILE = new File(
        FMLPaths.CONFIGDIR.get().toFile() + File.separator + EconomySystem.MODID,
        "notices.json"
    );

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();

    private static final List<NoticeData> NOTICES = new ArrayList<>();

    /**
     * 从配置文件加载公告数据
     */
    public static void loadFromConfig() {
        NOTICES.clear();

        // 确保配置目录存在
        File configDir = CONFIG_FILE.getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // 如果配置文件不存在，创建默认配置
        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        // 读取配置文件
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {

            Type listType = new TypeToken<List<NoticeData>>() {}.getType();
            List<NoticeData> loadedNotices = GSON.fromJson(isr, listType);

            if (loadedNotices != null) {
                NOTICES.addAll(loadedNotices);
            }

            EconomySystem.LOGGER.info("已加载 {} 条公告", NOTICES.size());

        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载公告配置文件失败", e);
        }
    }

    /**
     * 创建默认公告配置文件
     */
    private static void createDefaultConfig() {
        List<NoticeData> defaultNotices = new ArrayList<>();
        defaultNotices.add(new NoticeData(
            1,
            "欢迎来到梦鱼服",
            "感谢您加入我们的服务器！请遵守规则，祝您游戏愉快！",
            System.currentTimeMillis()
        ));

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            GSON.toJson(defaultNotices, osw);
            EconomySystem.LOGGER.info("已创建默认公告配置文件");

        } catch (IOException e) {
            EconomySystem.LOGGER.error("创建默认公告配置文件失败", e);
        }
    }

    /**
     * 获取所有公告（按发布时间倒序）
     */
    public static List<NoticeData> getNotices() {
        List<NoticeData> sortedNotices = new ArrayList<>(NOTICES);
        sortedNotices.sort(Comparator.comparingLong(NoticeData::getPublishTime).reversed());
        return sortedNotices;
    }

    /**
     * 获取最新的公告
     */
    public static NoticeData getLatestNotice() {
        if (NOTICES.isEmpty()) {
            return null;
        }
        return getNotices().get(0);
    }

    /**
     * 获取最大公告ID
     */
    public static int getMaxNoticeId() {
        return NOTICES.stream()
            .mapToInt(NoticeData::getNoticeId)
            .max()
            .orElse(0);
    }

    /**
     * 根据ID获取公告
     */
    public static NoticeData getNoticeById(int noticeId) {
        return NOTICES.stream()
            .filter(n -> n.getNoticeId() == noticeId)
            .findFirst()
            .orElse(null);
    }
}