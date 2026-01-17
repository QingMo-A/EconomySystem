package com.mo.economy_system.core.playerlevel_system.overalllevel;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_LevelUpNotify;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家无限等级+经验核心管理框架
 * 功能：经验累积、满经验升级、升级发提示、等级/经验获取与设置
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLevelManager {
    /**
     * 计算指定等级升级所需的总经验（累积经验）
     * 分段公式：1-50级简单期，51级+困难期
     * @param level 目标等级
     * @return 升级到该等级所需的累积总经验
     */
    private static long getExperienceRequiredForLevel(int level) {
        if (level <= 1) return 0L;

        if (level <= 50) {
            // ===== 简单期（1-50级）：低难度线性增长 =====
            // 公式：level × 500 + level² × 20
            // 示例：
            // Lv.1 -> Lv.2: 520 经验
            // Lv.10: 7,000 经验
            // Lv.20: 18,000 经验
            // Lv.30: 33,000 经验
            // Lv.50: 75,000 经验
            long levelSquared = (long) level * level;
            return level * 500 + levelSquared * 20;
        } else {
            // ===== 困难期（51级+）：高难度指数增长 =====
            // 公式：(level-50)³ × 500 + (level-50)² × 5000 + (level-50) × 10000 + 75000
            // 从51级开始，以50级的75,000为基数，难度指数级上升
            // 示例：
            // Lv.51: 80,500 经验（+5,500）
            // Lv.60: 175,000 经验（+100,000）
            // Lv.70: 475,000 经验（+400,000）
            // Lv.100: 1,575,000 经验（+1,500,000）
            int levelOver50 = level - 50;
            long levelCubed = (long) levelOver50 * levelOver50 * levelOver50;
            long levelSquared = (long) levelOver50 * levelOver50;
            long baseExp = 75000; // 50级时的经验值
            return baseExp + levelCubed * 500 + levelSquared * 5000 + levelOver50 * 10000;
        }
    }

    /**
     * 获取从当前等级升级到下一级所需的经验
     * @param currentLevel 当前等级
     * @return 升级到下一级所需的额外经验
     */
    public static long getExperienceNeededForNextLevel(int currentLevel) {
        long currentLevelTotalExp = getExperienceRequiredForLevel(currentLevel);
        long nextLevelTotalExp = getExperienceRequiredForLevel(currentLevel + 1);
        return nextLevelTotalExp - currentLevelTotalExp;
    }

    /**
     * 获取当前等级已获得的累积经验（用于显示进度条）
     * @param currentLevel 当前等级
     * @param currentExp 当前等级内的经验
     * @return 当前等级的总累积经验
     */
    public static long getTotalExperienceCurrentLevel(int currentLevel, long currentExp) {
        return getExperienceRequiredForLevel(currentLevel) + currentExp;
    }

    /**
     * 服务端设置玩家等级
     */
    public static void setPlayerLevelServer(ServerPlayer serverPlayer, int level) {
        if (serverPlayer == null) return;
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        playerData.setLevel(level);
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), level, playerData.getCurrentExperience());
        //发送升级提示
        sendLevelUpNotify(serverPlayer, level);
    }
    public static int getPlayerLevelServer(ServerPlayer serverPlayer) {
        if (serverPlayer == null) return 0;
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getLevel();
    }


    /**
     * 服务端给玩家添加经验（核心：经验累积入口）
     * @param serverPlayer 目标玩家
     * @param experienceToAdd 要添加的经验值
     */
    public static void addPlayerExperienceServer(ServerPlayer serverPlayer, long experienceToAdd) {
        if (serverPlayer == null || experienceToAdd <= 0) return;

        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        int currentLevel = playerData.getLevel();
        long currentExp = playerData.getCurrentExperience();

        //累加经验
        long newExp = currentExp + experienceToAdd;
        playerData.setCurrentExperience(newExp);

        //循环判断是否满足升级条件（支持一次性多段升级，例如：经验足够连升2级）
        while (true) {
            long expRequiredForNextLevel = getExperienceRequiredForLevel(currentLevel + 1) - getExperienceRequiredForLevel(currentLevel);
            // 经验 >= 下一级所需总经验 → 升级
            if (newExp >= expRequiredForNextLevel) {
                //扣除升级所需经验（保留多余经验，支持无限等级）
                newExp -= expRequiredForNextLevel;
                //等级+1
                currentLevel += 1;
                //更新玩家等级和经验
                playerData.setLevel(currentLevel);
                playerData.setCurrentExperience(newExp);
                //发送升级提示（给客户端渲染左上角文字）
                sendLevelUpNotify(serverPlayer, currentLevel);
            } else {
                //经验不足下一级 → 退出循环
                break;
            }
        }

        // 3. 持久化更新后的数据（等级+经验）
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), playerData.getLevel(), playerData.getCurrentExperience());
    }

    /**
     * 服务端获取玩家当前经验
     */
    public static long getPlayerExperienceServer(ServerPlayer serverPlayer) {
        if (serverPlayer == null) return 0L;
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        return playerData.getCurrentExperience();
    }

    /**
     * 服务端设置玩家当前经验
     */
    public static void setPlayerExperienceServer(ServerPlayer serverPlayer, long experience) {
        if (serverPlayer == null) return;
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        playerData.setCurrentExperience(Math.max(0, experience)); // 经验不能为负
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), playerData.getLevel(), playerData.getCurrentExperience());
    }


    /**
     * 服务端发送升级提示网络包给客户端
     * @param serverPlayer 升级的玩家
     * @param newLevel 升级后的新等级
     */
    private static void sendLevelUpNotify(ServerPlayer serverPlayer, int newLevel) {
        if (serverPlayer == null) return;
        EconomySystem_NetworkManager.INSTANCE.sendTo(
                new Packet_LevelUpNotify(newLevel),
                serverPlayer.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
        );
    }

    //客户端缓存
    private static final Map<UUID, Integer> CLIENT_LEVEL_CACHE = new HashMap<>();
    private static final Map<UUID, Long> CLIENT_EXPERIENCE_CACHE = new HashMap<>(); // 新增客户端经验缓存

    public static void setPlayerLevelClient(Player clientPlayer, int level) {
        if (clientPlayer == null) return;
        CLIENT_LEVEL_CACHE.put(clientPlayer.getUUID(), level);
    }

    public static int getPlayerLevelClient(Player clientPlayer) {
        if (clientPlayer == null) return 0;
        return CLIENT_LEVEL_CACHE.getOrDefault(clientPlayer.getUUID(), 0);
    }

    // 客户端经验操作
    public static void setPlayerExperienceClient(Player clientPlayer, long experience) {
        if (clientPlayer == null) return;
        CLIENT_EXPERIENCE_CACHE.put(clientPlayer.getUUID(), Math.max(0, experience));
    }

    public static long getPlayerExperienceClient(Player clientPlayer) {
        if (clientPlayer == null) return 0L;
        return CLIENT_EXPERIENCE_CACHE.getOrDefault(clientPlayer.getUUID(), 0L);
    }

    /**
     * 客户端获取从当前等级升级到下一级所需的经验
     * @param clientPlayer 客户端玩家
     * @return 升级所需经验
     */
    public static long getExperienceNeededForNextLevelClient(Player clientPlayer) {
        if (clientPlayer == null) return 0L;
        int currentLevel = getPlayerLevelClient(clientPlayer);
        return getExperienceNeededForNextLevel(currentLevel);
    }

    /**
     * 客户端获取当前等级内的经验进度百分比（用于进度条）
     * @param clientPlayer 客户端玩家
     * @return 0.0-1.0 之间的进度值
     */
    public static float getExperienceProgressClient(Player clientPlayer) {
        if (clientPlayer == null) return 0.0f;
        int currentLevel = getPlayerLevelClient(clientPlayer);
        long currentExp = getPlayerExperienceClient(clientPlayer);
        long neededExp = getExperienceNeededForNextLevel(currentLevel);

        if (neededExp <= 0) return 0.0f;
        return (float) currentExp / neededExp;
    }
}