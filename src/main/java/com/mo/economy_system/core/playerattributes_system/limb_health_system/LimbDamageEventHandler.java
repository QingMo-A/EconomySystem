package com.mo.economy_system.core.playerattributes_system.limb_health_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 肢体伤害事件处理器
 * <p>
 * 处理近战和弹射物攻击，根据击中部位应用不同的伤害倍率：
 * - 头部: ×1.2
 * - 胸部: ×1.0 (默认)
 * - 腿部: ×0.9
 * - 脚部: ×0.9
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LimbDamageEventHandler {

    /**
     * 存储弹射物击中信息（玩家UUID -> 弹射物位置）
     * 在 ProjectileImpactEvent 中记录，在 LivingHurtEvent 中使用
     */
    private static final Map<UUID, Vec3> PENDING_PROJECTILE_HITS = new ConcurrentHashMap<>();

    /**
     * 是否启用肢体伤害系统
     */
    public static boolean ENABLED = true;

    /**
     * 是否显示调试信息
     */
    public static boolean DEBUG = true;

    /**
     * 监听弹射物击中事件，记录击中位置
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        var rayTraceResult = event.getRayTraceResult();
        if (rayTraceResult.getType() != rayTraceResult.getType().ENTITY) {
            return;
        }

        Entity hitEntity = ((net.minecraft.world.phys.EntityHitResult) rayTraceResult).getEntity();
        if (hitEntity instanceof Player player && !player.level().isClientSide) {
            // 记录弹射物当前位置（用于判断击中哪个部位）
            PENDING_PROJECTILE_HITS.put(player.getUUID(), event.getEntity().position());
        }
    }

    /**
     * 监听玩家受伤事件，应用肢体伤害倍率
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 检查是否启用
        if (!ENABLED) {
            return;
        }

        // 只处理服务端玩家
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 跳过创造模式玩家
        if (player.getAbilities().invulnerable) {
            return;
        }

        float originalDamage = event.getAmount();
        LimbType hitPart = determineHitPart(player, event.getSource());

        // 环境伤害不应用倍率
        if (hitPart == null) {
            return;
        }

        // 应用伤害倍率
        float multiplier = LimbDamageConfig.getMultiplier(hitPart);
        float newDamage = originalDamage * multiplier;

        // 更新伤害值
        event.setAmount(newDamage);

        // 清理弹射物记录
        PENDING_PROJECTILE_HITS.remove(player.getUUID());

        // 调试日志
        if (DEBUG) {
            var source = event.getSource();
            String attackerName = source.getEntity() != null
                    ? source.getEntity().getType().getDescription().getString()
                    : source.getMsgId();

            EconomySystem.LOGGER.info("[肢体伤害] 玩家: {}, 攻击者: {}, 击中部位: {}, 原伤害: {}, 新伤害: {}, 倍率: {}",
                    player.getScoreboardName(),
                    attackerName,
                    hitPart.getDisplayName(),
                    String.format("%.1f", originalDamage),
                    String.format("%.1f", newDamage),
                    String.format("%.1f", multiplier));
        }
    }

    /**
     * 判断击中哪个部位
     *
     * @param player 被攻击的玩家
     * @param source 伤害来源
     * @return 击中的肢体部位，null 表示环境伤害不应用倍率
     */
    private static LimbType determineHitPart(Player player, net.minecraft.world.damagesource.DamageSource source) {
        // ========== 弹射物伤害 ==========
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            Vec3 projectilePos = PENDING_PROJECTILE_HITS.get(player.getUUID());
            if (projectilePos != null) {
                return LimbHitCalculator.getHitPartByProjectilePosition(player, projectilePos);
            }
            // 弹射物位置未记录，使用默认胸部
            return LimbType.CHEST;
        }

        // ========== 生物实体近战伤害（怪物、玩家等）==========
        Entity attacker = source.getEntity();
        if (attacker instanceof net.minecraft.world.entity.LivingEntity livingAttacker) {
            // 检查是否是直接攻击（非弹射物）
            if (attacker == source.getDirectEntity()) {
                // 玩家攻击：根据瞄准角度判断部位
                if (livingAttacker instanceof Player playerAttacker) {
                    return LimbHitCalculator.getHitPartByPlayerAim(player, playerAttacker);
                }
                // 怪物攻击：根据手部高度判断部位
                return LimbHitCalculator.getHitPartByAttackerHeight(player, livingAttacker);
            }
        }

        // ========== 环境伤害（跌落、火焰、魔法等）==========
        // 不应用肢体伤害倍率
        return null;
    }

    /**
     * 每tick清理过期的弹射物记录
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // 简单的清理策略：如果记录超过 1 tick 就删除
        // 实际上在 onPlayerHurt 中会正常清理，这里只是防止意外残留
        // 由于 ConcurrentHashMap 不会在遍历时抛出异常，直接清理即可
        PENDING_PROJECTILE_HITS.clear();
    }
}
