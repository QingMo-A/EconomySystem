package com.mo.economy_system.core.playerattributes_system.health;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerCustomHealthManager {
    // 药品冷却缓存（key：玩家UUID，value：冷却结束时间戳（毫秒））
    private static final Map<UUID, Long> MEDICINE_COOLDOWN_MAP = new HashMap<>();

    /**
     * 处理自定义药品回血
     * @param player 服务端玩家
     * @param healAmount 本次回血数值
     * @param cooldownMillis 本次药品冷却时间
     * @return 是否回血成功（false：冷却中/已达最大血量/参数无效）
     */
    public static boolean handleMedicineHeal(ServerPlayer player, double healAmount, long cooldownMillis, int durabilityCost) {
        //非空和有效性校验
        if (player == null || healAmount <= 0 || cooldownMillis < 0) {
            return false;
        }
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();

        //冷却时间校验
        if (MEDICINE_COOLDOWN_MAP.containsKey(playerUUID)) {
            long cooldownEndTime = MEDICINE_COOLDOWN_MAP.get(playerUUID);
            // 未过冷却时间，直接返回失败
            if (currentTime < cooldownEndTime) {
                long remainingCooldown = (cooldownEndTime - currentTime) / 1000; // 转换为秒
//                EconomySystem.LOGGER.info("玩家 {} 药品冷却中，剩余{}秒", player.getScoreboardName(), remainingCooldown);
                return false;
            }
        }

        //获取玩家手中的物品栈
        ItemStack heldItemStack = player.getMainHandItem();
        // 校验物品栈是否为空 + 物品是否有耐久度 + 耐久度足够消耗
        if (heldItemStack.isEmpty() || !heldItemStack.isDamageableItem() || heldItemStack.getDamageValue() + durabilityCost > heldItemStack.getMaxDamage()) {
//            EconomySystem.LOGGER.warn("玩家 {} 物品耐久不足或无法消耗耐久", player.getScoreboardName());
            return false;
        }

        //获取玩家属性数据，执行回血
        PlayerAttributesData playerAttrData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        boolean healSuccess = playerAttrData.restoreCustomHealth(player, healAmount);

        //回血成功：设置冷却时间 + 消耗药品
        if (healSuccess) {
            // 记录冷却结束时间（当前时间 + 传入的冷却毫秒数）
            MEDICINE_COOLDOWN_MAP.put(playerUUID, currentTime + cooldownMillis);
            // 消耗主手物品（1个），若需要副手可自行修改
            heldItemStack.hurt(durabilityCost, player.getRandom(), player);;
//            EconomySystem.LOGGER.info("玩家 {} 药品回血成功：+{}血量，冷却{}毫秒",
//                    player.getScoreboardName(), healAmount, cooldownMillis);
        }

        return healSuccess;
    }
}