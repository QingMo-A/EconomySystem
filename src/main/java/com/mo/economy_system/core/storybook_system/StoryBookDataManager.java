package com.mo.economy_system.core.storybook_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.storybook_system.Packet_OpenStoryBookGUI;
import com.mo.economy_system.network.packets.storybook_system.Packet_OpenStoryFragmentGUI;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 片段数据管理器
 * 负责加载和管理片段配置数据，以及玩家的随记本数据
 * 配置文件路径: config/economy_system/data/fragment_data.json
 * 玩家数据路径: config/economy_system/data/storybook_player_data.json
 */
@EventBusSubscriber(modid = EconomySystem.MODID)
public class StoryBookDataManager {

    private static final File FRAGMENT_DATA_FILE = new File("config/economy_system/data/fragment_data.json");
    private static final File PLAYER_DATA_FILE = new File("config/economy_system/data/storybook_player_data.json");

    // ==================== 片段配置数据 ====================
    // 片段缓存：fragmentId -> FragmentData
    public static Map<Integer, FragmentData> FRAGMENT_CACHE = new ConcurrentHashMap<>();

    // 阶段片段索引：stageId -> List<FragmentData>
    public static Map<Integer, List<FragmentData>> STAGE_INDEX = new ConcurrentHashMap<>();

    // 章节片段索引：chapterId -> List<FragmentData>
    public static Map<Integer, List<FragmentData>> CHAPTER_INDEX = new ConcurrentHashMap<>();

    // ==================== 玩家随记本数据 ====================
    // 玩家数据：playerUUID -> StoryBookData
    public static Map<UUID, StoryBookData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();

    // 待保存的玩家数据队列
    private static final Set<UUID> DIRTY_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 自动保存间隔（tick）
    private static final int AUTO_SAVE_INTERVAL = 6000; // 5分钟
    private static int autoSaveCounter = 0;

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final Type FRAGMENT_LIST_TYPE = new TypeToken<List<FragmentData>>() {}.getType();
    private static final Type PLAYER_DATA_TYPE = new TypeToken<Map<String, StoryBookData>>() {}.getType();

    // ==================== 服务器事件 ====================

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ensureFragmentFileExists();
        ensurePlayerDataFileExists();
        loadFragmentData();
        loadPlayerData();
        EconomySystem.LOGGER.info("片段数据管理器已启动");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveAllPlayerData();
        EconomySystem.LOGGER.info("片段数据管理器已关闭，玩家数据已保存");
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUuid = player.getUUID();
            PLAYER_DATA_CACHE.computeIfAbsent(playerUuid, k -> new StoryBookData());
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUuid = player.getUUID();
            markPlayerDirty(playerUuid);
            savePlayerData(playerUuid);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        

