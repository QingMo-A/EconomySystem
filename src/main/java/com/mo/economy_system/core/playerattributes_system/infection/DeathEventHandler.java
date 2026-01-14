package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 玩家死亡事件处理
 * 处理玩家被丧尸击败后的感染值增加和感染者转换
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathEventHandler {

    private static final int INFECTION_MAX = 100;

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
        int infectionIncrease = 1 + random.nextInt(20); // 10-30

        int currentInfection = attributesData.getCurrentInfection();
        int newInfection = Math.min(currentInfection + infectionIncrease, INFECTION_MAX);

        attributesData.setCurrentInfection(newInfection);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, attributesData);
        PlayerInfectionClientSync.sendInfectionDataToClient(player, newInfection);

        // 发送消息给玩家
        player.displayClientMessage(
                Component.literal("§c你被丧尸击败了！感染值增加了 " + infectionIncrease + "（当前：" + newInfection + "/" + INFECTION_MAX + "）"),
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
//        if (entity.getType() == EntityType.ZOMBIE) {
//            return true;
//        }
//        // 检查是否是溺尸
//        if (entity.getType() == EntityType.DROWNED) {
//            return true;
//        }
//        // 检查是否是尸壳
//        if (entity.getType() == EntityType.HUSK) {
//            return true;
//        }
        // 检查是否是模组的 HiveZombie
        if (entity instanceof HiveZombieEntity) {
            return true;
        }

        return false;
    }
}