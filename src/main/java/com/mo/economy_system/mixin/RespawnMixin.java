package com.mo.economy_system.mixin;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.death.DeathItemStorage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Respawn Mixin - 处理死亡物品存储
 * 当玩家设置了 EconomySystem_DeathPending 标记时，存储物品副本用于后续"普通死亡"掉落
 * 注意：keepInventory 已被强制开启，这里只需要存储副本即可
 */
@Mixin(LivingEntity.class)
public class RespawnMixin {

    /**
     * 注入 dropAllDeathLoot 方法
     * 如果是玩家且设置了 EconomySystem_DeathPending 标记，存储物品副本
     */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void economySystem$onDropAllDeathLoot(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // 只处理玩家
        if (!(entity instanceof Player player)) {
            return;
        }

        // 检查是否有待处理的死亡（等待玩家选择）
        if (player.getPersistentData().getBoolean("EconomySystem_DeathPending")) {
            // 存储玩家物品副本（用于玩家选择"普通死亡"时在死亡点掉落）
            DeathItemStorage.storePlayerInventory(player);

            EconomySystem.LOGGER.info("玩家 {} 的物品副本已存储（keepInventory已开启，物品原样保留）", player.getScoreboardName());
        }
    }
}