        autoSaveCounter++;
        if (autoSaveCounter >= AUTO_SAVE_INTERVAL) {
            autoSaveCounter = 0;
            saveDirtyPlayerData();
        }
    }

    // ==================== 文件操作 ====================

    /**
     * 确保片段配置文件存在
     */
    private static void ensureFragmentFileExists() {
        try {
            File parentDir = FRAGMENT_DATA_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("片段数据目录创建成功：{}", parentDir.getPath());
                }
            }

            if (!FRAGMENT_DATA_FILE.exists()) {
                boolean fileCreated = FRAGMENT_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("片段数据文件创建成功：{}", FRAGMENT_DATA_FILE.getPath());
                    saveDefaultFragmentConfig();
                }
            } else if (FRAGMENT_DATA_FILE.length() == 0) {
                EconomySystem.LOGGER.info("片段数据文件为空，创建默认配置");
                saveDefaultFragmentConfig();
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化片段数据文件失败", e);
        }
    }

    /**
     * 确保玩家数据文件存在
     */
    private static void ensurePlayerDataFileExists() {
        try {
            File parentDir = PLAYER_DATA_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("随记本玩家数据目录创建成功：{}", parentDir.getPath());
                }
            }

            if (!PLAYER_DATA_FILE.exists()) {
                boolean fileCreated = PLAYER_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("随记本玩家数据文件创建成功：{}", PLAYER_DATA_FILE.getPath());
                }
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化随记本玩家数据文件失败", e);
        }
    }

    // ==================== 片段配置数据加载/保存 ====================

    /**
     * 加载片段配置数据
     */
    public static void loadFragmentData() {
        FRAGMENT_CACHE.clear();
        STAGE_INDEX.clear();
        CHAPTER_INDEX.clear();

        try (FileReader reader = new FileReader(FRAGMENT_DATA_FILE)) {
            if (FRAGMENT_DATA_FILE.length() == 0) {
                saveDefaultFragmentConfig();
                return;
            }

            List<FragmentData> fragmentList = GSON.fromJson(reader, FRAGMENT_LIST_TYPE);
            if (fragmentList != null) {
                for (FragmentData fragment : fragmentList) {
                    FRAGMENT_CACHE.put(fragment.getId(), fragment);
                    // 构建阶段索引
                    STAGE_INDEX.computeIfAbsent(fragment.getStageId(), k -> new ArrayList<>()).add(fragment);
                    // 构建章节索引
                    CHAPTER_INDEX.computeIfAbsent(fragment.getChapterId(), k -> new ArrayList<>()).add(fragment);
                }
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载片段数据失败", e);
            saveDefaultFragmentConfig();
        }

        EconomySystem.LOGGER.info("片段数据加载完成，共 {} 条片段，{} 个阶段，{} 个章节",
                FRAGMENT_CACHE.size(), STAGE_INDEX.size(), CHAPTER_INDEX.size());
    }

    /**
     * 保存片段配置数据
     */
    public static void saveFragmentData() {
        try (FileWriter writer = new FileWriter(FRAGMENT_DATA_FILE)) {
            List<FragmentData> fragmentList = new ArrayList<>(FRAGMENT_CACHE.values());
            // 按ID排序
            fragmentList.sort(Comparator.comparingInt(FragmentData::getId));
            GSON.toJson(fragmentList, writer);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存片段数据失败", e);
        }
    }

    /**
     * 保存默认片段配置
     */
    private static void saveDefaultFragmentConfig() {
        List<FragmentData> defaultFragments = new ArrayList<>();

        // 序章片段（使用4次残页解锁）
        defaultFragments.add(new FragmentData(
                1,
                1,
                0,
                "未知幸存者",
                "残页一",
                "2066年3月15日",
                "这是一切的开始...\n\n在这片荒芜的土地上，埋藏着许多不为人知的秘密。\n\n空气中弥漫着不祥的气息，我必须继续调查下去..."
        ));

        defaultFragments.add(new FragmentData(
                2,
                1,
                0,
                "失踪的矿工",
                "残页二",
                "2066年4月2日",
                "我们挖到了一些奇怪的东西...那不是矿石...\n\n那是某种古老的遗迹，里面传来低沉的嗡嗡声。\n\n我不该再待在这里了..."
        ));

        defaultFragments.add(new FragmentData(
                3,
                1,
                0,
                "匿名研究者",
                "残页三",
                "2066年5月10日",
                "有些东西比黄金更珍贵，也更危险...\n\n那批货物来自地下深处，我把它们藏在了北方的废弃矿坑里。\n\n如果你能找到它们，就是你的了。但记住，知识是有代价的..."
        ));

        defaultFragments.add(new FragmentData(
                4,
                1,
                0,
                "最后的记录者",
                "残页四",
                "2066年6月1日",
                "我终于明白了真相...\n\n那些感染者...他们不是怪物。\n\n他们是在进化。\n\n如果你看到了这个，请继续往北走...那里有答案..."
        ));

        try (FileWriter writer = new FileWriter(FRAGMENT_DATA_FILE)) {
            GSON.toJson(defaultFragments, writer);
            EconomySystem.LOGGER.info("默认片段配置已保存");
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存默认片段配置失败", e);
        }
    }

    // ==================== 玩家数据加载/保存 ====================

    /**
     * 加载玩家随记本数据
     */
    public static void loadPlayerData() {
        PLAYER_DATA_CACHE.clear();

        if (!PLAYER_DATA_FILE.exists() || PLAYER_DATA_FILE.length() == 0) {
            EconomySystem.LOGGER.info("随记本玩家数据文件为空，跳过加载");
            return;
        }

        try (FileReader reader = new FileReader(PLAYER_DATA_FILE)) {
            Map<String, StoryBookData> dataMap = GSON.fromJson(reader, PLAYER_DATA_TYPE);
            if (dataMap != null) {
                for (Map.Entry<String, StoryBookData> entry : dataMap.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        PLAYER_DATA_CACHE.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException e) {
                        EconomySystem.LOGGER.warn("跳过无效的玩家UUID：{}", entry.getKey());
                    }
                }
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("加载随记本玩家数据失败", e);
        }

        EconomySystem.LOGGER.info("随记本玩家数据加载完成，共 {} 个玩家", PLAYER_DATA_CACHE.size());
    }

    /**
     * 保存所有玩家数据
     */
    public static void saveAllPlayerData() {
        Map<String, StoryBookData> dataMap = new HashMap<>();
        for (Map.Entry<UUID, StoryBookData> entry : PLAYER_DATA_CACHE.entrySet()) {
            dataMap.put(entry.getKey().toString(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(PLAYER_DATA_FILE)) {
            GSON.toJson(dataMap, writer);
            DIRTY_PLAYERS.clear();
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存随记本玩家数据失败", e);
        }
    }

    /**
     * 保存单个玩家数据
     */
    public static void savePlayerData(UUID playerUuid) {
        if (playerUuid == null || !PLAYER_DATA_CACHE.containsKey(playerUuid)) {
            return;
        }

        Map<String, StoryBookData> dataMap = new HashMap<>();
        for (Map.Entry<UUID, StoryBookData> entry : PLAYER_DATA_CACHE.entrySet()) {
            dataMap.put(entry.getKey().toString(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(PLAYER_DATA_FILE)) {
            GSON.toJson(dataMap, writer);
            DIRTY_PLAYERS.remove(playerUuid);
        } catch (IOException e) {
            EconomySystem.LOGGER.error("保存玩家随记本数据失败：{}", playerUuid, e);
        }
    }

    /**
     * 保存脏数据
     */
    private static void saveDirtyPlayerData() {
        if (DIRTY_PLAYERS.isEmpty()) {
            return;
        }

        Map<String, StoryBookData> dataMap = new HashMap<>();
        for (Map.Entry<UUID, StoryBookData> entry : PLAYER_DATA_CACHE.entrySet()) {
            dataMap.put(entry.getKey().toString(), entry.getValue());
        }

        try (FileWriter writer = new FileWriter(PLAYER_DATA_FILE)) {
            GSON.toJson(dataMap, writer);
            DIRTY_PLAYERS.clear();
            EconomySystem.LOGGER.debug("随记本玩家数据自动保存完成");
        } catch (IOException e) {
            EconomySystem.LOGGER.error("自动保存随记本玩家数据失败", e);
        }
    }

    /**
     * 标记玩家数据为脏（需要保存）
     */
    public static void markPlayerDirty(UUID playerUuid) {
        DIRTY_PLAYERS.add(playerUuid);
    }

    // ==================== 片段查询方法 ====================

    /**
     * 根据片段ID获取片段数据
     */
    public static FragmentData getFragment(int fragmentId) {
        return FRAGMENT_CACHE.get(fragmentId);
    }

    /**
     * 根据阶段ID获取该阶段的所有片段
     */
    public static List<FragmentData> getFragmentsByStage(int stageId) {
        return new ArrayList<>(STAGE_INDEX.getOrDefault(stageId, Collections.emptyList()));
    }

    /**
     * 根据章节ID获取该章节的所有片段
     */
    public static List<FragmentData> getFragmentsByChapter(int chapterId) {
        return new ArrayList<>(CHAPTER_INDEX.getOrDefault(chapterId, Collections.emptyList()));
    }

    /**
     * 获取所有片段
     */
    public static Map<Integer, FragmentData> getAllFragments() {
        return new HashMap<>(FRAGMENT_CACHE);
    }

    /**
     * 获取片段总数
     */
    public static int getFragmentCount() {
        return FRAGMENT_CACHE.size();
    }

    /**
     * 检查片段是否存在
     */
    public static boolean hasFragment(int fragmentId) {
        return FRAGMENT_CACHE.containsKey(fragmentId);
    }

    // ==================== 玩家数据查询方法 ====================

    /**
     * 获取玩家的随记本数据
     */
    public static StoryBookData getPlayerStoryBook(UUID playerUuid) {
        return PLAYER_DATA_CACHE.computeIfAbsent(playerUuid, k -> new StoryBookData());
    }

    /**
     * 获取玩家的随记本数据（ServerPlayer）
     */
    public static StoryBookData getPlayerStoryBook(ServerPlayer player) {
        return getPlayerStoryBook(player.getUUID());
    }

    /**
     * 解锁片段
     */
    public static boolean unlockFragmentForPlayer(UUID playerUuid, int fragmentId) {
        if (!hasFragment(fragmentId)) {
            return false;
        }

        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        boolean unlocked = storyBook.unlockFragment(fragmentId);
        if (unlocked) {
            markPlayerDirty(playerUuid);
        }
        return unlocked;
    }

    /**
     * 解锁章节
     */
    public static boolean unlockChapterForPlayer(UUID playerUuid, int chapterId) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        boolean unlocked = storyBook.unlockChapter(chapterId);
        if (unlocked) {
            markPlayerDirty(playerUuid);
        }
        return unlocked;
    }

    /**
     * 标记片段已读
     */
    public static void markFragmentReadForPlayer(UUID playerUuid, int fragmentId) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        storyBook.markFragmentRead(fragmentId);
        markPlayerDirty(playerUuid);
    }

    /**
     * 检查玩家是否拥有随记本
     */
    public static boolean playerHasStoryBook(UUID playerUuid) {
        StoryBookData storyBook = PLAYER_DATA_CACHE.get(playerUuid);
        return storyBook != null && storyBook.hasStoryBook();
    }

    public static List<StoryBookEntryViewData> getStoryBookEntriesForPlayer(UUID playerUuid) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        List<StoryBookEntryViewData> entries = new ArrayList<>();
        for (Integer fragmentId : storyBook.getSortedFragmentIds()) {
            FragmentData fragmentData = getFragment(fragmentId);
            if (fragmentData == null) {
                continue;
            }
            entries.add(new StoryBookEntryViewData(
                    fragmentData.getId(),
                    fragmentData.getStageId(),
                    fragmentData.getChapterId(),
                    fragmentData.getTitle(),
                    fragmentData.getContent(),
                    fragmentData.getTime(),
                    fragmentData.getAuthorName(),
                    storyBook.hasReadFragment(fragmentId)
            ));
        }
        return entries;
    }

    /**
     * 给予玩家随记本
     */
    public static void giveStoryBookToPlayer(UUID playerUuid) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        if (!storyBook.hasStoryBook()) {
            storyBook.setHasStoryBook(true);
            markPlayerDirty(playerUuid);
        }
    }

    /**
     * 检查玩家是否开始旅程
     */
    public static boolean playerJourneyStarted(UUID playerUuid) {
        StoryBookData storyBook = PLAYER_DATA_CACHE.get(playerUuid);
        return storyBook != null && storyBook.isJourneyStarted();
    }

    /**
     * 开始玩家旅程
     */
    public static void startPlayerJourney(UUID playerUuid) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        if (!storyBook.isJourneyStarted()) {
            storyBook.setJourneyStarted(true);
            markPlayerDirty(playerUuid);
        }
    }

    /**
     * 增加玩家残页使用次数
     */
    public static int incrementFragmentPageUseCount(UUID playerUuid) {
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        storyBook.incrementFragmentPageUseCount();
        markPlayerDirty(playerUuid);
        return storyBook.getFragmentPageUseCount();
    }

    /**
     * 获取玩家残页使用次数
     */
    public static int getFragmentPageUseCount(UUID playerUuid) {
        StoryBookData storyBook = PLAYER_DATA_CACHE.get(playerUuid);
        return storyBook != null ? storyBook.getFragmentPageUseCount() : 0;
    }

    public static void openStoryBook(ServerPlayer player) {
        EconomySystem_NetworkManager.sendToClient(
                new Packet_OpenStoryBookGUI(getStoryBookEntriesForPlayer(player.getUUID())),
                player
        );
    }

    public static void updateFragmentOrderForPlayer(UUID playerUuid, List<Integer> orderedFragmentIds) {
        if (orderedFragmentIds == null) {
            return;
        }

        StoryBookData storyBook = getPlayerStoryBook(playerUuid);
        storyBook.setObtainedOrder(orderedFragmentIds);
        markPlayerDirty(playerUuid);
    }

    public static boolean useFragmentPage(ServerPlayer player) {
        return useFragmentPage(player, null);
    }

    /**
     * 使用片段残页。
     * 如果物品指定了 fragmentId，则直接读取对应 json 片段；否则按默认顺序补全下一条内容。
     */
    public static boolean useFragmentPage(ServerPlayer player, Integer specifiedFragmentId) {
        UUID playerUuid = player.getUUID();
        StoryBookData storyBook = getPlayerStoryBook(playerUuid);

        if (specifiedFragmentId == null) {
            player.sendSystemMessage(Component.literal("§c这张残页没有绑定编号，无法整理出新的内容。"));
            return false;
        }

        if (!hasFragment(specifiedFragmentId)) {
            player.sendSystemMessage(Component.literal("§c指定的残页编号不存在：§f" + specifiedFragmentId));
            return false;
        }

        boolean firstUse = !storyBook.isJourneyStarted();
        giveStoryBookToPlayer(playerUuid);
        ensurePlayerHasStoryBookItem(player);

        if (firstUse) {
            startPlayerJourney(playerUuid);
            unlockChapterForPlayer(playerUuid, 0);
            grantJourneyStartedAdvancement(player);
            player.sendSystemMessage(Component.literal("§6已解锁成就：§e远旅开端"));
        }

        incrementFragmentPageUseCount(playerUuid);

        int fragmentIdToDisplay = specifiedFragmentId;
        if (!storyBook.hasUnlockedFragment(fragmentIdToDisplay)) {
            unlockFragmentForPlayer(playerUuid, fragmentIdToDisplay);
            FragmentData fragmentData = getFragment(fragmentIdToDisplay);
            if (fragmentData != null) {
                unlockChapterForPlayer(playerUuid, fragmentData.getChapterId());
                player.sendSystemMessage(Component.literal("§a你拼出了新的内容：§f" + fragmentData.getTitle()));
            }
        } else {
            player.sendSystemMessage(Component.literal("§7你翻开了已收录的残页：§f" + specifiedFragmentId));
        }

        FragmentData fragmentToDisplay = getFragment(fragmentIdToDisplay);
        if (fragmentToDisplay == null) {
            player.sendSystemMessage(Component.literal("§c这张残页上没有可读取的内容。"));
            return false;
        }

        markFragmentReadForPlayer(playerUuid, fragmentIdToDisplay);
        EconomySystem_NetworkManager.sendToClient(new Packet_OpenStoryFragmentGUI(fragmentToDisplay), player);
        levelUpStoryFeedback(player);
        markPlayerDirty(playerUuid);
        return true;
    }

    private static int getLatestUnlockedFragmentId(StoryBookData storyBook) {
        List<Integer> obtainedOrder = storyBook.getObtainedOrder();
        if (!obtainedOrder.isEmpty()) {
            return obtainedOrder.get(obtainedOrder.size() - 1);
        }
        return storyBook.getUnlockedFragmentIds().stream()
                .max(Integer::compareTo)
                .orElse(-1);
    }

    private static void ensurePlayerHasStoryBookItem(ServerPlayer player) {
        ItemStack storyBookStack = new ItemStack(EconomySystem_Items.STORY_BOOK.get());
        if (player.getInventory().contains(storyBookStack)) {
            return;
        }

        if (!player.addItem(storyBookStack)) {
            player.drop(storyBookStack, false);
        }
    }

    private static void grantJourneyStartedAdvancement(ServerPlayer player) {
        var advancement = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "storybook/journey_started"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "triggered");
        }
    }

    private static void levelUpStoryFeedback(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }
}
