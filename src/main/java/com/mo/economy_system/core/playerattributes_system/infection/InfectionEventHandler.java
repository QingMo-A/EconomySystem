package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 感染值事件处理
 * 处理玩家生命值变化、被丧尸击败等事件导致的感染值变化
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InfectionEventHandler {

    private static final int INFECTION_MAX = 100;
    private static final int HEALTH_CHECK_INTERVAL = 20; // 1秒检查一次生命值变化
    private static final int PROXIMITY_CHECK_INTERVAL = 600; // 30秒检查一次附近感染者（每分钟加2点，每次检查加1点）
    private static final double PROXIMITY_RADIUS = 32.0; // 检测范围：32格

    // 记录玩家上次检查时的生命值
    private static final Map<UUID, Float> LAST_HEALTH = new ConcurrentHashMap<>();

    // 记录玩家在感染者附近的检查次数（用于每分钟提示一次）
    private static final Map<UUID, Integer> NEARBY_INFECTED_CHECK_COUNT = new ConcurrentHashMap<>();

    /**
     * 玩家tick事件 - 每秒检查生命值变化，净损失时增加感染值
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        if (event.side.isClient() || !event.player.isAlive() || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        if (player.tickCount % HEALTH_CHECK_INTERVAL != 0) {
            return;
        }

        UUID playerUUID = player.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return;
        }

        // 如果已经是感染者，不再增加感染值
        if (attributesData.isInfected()) {
            LAST_HEALTH.put(playerUUID, player.getHealth());
            return;
        }

        float currentHealth = player.getHealth();
        Float lastHealth = LAST_HEALTH.get(playerUUID);

        if (lastHealth != null && currentHealth < lastHealth) {
            // 生命值净减少，增加感染值
            float healthLoss = lastHealth - currentHealth;
            float infectionIncrease = healthLoss / 5.0F;

            if (infectionIncrease > 0) {
                float currentInfection = attributesData.getCurrentInfection();
                float newInfection = Math.min(currentInfection + infectionIncrease, INFECTION_MAX);

                if (Math.abs(newInfection - currentInfection) > 0.01F) {
                    attributesData.setCurrentInfection(newInfection);
                    PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
                    PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);

                    // 调试日志
                    EconomySystem.LOGGER.info("感染值增加: 玩家{}, 生命损失:{}, 增加感染:{}, {}->{}",
                            player.getScoreboardName(), String.format("%.1f", healthLoss),
                            String.format("%.2f", infectionIncrease), String.format("%.2f", currentInfection), String.format("%.2f", newInfection));

                    // 感染值达到100时转换为感染者
                    if (newInfection >= INFECTION_MAX && !attributesData.isInfected()) {
                        attributesData.setInfected(true);
                        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
                        player.displayClientMessage(
                                Component.literal("§4§l你已经完全感染，变成了感染者！"),
                                true
                        );
                    }
                }
            }
        }
        // 更新记录的生命值
        LAST_HEALTH.put(playerUUID, currentHealth);

        // 检查附近是否有感染者，如果有则增加感染值
        // 每30秒检查一次，每次加1点，每分钟共2点
        if (player.tickCount % PROXIMITY_CHECK_INTERVAL == 0) {
            checkNearbyInfectedPlayers(player, attributesData);
        }
    }

    /**
     * 检查附近是否有感染者，如果有则增加幸存者的感染值
     * @param player 幸存者玩家
     * @param attributesData 玩家属性数据
     */
    private static void checkNearbyInfectedPlayers(ServerPlayer player, PlayerAttributesData attributesData) {
        // 如果已经是感染者，不受影响
        if (attributesData.isInfected()) {
            return;
        }

        // 如果感染值已满，不再增加
        if (attributesData.getCurrentInfection() >= INFECTION_MAX) {
            return;
        }

        // 获取所有在线玩家
        var serverPlayers = player.server.getPlayerList().getPlayers();
        boolean hasNearbyInfected = false;

        for (ServerPlayer otherPlayer : serverPlayers) {
            // 跳过自己
            if (otherPlayer.getUUID().equals(player.getUUID())) {
                continue;
            }

            // 检查对方是否是感染者
            PlayerAttributesData otherAttributes = PlayerAttributesDataManager.getPlayerAttributesData(otherPlayer.getUUID());
            if (otherAttributes == null || !otherAttributes.isInfected()) {
                continue;
            }

            // 检查距离
            double distance = player.position().distanceTo(otherPlayer.position());
            if (distance <= PROXIMITY_RADIUS) {
                hasNearbyInfected = true;
                break;
            }
        }

        // 如果附近有感染者，增加1点感染值
        if (hasNearbyInfected) {
            float currentInfection = attributesData.getCurrentInfection();
            float newInfection = Math.min(currentInfection + 1.0F, INFECTION_MAX);

            if (newInfection > currentInfection) {
                attributesData.setCurrentInfection(newInfection);
                PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
                PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);

                // 增加检查计数
                UUID playerUUID = player.getUUID();
                int checkCount = NEARBY_INFECTED_CHECK_COUNT.getOrDefault(playerUUID, 0) + 1;
                NEARBY_INFECTED_CHECK_COUNT.put(playerUUID, checkCount);

                // 每2次检查（1分钟）发送一次提示
                if (checkCount % 2 == 0) {
                    player.displayClientMessage(
                            Component.literal("§c您附近有感染者，感染值在逐渐增加..."),
                            true
                    );
                }

                // 发送提示消息（仅在第一次或达到阈值时）
                if (newInfection >= INFECTION_MAX) {
                    player.displayClientMessage(
                            Component.literal("§c你已经完全感染，变成了感染者！"),
                            true
                    );
                    // 转换为感染者
                    attributesData.setInfected(true);
                    PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
                    // 清除计数
                    NEARBY_INFECTED_CHECK_COUNT.remove(playerUUID);
                }
            }
        } else {
            // 附近没有感染者，清除计数
            NEARBY_INFECTED_CHECK_COUNT.remove(player.getUUID());
        }
    }

    /**
     * 玩家被丧尸击败事件
     * 随机增加感染值，感染值达到100时玩家变成感染者
     */
    @SubscribeEvent
    public static void onPlayerKilledByZombie(LivingDeathEvent event) {
        // 只处理服务端玩家
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 检查伤害来源是否是实体
        if (event.getSource().getEntity() == null) {
            return;
        }

        // 检查是否是丧尸类生物
        boolean isZombieKiller = isZombie(event.getSource().getEntity());

        if (!isZombieKiller) {
            return;
        }

        // 获取玩家属性数据
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
        if (attributesData == null) {
            return;
        }

        // 如果已经是感染者，不再增加感染值
        if (attributesData.isInfected()) {
            return;
        }

        // 随机增加感染值
        Random random = new Random();
        float infectionIncrease = 1 + random.nextInt(9);

        float currentInfection = attributesData.getCurrentInfection();
        float newInfection = Math.min(currentInfection + infectionIncrease, INFECTION_MAX);

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);

        // 发送消息给玩家
        player.displayClientMessage(
                Component.literal("§c你被丧尸击败了！感染值增加了 " + String.format("%.1f", infectionIncrease) + "（当前：" + String.format("%.1f", newInfection) + "/" + INFECTION_MAX + "）"),
                true
        );

        // 检查是否达到100，转换为感染者
        if (newInfection >= INFECTION_MAX) {
            attributesData.setInfected(true);
            PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);

            // 发送转换消息
            player.displayClientMessage(
                    Component.literal("§4§l你已经完全感染，变成了感染者！"),
                    true
            );

//            EconomySystem.LOGGER.info("玩家 {} 已转变为感染者", player.getScoreboardName());
        }
    }

    /**
     * 判断实体是否是丧尸类
     */
    private static boolean isZombie(net.minecraft.world.entity.Entity entity) {
        if (entity == null) {
            return false;
        }

//        // 检查是否是原版丧尸
        if (entity.getType() == EntityType.ZOMBIE) {
            return true;
        }
        // 检查是否是溺尸
        if (entity.getType() == EntityType.DROWNED) {
            return true;
        }
        // 检查是否是尸壳
        if (entity.getType() == EntityType.HUSK) {
            return true;
        }
        // 检查是否是僵尸村民
        if (entity.getType() == EntityType.ZOMBIE_VILLAGER) {
            return true;
        }
        // 检查是否是模组的 HiveZombie
        if (entity instanceof HiveZombieEntity) {
            return true;
        }

        return false;
    }
}
