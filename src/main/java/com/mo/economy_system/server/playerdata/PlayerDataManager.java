package com.mo.economy_system.server.playerdata;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.server.LoginSync;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.notice.NewPlayerGuide;
import com.mo.economy_system.server.rank.Rank;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//初始化玩家进服数据管理，对外提供数据初始化和发送，更新的方法

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataManager {
    private static final File PLAYER_DATA_FILE = new File("config/economy_system/player_data.json");
    private static final Map<UUID, PlayerData> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    static {
        try {
            //创建目录
            if (!PLAYER_DATA_FILE.getParentFile().exists()) {
                boolean dirCreated = PLAYER_DATA_FILE.getParentFile().mkdirs();
                if (dirCreated) {
                    EconomySystem.LOGGER.info("玩家数据目录创建成功：{}", PLAYER_DATA_FILE.getParentFile().getPath());
                } else {
                    EconomySystem.LOGGER.error("玩家数据目录创建失败：{}", PLAYER_DATA_FILE.getParentFile().getPath());
                }
            }
            if (!PLAYER_DATA_FILE.exists()) {
                boolean fileCreated = PLAYER_DATA_FILE.createNewFile();
                if (fileCreated) {
                    EconomySystem.LOGGER.info("玩家数据文件创建成功：{}", PLAYER_DATA_FILE.getPath());
                } else {
                    EconomySystem.LOGGER.error("玩家数据文件创建失败：{}", PLAYER_DATA_FILE.getPath());
                }
            } else {
                EconomySystem.LOGGER.info("玩家数据文件已存在：{}", PLAYER_DATA_FILE.getPath());
            }
        } catch (IOException e) {
            EconomySystem.LOGGER.error("初始化玩家数据文件失败", e);
        }
    }


    public static boolean hasPlayerData(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        //优先查缓存
        if (PLAYER_DATA_CACHE.containsKey(playerUUID)) {
            return true;
        }
        //缓存无则查文件
        Map<UUID, PlayerData> allPlayerData = loadAllPlayerDataFromFile();
        return allPlayerData.containsKey(playerUUID);
    }

    public static void initPlayerData(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        //避免重复初始化
        if (hasPlayerData(player)) {
            EconomySystem.LOGGER.info("玩家 {} 已有数据，无需重复初始化", player.getScoreboardName());
            //同步到缓存
            PlayerData existingData = getPlayerData(playerUUID);
            PLAYER_DATA_CACHE.put(playerUUID, existingData);
            PlayerAttributesDataManager.initPlayerAttributesData(player, existingData.getLevel());
            return;
        }

        PlayerData newPlayerData = new PlayerData(player);
        PLAYER_DATA_CACHE.put(playerUUID, newPlayerData);
        //写入文件
        Map<UUID, PlayerData> allPlayerData = loadAllPlayerDataFromFile();
        allPlayerData.put(playerUUID, newPlayerData);
        saveAllPlayerDataToFile(allPlayerData);

        //新手教程
        NewPlayerGuide.sendNewPlayerGuide(player);

        EconomySystem.LOGGER.info("新玩家 {} 数据初始化完成（默认Rank={}, Title={}, Level={}）",
                player.getScoreboardName(),
                newPlayerData.getRank().getRankName(),
                newPlayerData.getTitle().getTitleName(),
                newPlayerData.getLevel());

        //初始化玩家属性数据
        PlayerAttributesDataManager.initPlayerAttributesData(player, newPlayerData.getLevel());
    }

    public static PlayerData getPlayerData(UUID playerUUID) {
        //优先查缓存
        if (PLAYER_DATA_CACHE.containsKey(playerUUID)) {
            return PLAYER_DATA_CACHE.get(playerUUID);
        }

        //缓存无则查文件
        Map<UUID, PlayerData> allPlayerData = loadAllPlayerDataFromFile();
        PlayerData playerData = allPlayerData.get(playerUUID);

        //文件也无则返回默认数据
        if (playerData == null) {
            EconomySystem.LOGGER.warn("玩家 {} 无数据，返回默认数据", playerUUID);
            playerData = new PlayerData();
        } else {
            //同步到缓存
            PLAYER_DATA_CACHE.put(playerUUID, playerData);
        }
        return playerData;
    }

    public static void updatePlayerData(ServerPlayer serverPlayer, Rank rank, Title title, int level) {
        UUID playerUUID = serverPlayer.getUUID();
        Map<UUID, PlayerData> allPlayerData = loadAllPlayerDataFromFile();

        PlayerData playerData = allPlayerData.get(playerUUID);
        if (playerData == null) {
            playerData = new PlayerData(serverPlayer);
            EconomySystem.LOGGER.warn("玩家 {} 无原有数据，创建新数据并更新", serverPlayer.getScoreboardName());
        } else {
            playerData.setRank(rank);
            playerData.setTitle(title);
            playerData.setLevel(level);
        }

        PLAYER_DATA_CACHE.put(playerUUID, playerData);
        allPlayerData.put(playerUUID, playerData);
        saveAllPlayerDataToFile(allPlayerData);

        PlayerAttributesDataManager.initPlayerAttributesData(serverPlayer, level);

        EconomySystem.LOGGER.info("玩家 {} 数据更新成功（Rank={}, Title={}, Level={}）",
                serverPlayer.getScoreboardName(),
                rank.getRankName(),
                title.getTitleName(),
                level);

        //同步数据包给自己和其他玩家
        LoginSync.sendSyncPacketToPlayer(serverPlayer, serverPlayer);
        LoginSync.broadcastPlayerDataToAllOnlinePlayers(serverPlayer);
    }

    private static Map<UUID, PlayerData> loadAllPlayerDataFromFile() {
        Map<UUID, PlayerData> allPlayerData = new HashMap<>();
        try (FileReader reader = new FileReader(PLAYER_DATA_FILE)) {
            // 处理空文件：避免Gson解析空字符串报错
            if (PLAYER_DATA_FILE.length() == 0) {
                return allPlayerData;
            }
            Type mapType = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            allPlayerData = GSON.fromJson(reader, mapType);
            // 兜底：Gson解析失败返回空Map
            if (allPlayerData == null) {
                allPlayerData = new HashMap<>();
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.warn("读取玩家数据文件失败，返回空数据", e);
        }
        return allPlayerData;
    }

    private static void saveAllPlayerDataToFile(Map<UUID, PlayerData> allPlayerData) {
        try (FileWriter writer = new FileWriter(PLAYER_DATA_FILE)) {
            GSON.toJson(allPlayerData, writer);
        } catch (Exception e) {
            EconomySystem.LOGGER.error("写入玩家数据文件失败", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        initPlayerData(player);

        PlayerData data = getPlayerData(player.getUUID());
        data.setLastLoginTime(System.currentTimeMillis());
        EconomySystem.LOGGER.info("玩家 {} 登录，记录登录时间: {}",
                player.getScoreboardName(), data.getLastLoginTime());
    }

    //离线清理缓存并计算在线时间
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();
            //游玩时长 当前-登录时间
            PlayerData data = getPlayerData(playerUUID);
            long onlineTime = System.currentTimeMillis() - data.getLastLoginTime();
            data.addPlayTime(onlineTime);

            Map<UUID, PlayerData> allData = loadAllPlayerDataFromFile();
            allData.put(playerUUID, data);
            saveAllPlayerDataToFile(allData);

            EconomySystem.LOGGER.info("玩家 {} 登出，本次在线: {}秒，总时长: {}秒",
                    player.getScoreboardName(),
                    onlineTime / 1000,
                    data.getTotalPlayTime() / 1000);

            PLAYER_DATA_CACHE.remove(playerUUID);
            EconomySystem.LOGGER.info("玩家 {} 缓存已清理", player.getScoreboardName());
        }
    }
}