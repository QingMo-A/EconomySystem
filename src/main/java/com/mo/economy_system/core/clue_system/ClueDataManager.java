package com.mo.economy_system.core.clue_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线索数据管理器
 * 负责加载和管理线索配置数据
 * 配置文件路径: config/economy_system/clue_data.json
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClueDataManager {

    private static final File CLUE_DATA_FILE = new File("config/economy_system/data/clue_data.json");

    // 线索缓存：clueId -> ClueData
    public static Map<Integer, ClueData> CLUE_CACHE = new ConcurrentHashMap<>();

    // 阶段线索索引：stageId -> List<ClueData>
    public static Map<Integer, List<ClueData>> STAGE_INDEX = new ConcurrentHashMap<>();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final Type LIST_TYPE = new TypeToken<List<ClueData>>() {}.getType();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ensureFileExists();
        loadClueData();
    }

    /**
     * 确保配置文件存在
     */
    private static void ensureFileExists() {
        try {
            // 确保目录存在
            File parentDir = CLUE_DATA_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("线索数据目录创建成功：{}", parentDir.getPath());
                }
            }

            // 确保文件存在
            if (!CLUE_DATA_FILE.exists()) {
                boolean fileCreated = CLUE_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("线索数据文件创建成功：{}", CLUE_DATA_FILE.getPath());
                    saveDefaultConfig();
                }
            } else if (CLUE_DATA_FILE.length() == 0) {
                // 文件存在但为空
                EconomySystem.LOGGER.info("线索数据文件为空，创建默认配置");
                saveDefaultConfig();
            } else {
                EconomySystem.LOGGER.info("线索数据文件已存在：{}", CLUE_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化线索数据文件失败", e);
        }
    }

    /**
     * 加载线索数据
     */
    public static void loadClueData() {
        CLUE_CACHE.clear();
        STAGE_INDEX.clear();

        try (FileReader reader = new FileReader(CLUE_DATA_FILE)) {
            // 处理空文件
            if (CLUE_DATA_FILE.length() == 0) {
                saveDefaultConfig();
                return;
            }

            List<ClueData> clueList = GSON.fromJson(reader, LIST_TYPE);
            if (clueList != null) {
                for (ClueData clue : clueList) {
                    CLUE_CACHE.put(clue.getClueId(), clue);
                    // 构建阶段索引
                    STAGE_INDEX.computeIfAbsent(clue.getClueStage(), k -> new java.util.ArrayList<>()).add(clue);
                }
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载线索数据失败", e);
            saveDefaultConfig();
        }

        EconomySystem.LOGGER.info("线索数据加载完成，共 {} 条线索，{} 个阶段",
                CLUE_CACHE.size(), STAGE_INDEX.size());
    }

    /**
     * 保存线索数据
     */
    public static void saveClueData() {
        try (FileWriter writer = new FileWriter(CLUE_DATA_FILE)) {
            List<ClueData> clueList = new java.util.ArrayList<>(CLUE_CACHE.values());
            // 按ID排序
            clueList.sort(java.util.Comparator.comparingInt(ClueData::getClueId));
            GSON.toJson(clueList, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存线索数据失败", e);
        }
    }

    /**
     * 保存默认配置
     */
    private static void saveDefaultConfig() {
        List<ClueData> defaultClues = new java.util.ArrayList<>();

        // 示例线索数据
        defaultClues.add(new ClueData(
                1,
                1,
                "神秘的开端",
                "1899年",
                "未知探险家",
                "在这片荒芜的土地上，埋藏着许多不为人知的秘密...\n\n我发现了这个奇怪的地方，空气中弥漫着不祥的气息。\n\n需要继续调查下去..."
        ));

        defaultClues.add(new ClueData(
                2,
                1,
                "旧日记残页",
                "1899年冬",
                "失踪的矿工",
                "我们挖到了一些奇怪的东西...那不是矿石...\n\n那是某种古老的遗迹，里面传来低沉的嗡嗡声。\n\n我不该再待在这里了..."
        ));

        defaultClues.add(new ClueData(
                3,
                2,
                "黑市商人的秘密",
                "1900年春",
                "匿名商人",
                "有些东西比黄金更珍贵，也更危险...\n\n那批货物来自地下深处，我把它们藏在了北方的废弃矿坑里。\n\n如果你能找到它们，就是你的了。但记住，知识是有代价的..."
        ));

        try (FileWriter writer = new FileWriter(CLUE_DATA_FILE)) {
            GSON.toJson(defaultClues, writer);
            EconomySystem.LOGGER.info("默认线索配置已保存");
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存默认线索配置失败", e);
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 根据线索ID获取线索数据
     */
    public static ClueData getClue(int clueId) {
        return CLUE_CACHE.get(clueId);
    }

    /**
     * 根据阶段ID获取该阶段的所有线索
     */
    public static List<ClueData> getCluesByStage(int stageId) {
        return STAGE_INDEX.getOrDefault(stageId, new java.util.ArrayList<>());
    }

    /**
     * 获取所有线索
     */
    public static Map<Integer, ClueData> getAllClues() {
        return CLUE_CACHE;
    }

    /**
     * 获取线索总数
     */
    public static int getClueCount() {
        return CLUE_CACHE.size();
    }

    /**
     * 获取阶段总数
     */
    public static int getStageCount() {
        return STAGE_INDEX.size();
    }

    /**
     * 检查线索是否存在
     */
    public static boolean hasClue(int clueId) {
        return CLUE_CACHE.containsKey(clueId);
    }
}
