package com.mo.economy_system.item.items;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.health.PlayerCustomHealthManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class AidKitEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().isAlive() || event.getEntity().level().isClientSide()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();

        // 获取手持的急救包
        ItemStack aidKitStack = Item_AidKit.getHeldAidKit(player);
        if (aidKitStack.isEmpty()) {
            // 如果没有手持急救包，清除所有可能的使用状态
            clearAllAidKitStates(player);
            return;
        }

        // 确保手持物品是Item_AidKit类型
        if (!(aidKitStack.getItem() instanceof Item_AidKit aidKit)) {
            return;
        }

        // 获取该急救包的唯一键名
        String usingKey = aidKit.getUsingKey();
        CompoundTag playerData = player.getPersistentData();
        boolean isUsing = playerData.getBoolean(usingKey);

        if (!isUsing) {
            // 重置状态
            playerData.putInt(aidKit.getDurabilityTickKey(), 0);
            playerData.remove(aidKit.getStartTimeKey());
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            return;
        }

        // 添加缓慢效果（表示正在治疗）
        if (!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
        }

        // 检查是否满足治疗条件
        boolean isHealthFull = player.getHealth() >= player.getMaxHealth();
        boolean isDurabilityEmpty = aidKitStack.getDamageValue() >= aidKitStack.getMaxDamage();

        if (isHealthFull || isDurabilityEmpty) {
            if (isHealthFull) {
                player.sendSystemMessage(Component.literal("§a生命值已满，治疗停止！"));
            }
            if (isDurabilityEmpty) {
                player.sendSystemMessage(Component.literal("§c急救包耐久耗尽！"));
            }
            aidKit.clearUsingState(player);
            return;
        }

        // 获取启动时间
        long startTime = playerData.getLong(aidKit.getStartTimeKey());
        long currentTime = player.level().getGameTime();
        boolean isDelayOver = currentTime >= startTime + aidKit.getStartDelayTicks();

        // 获取耐久计数器
        int durabilityTick = playerData.getInt(aidKit.getDurabilityTickKey());

        // 治疗逻辑
        if (isDelayOver && player.tickCount % aidKit.getHealInterval() == 0) {
            // 调用自定义回血系统
            // 注意：需要将tick转换为毫秒（1 tick = 50毫秒）
            long cooldownMillis = aidKit.getMedicineCooldown();

            boolean healSuccess = PlayerCustomHealthManager.handleMedicineHeal(
                    player,
                    aidKit.getPerHealAmount(),
                    cooldownMillis,
                    1  // 每次治疗消耗1点耐久
            );

            if (healSuccess) {
                // 更新耐久计数器（可选，因为handleMedicineHeal已经处理了耐久）
                durabilityTick++;
                if (durabilityTick >= aidKit.getDurabilityConsumeInterval()) {
                    // 这里不需要再次消耗耐久，因为handleMedicineHeal已经处理了
                    durabilityTick = 0;
                }
                playerData.putInt(aidKit.getDurabilityTickKey(), durabilityTick);

                // 播放治疗音效
                player.level().playSound(null, player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        0.3F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAllAidKitStates(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAllAidKitStates(player);
        }
    }

    /**
     * 清除玩家所有急救包的使用状态
     * 修复了ConcurrentModificationException问题
     */
    private static void clearAllAidKitStates(ServerPlayer player) {
        if (player == null) return;

        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // 移除所有包含特定前缀的键
        CompoundTag playerData = player.getPersistentData();
        if (playerData == null) return;

        // 方法1：使用副本避免并发修改
        List<String> keysToRemove = new ArrayList<>();

        // 收集需要删除的键
        for (String key : playerData.getAllKeys()) {
            if (isAidKitKey(key)) {
                keysToRemove.add(key);
            }
        }

        // 删除收集到的键
        for (String key : keysToRemove) {
            playerData.remove(key);
        }
    }

    /**
     * 检查键名是否属于急救包系统
     */
    private static boolean isAidKitKey(String key) {
        return key.startsWith("Using_") ||
                key.startsWith("DurabilityTick_") ||
                key.startsWith("StartTime_");
    }
}
