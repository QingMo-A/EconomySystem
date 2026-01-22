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
        long now = System.currentTimeMillis();

        defaultNotices.add(new NoticeData(
            1,
            "欢迎来到梦鱼服",
            "感谢您加入我们的服务器！请遵守规则，祝您游戏愉快！\n\n在这里您可以体验到独特的经济系统、领地系统以及细胞分裂机制。",
            now - 10000000
        ));

        defaultNotices.add(new NoticeData(
            2,
            "服务器规则更新",
            "为了维护游戏环境，我们更新了部分规则：\n\n1. 禁止使用任何外挂或作弊客户端\n2. 禁止恶意破坏他人建筑\n3. 禁止谩骂或骚扰其他玩家\n4. 请保持良好的游戏氛围\n\n违反规则可能会导致封禁处理。",
            now - 8000000
        ));

        defaultNotices.add(new NoticeData(
            3,
            "新增领地增益系统",
            "领地系统现已升级！\n\n现在您可以在领地中建造各种增益建筑，提升资源产出效率。\n\n包括：\n- 农田增益：提升作物生长速度\n- 矿洞增益：提升矿石生成率\n- 经验增益：提升经验获取速度\n\n快来建造您的领地吧！",
            now - 6000000
        ));

        defaultNotices.add(new NoticeData(
            4,
            "周末双倍经验活动",
            "本周六、日将开启双倍经验活动！\n\n活动期间，所有玩家的经验获取速度翻倍。\n\n同时还有更多惊喜活动等你来参与！",
            now - 4000000
        ));

        defaultNotices.add(new NoticeData(
            5,
            "细胞分裂机制说明",
            "关于细胞分裂系统：\n\n当您不幸死亡时，会消耗复活点数。复活点数耗尽后，您将无法通过常规方式重生。\n\n但不要担心！其他幸存者可以通过特殊方式复活您。\n\n请珍惜每一次生命...",
            now - 2000000
        ));

        defaultNotices.add(new NoticeData(
            6,
            "经济系统上线",
            "全新的经济系统已经上线！\n\n您现在可以：\n- 在商店购买各种物品\n- 在市场中发布卖单和求单\n- 与其他玩家进行自由交易\n\n快去体验吧！",
            now
        ));

        defaultNotices.add(new NoticeData(
            7,
            "关于感染度系统",
            "感染度是本服的特色系统之一。\n\n当您处于感染状态时：\n- 每次复活消耗20点复活点数\n- 需要通过特殊方式治疗\n\n请保持警惕，避免被感染！",
            now + 1000
        ));

        defaultNotices.add(new NoticeData(
            8,
            "服务器维护通知",
            "服务器将于本周日凌晨2:00-4:00进行维护升级。\n\n维护期间将无法登录，请提前做好准备。\n\n感谢您的理解与支持！",
            now + 2000
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

    /**
     * 添加新公告并保存到配置文件
     * @param notice 要添加的公告
     * @return 是否添加成功
     */
    public static boolean addNotice(NoticeData notice) {
        NOTICES.add(notice);
        return saveToConfig();
    }

    /**
     * 删除指定ID的公告并保存到配置文件
     * @param noticeId 要删除的公告ID
     * @return 是否删除成功
     */
    public static boolean deleteNotice(int noticeId) {
        boolean removed = NOTICES.removeIf(n -> n.getNoticeId() == noticeId);
        if (removed) {
            saveToConfig();
        }
        return removed;
    }

    /**
     * 保存公告数据到配置文件
     * @return 是否保存成功
     */
    private static boolean saveToConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            GSON.toJson(NOTICES, osw);
            EconomySystem.LOGGER.info("已保存 {} 条公告到配置文件", NOTICES.size());
            return true;

        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存公告配置文件失败", e);
            return false;
        }
    }
}