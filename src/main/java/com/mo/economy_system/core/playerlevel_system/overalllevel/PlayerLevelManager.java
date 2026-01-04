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
     * 计算指定等级所需的总经验
     * @param level 目标等级
     * @return 该等级升级所需的总经验
     */
    private static long getExperienceRequiredForLevel(int level) {
        // 示例占位符：请替换为你的经验需求公式（支持无限等级）
        // 例如1：线性递增：return (long) level * 1000;
        // 例如2：指数递增：return (long) (Math.pow(level, 1.5) * 1000);
        // 目前留空，返回临时值，后续修改
        return 0L;
    }

    /**
     * 服务端设置玩家等级
     */
    public static void setPlayerLevelServer(ServerPlayer serverPlayer, int level) {
        if (serverPlayer == null) return;
        PlayerData playerData = PlayerDataManager.getPlayerData(serverPlayer.getUUID());
        playerData.setLevel(level);
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), level);
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

        // 1. 累加经验
        long newExp = currentExp + experienceToAdd;
        playerData.setCurrentExperience(newExp);

        // 2. 循环判断是否满足升级条件（支持一次性多段升级，例如：经验足够连升2级）
        while (true) {
            long expRequiredForNextLevel = getExperienceRequiredForLevel(currentLevel + 1);
            // 条件1：经验 >= 下一级所需总经验 → 升级
            if (newExp >= expRequiredForNextLevel) {
                // 2.1 扣除升级所需经验（保留多余经验，支持无限等级）
                newExp -= expRequiredForNextLevel;
                // 2.2 等级+1
                currentLevel += 1;
                // 2.3 更新玩家等级和经验
                playerData.setLevel(currentLevel);
                playerData.setCurrentExperience(newExp);
                // 2.4 发送升级提示（给客户端渲染左上角文字）
                sendLevelUpNotify(serverPlayer, currentLevel);
            } else {
                // 条件2：经验不足下一级 → 退出循环
                break;
            }
        }

        // 3. 持久化更新后的数据（等级+经验）
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), playerData.getLevel());
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
        PlayerDataManager.updatePlayerData(serverPlayer, playerData.getRank(), playerData.getTitle(), playerData.getLevel());
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
}