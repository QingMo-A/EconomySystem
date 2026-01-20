package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_DeathScreenData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

//幸存者死亡，可以花费50点复活点数死亡不掉落
//感染值死亡，直接扣除20点死亡点数
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathEventHandler {

    //死亡消耗
    private final static int RESPAWN_COST_NOT_INFECTED = 5;    //幸存者
    private final static int RESPAWN_COST_INFECTED = 20;        //感染者

    //死亡不掉落额外消耗
    private final static int KEEP_INVENTORY_COST = 30;

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 设置死亡待处理标记，阻止物品掉落
        serverPlayer.getPersistentData().putBoolean("EconomySystem_DeathPending", true);

        UUID deathPlayerUUID = serverPlayer.getUUID();
        UserBanList banList = serverPlayer.server.getPlayerList().getBans();

        PlayerAttributesData deathPlayerAttributesData = PlayerAttributesDataManager.getPlayerAttributesData(deathPlayerUUID);
        if (deathPlayerAttributesData == null) {
            return;
        }

        boolean isInfected = deathPlayerAttributesData.isInfected();
        float currentRespawnPoint = deathPlayerAttributesData.getRespawnPoint();

        // 计算消耗
        int respawnCost = isInfected ? RESPAWN_COST_INFECTED : RESPAWN_COST_NOT_INFECTED;

        // 检查复活点数是否足够
        if (currentRespawnPoint <= respawnCost) {
            // 复活点不足，封禁并踢出
            UserBanListEntry banEntry = new UserBanListEntry(
                    serverPlayer.getGameProfile(),
                    null,
                    "DeathSystem",
                    null,
                    "§c很不幸，您的复活点数耗尽...请等待一名幸存者来拯救你"
            );
            banList.add(banEntry);

            EconomySystem.LOGGER.info("玩家 {} 复活点数不足({})，已被封禁",
                    serverPlayer.getScoreboardName(), currentRespawnPoint);

            // 立即踢出玩家
            serverPlayer.connection.disconnect(Component.literal("§c很不幸，您的复活点数耗尽...请等待一名幸存者来拯救你"));
            return;
        }

        // 复活点足够，发送死亡屏幕数据包
        Component deathMessage = serverPlayer.getCombatTracker().getDeathMessage();

        // 获取死亡位置
        double deathX = serverPlayer.getX();
        double deathY = serverPlayer.getY();
        double deathZ = serverPlayer.getZ();
        String dimension = serverPlayer.level().dimension().location().toString();

        // 持久化死亡状态到玩家 NBT，防止退出后丢失
        serverPlayer.getPersistentData().putFloat("EconomySystem_DeathRespawnPoint", currentRespawnPoint);
        serverPlayer.getPersistentData().putFloat("EconomySystem_DeathNormalCost", respawnCost);
        serverPlayer.getPersistentData().putFloat("EconomySystem_DeathKeepInventoryCost", respawnCost + KEEP_INVENTORY_COST);
        serverPlayer.getPersistentData().putBoolean("EconomySystem_DeathIsInfected", isInfected);
        serverPlayer.getPersistentData().putDouble("EconomySystem_DeathX", deathX);
        serverPlayer.getPersistentData().putDouble("EconomySystem_DeathY", deathY);
        serverPlayer.getPersistentData().putDouble("EconomySystem_DeathZ", deathZ);
        serverPlayer.getPersistentData().putString("EconomySystem_DeathDimension", dimension);

        Packet_DeathScreenData packet = new Packet_DeathScreenData(
                currentRespawnPoint,
                respawnCost,
                respawnCost + KEEP_INVENTORY_COST,
                isInfected,
                deathMessage,
                deathX,
                deathY,
                deathZ,
                dimension
        );
        EconomySystem_NetworkManager.sendToClient(packet, serverPlayer);

        EconomySystem.LOGGER.info("玩家 {} 死亡状态已持久化，位置: {} {} {}",
                serverPlayer.getScoreboardName(), dimension, (int)deathX, (int)deathY, (int)deathZ);
    }

    /**
     * 清除玩家的死亡状态
     */
    public static void clearDeathState(ServerPlayer player) {
        player.getPersistentData().remove("EconomySystem_DeathPending");
        player.getPersistentData().remove("EconomySystem_DeathRespawnPoint");
        player.getPersistentData().remove("EconomySystem_DeathNormalCost");
        player.getPersistentData().remove("EconomySystem_DeathKeepInventoryCost");
        player.getPersistentData().remove("EconomySystem_DeathIsInfected");
        EconomySystem.LOGGER.info("玩家 {} 的死亡状态已清除", player.getScoreboardName());
    }

    /**
     * 检查玩家是否有未处理的死亡状态
     */
    public static boolean hasDeathState(ServerPlayer player) {
        return player.getPersistentData().getBoolean("EconomySystem_DeathPending");
    }

    /**
     * 恢复玩家的死亡状态，发送死亡数据包
     */
    public static void restoreDeathState(ServerPlayer player) {
        if (!hasDeathState(player)) {
            return;
        }

        float respawnPoint = player.getPersistentData().getFloat("EconomySystem_DeathRespawnPoint");
        float normalCost = player.getPersistentData().getFloat("EconomySystem_DeathNormalCost");
        float keepInventoryCost = player.getPersistentData().getFloat("EconomySystem_DeathKeepInventoryCost");
        boolean isInfected = player.getPersistentData().getBoolean("EconomySystem_DeathIsInfected");
        double deathX = player.getPersistentData().getDouble("EconomySystem_DeathX");
        double deathY = player.getPersistentData().getDouble("EconomySystem_DeathY");
        double deathZ = player.getPersistentData().getDouble("EconomySystem_DeathZ");
        String dimension = player.getPersistentData().getString("EconomySystem_DeathDimension");

        Component deathMessage = Component.literal("您 died");

        Packet_DeathScreenData packet = new Packet_DeathScreenData(
                respawnPoint,
                normalCost,
                keepInventoryCost,
                isInfected,
                deathMessage,
                deathX,
                deathY,
                deathZ,
                dimension
        );
        EconomySystem_NetworkManager.sendToClient(packet, player);

        EconomySystem.LOGGER.info("玩家 {} 的死亡状态已恢复", player.getScoreboardName());
    }

    //获取正常复活消耗
    public static float getNormalCost(boolean isInfected) {
        return isInfected ? RESPAWN_COST_INFECTED : RESPAWN_COST_NOT_INFECTED;
    }

    //获取保留物品复活消耗
    public static float getKeepInventoryCost(boolean isInfected) {
        return getNormalCost(isInfected) + KEEP_INVENTORY_COST;
    }
}
