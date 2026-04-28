package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_DeathScreenData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;
import java.util.UUID;

//幸存者死亡，可以花费50点复活点数死亡不掉落
//感染值死亡，直接扣除20点死亡点数
@EventBusSubscriber(modid = EconomySystem.MODID)
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

        // 检查复活点数是否足够（严格小于消耗时才封禁）
        if (currentRespawnPoint < respawnCost) {
            // 复活点不足，先掉落所有物品，然后封禁并踢出
            dropAllInventoryItems(serverPlayer);

            // 复活玩家（不扣除点数），避免重连时显示死亡界面
            float maxHealth = (float) deathPlayerAttributesData.getMaxHealth();
            serverPlayer.setHealth(maxHealth);
            serverPlayer.deathTime = 0;

            // 传送到复活点
            teleportToRespawnPosition(serverPlayer);

            // 清除死亡待处理标记（物品已掉落，不需要再显示死亡界面）
            clearDeathState(serverPlayer);

            UserBanListEntry banEntry = new UserBanListEntry(
                    serverPlayer.getGameProfile(),
                    null,
                    "DeathSystem",
                    null,
                    "§c很不幸，您的复活点数耗尽...请等待一名幸存者来拯救你"
            );
            banList.add(banEntry);

            EconomySystem.LOGGER.info("玩家 {} 复活点数不足({})，物品已掉落，已被封禁",
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
        // 保存死亡消息
        serverPlayer.getPersistentData().putString("EconomySystem_DeathMessage", Component.Serializer.toJson(deathMessage, serverPlayer.registryAccess()));

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

//        EconomySystem.LOGGER.info("玩家 {} 死亡状态已持久化，位置: {} {} {}",
//                serverPlayer.getScoreboardName(), dimension, (int)deathX, (int)deathY, (int)deathZ);
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
        player.getPersistentData().remove("EconomySystem_DeathMessage");
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
     * 注意：使用玩家当前的实际复活点数，而不是存储的旧值
     */
    public static void restoreDeathState(ServerPlayer player) {
        if (!hasDeathState(player)) {
            return;
        }

        // 获取玩家当前的实际属性数据
        PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
        if (data == null) {
            EconomySystem.LOGGER.warn("玩家 {} 重连时无法获取属性数据", player.getScoreboardName());
            return;
        }

        // 使用当前复活点数和感染状态
        float currentRespawnPoint = data.getRespawnPoint();
        boolean isInfected = data.isInfected();

        // 根据当前状态重新计算消耗
        float normalCost = getNormalCost(isInfected);
        float keepInventoryCost = getKeepInventoryCost(isInfected);

        // 从 NBT 读取死亡位置和消息
        double deathX = player.getPersistentData().getDouble("EconomySystem_DeathX");
        double deathY = player.getPersistentData().getDouble("EconomySystem_DeathY");
        double deathZ = player.getPersistentData().getDouble("EconomySystem_DeathZ");
        String dimension = player.getPersistentData().getString("EconomySystem_DeathDimension");

        // 读取保存的死亡消息
        Component deathMessage;
        String deathMessageJson = player.getPersistentData().getString("EconomySystem_DeathMessage");
        if (deathMessageJson != null && !deathMessageJson.isEmpty()) {
            try {
                deathMessage = Component.Serializer.fromJson(deathMessageJson, player.registryAccess());
            } catch (Exception e) {
                deathMessage = Component.literal("您 died");
            }
        } else {
            deathMessage = Component.literal("您 died");
        }

        // 发送死亡界面数据包（使用当前数据）
        Packet_DeathScreenData packet = new Packet_DeathScreenData(
                currentRespawnPoint,  // 使用当前复活点数
                normalCost,            // 重新计算的消耗
                keepInventoryCost,     // 重新计算的消耗
                isInfected,            // 当前感染状态
                deathMessage,
                deathX,
                deathY,
                deathZ,
                dimension
        );
        EconomySystem_NetworkManager.sendToClient(packet, player);

        EconomySystem.LOGGER.info("玩家 {} 的死亡状态已恢复，当前复活点: {}",
                player.getScoreboardName(), currentRespawnPoint);
    }

    //获取正常复活消耗
    public static float getNormalCost(boolean isInfected) {
        return isInfected ? RESPAWN_COST_INFECTED : RESPAWN_COST_NOT_INFECTED;
    }

    //获取保留物品复活消耗
    public static float getKeepInventoryCost(boolean isInfected) {
        return getNormalCost(isInfected) + KEEP_INVENTORY_COST;
    }

    /**
     * 将玩家传送到其复活点（床/重生锚/世界出生点）
     */
    private static void teleportToRespawnPosition(ServerPlayer player) {
        // 获取玩家的复活点设置
        BlockPos respawnPos = player.getRespawnPosition();
        ResourceKey<Level> respawnDim = player.getRespawnDimension();
        float respawnAngle = player.getRespawnAngle();
        boolean respawnForced = player.isRespawnForced();

        ServerLevel targetLevel;
        Vec3 targetPos;

        // 尝试使用玩家设置的复活点（床/重生锚）
        if (respawnPos != null && respawnDim != null) {
            targetLevel = player.server.getLevel(respawnDim);
            if (targetLevel != null) {
                targetPos = Vec3.atBottomCenterOf(respawnPos);
                player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, respawnAngle, 0);
                EconomySystem.LOGGER.info("玩家 {} 已传送到复活点: {} {} {}",
                        player.getScoreboardName(), (int)targetPos.x, (int)targetPos.y, (int)targetPos.z);
                return;
            }
        }

        // 没有有效复活点，传送到世界出生点
        targetLevel = player.server.overworld();
        BlockPos spawnPos = targetLevel.getSharedSpawnPos();
        targetPos = Vec3.atBottomCenterOf(spawnPos);
        player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, 0, 0);
        EconomySystem.LOGGER.info("玩家 {} 已传送到世界出生点: {} {} {}",
                player.getScoreboardName(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
    }

    /**
     * 掉落玩家所有物品（主物品栏、盔甲栏、副手栏）
     */
    private static void dropAllInventoryItems(ServerPlayer player) {
        // 掉落主物品栏
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
                player.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }

        // 掉落盔甲栏
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
                player.getInventory().armor.set(i, ItemStack.EMPTY);
            }
        }

        // 掉落副手栏
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = player.getInventory().offhand.get(i);
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
                player.getInventory().offhand.set(i, ItemStack.EMPTY);
            }
        }

        EconomySystem.LOGGER.info("玩家 {} 的所有物品已在死亡点掉落", player.getScoreboardName());
    }
}
