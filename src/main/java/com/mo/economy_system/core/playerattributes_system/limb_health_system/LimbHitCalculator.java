package com.mo.economy_system.core.playerattributes_system.limb_health_system;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 肢体击中计算器
 * 基于攻击者手部/嘴部高度与玩家身体高度的对比，判断击中哪个部位
 */
public class LimbHitCalculator {

    // ========== 玩家身体部位高度范围（相对于玩家脚下 y=0） ==========

    /**
     * 头部高度范围
     */
    public static final double HEAD_BOTTOM = 1.44;
    public static final double HEAD_TOP = 1.80;

    /**
     * 胸部高度范围
     */
    public static final double CHEST_BOTTOM = 0.81;
    public static final double CHEST_TOP = 1.44;

    /**
     * 腿部高度范围
     */
    public static final double LEGS_BOTTOM = 0.27;
    public static final double LEGS_TOP = 0.81;

    /**
     * 脚部高度范围
     */
    public static final double FEET_BOTTOM = 0.0;
    public static final double FEET_TOP = 0.27;

    /**
     * 根据攻击者手部高度判断击中哪个部位
     *
     * @param player 被攻击的玩家
     * @param mob    攻击的怪物
     * @return 击中的肢体部位
     */
    public static LimbType getHitPartByMobHandHeight(Player player, Mob mob) {
        double mobHandY = getMobAttackPointHeight(mob);
        double playerFeetY = player.getY();

        // 转换为相对于玩家脚部的坐标
        double relativeHandY = mobHandY - playerFeetY;

        // 获取当前玩家姿势下的身体部位范围
        BodyPartRanges ranges = getBodyPartRanges(player);

        // 根据高度判断部位
        if (relativeHandY >= ranges.headBottom) {
            return LimbType.HEAD;
        } else if (relativeHandY >= ranges.chestBottom) {
            return LimbType.CHEST;
        } else if (relativeHandY >= ranges.legsBottom) {
            return LimbType.LEGS;
        } else {
            return LimbType.FEET;
        }
    }

    /**
     * 根据攻击者手部高度判断击中哪个部位（通用方法，支持所有 LivingEntity）
     *
     * @param player 被攻击的玩家
     * @param attacker 攻击者（可以是怪物、玩家等）
     * @return 击中的肢体部位
     */
    public static LimbType getHitPartByAttackerHeight(Player player, net.minecraft.world.entity.LivingEntity attacker) {
        double attackerHandY = getLivingEntityAttackPointHeight(attacker);
        double playerFeetY = player.getY();

        // 转换为相对于玩家脚部的坐标
        double relativeHandY = attackerHandY - playerFeetY;

        // 获取当前玩家姿势下的身体部位范围
        BodyPartRanges ranges = getBodyPartRanges(player);

        // 根据高度判断部位
        if (relativeHandY >= ranges.headBottom) {
            return LimbType.HEAD;
        } else if (relativeHandY >= ranges.chestBottom) {
            return LimbType.CHEST;
        } else if (relativeHandY >= ranges.legsBottom) {
            return LimbType.LEGS;
        } else {
            return LimbType.FEET;
        }
    }

    /**
     * 根据弹射物击中位置判断击中哪个部位
     *
     * @param player       被攻击的玩家
     * @param projectilePos 弹射物当前位置
     * @return 击中的肢体部位
     */
    public static LimbType getHitPartByProjectilePosition(Player player, Vec3 projectilePos) {
        double projectileY = projectilePos.y;
        double playerFeetY = player.getY();

        // 转换为相对于玩家脚部的坐标
        double relativeY = projectileY - playerFeetY;

        // 获取当前玩家姿势下的身体部位范围
        BodyPartRanges ranges = getBodyPartRanges(player);

        // 根据高度判断部位
        if (relativeY >= ranges.headBottom) {
            return LimbType.HEAD;
        } else if (relativeY >= ranges.chestBottom) {
            return LimbType.CHEST;
        } else if (relativeY >= ranges.legsBottom) {
            return LimbType.LEGS;
        } else {
            return LimbType.FEET;
        }
    }

    /**
     * 获取怪物攻击点高度（世界坐标 Y）
     * <p>
     * 不同怪物的攻击点位置：
     * - 双足怪物（僵尸、骷髅）：手部在高度的 60-65%
     * - 四足动物（狼、豹）：嘴部在高度的 40-45%
     * - 高个怪物（末影人）：手部在高度的 50-55%
     *
     * @param mob 怪物实体
     * @return 攻击点的 Y 坐标
     */
    public static double getMobAttackPointHeight(Mob mob) {
        double mobY = mob.getY();
        double mobHeight = mob.getBbHeight();

        // 获取怪物攻击高度比例
        double heightRatio = getMobAttackHeightRatio(mob);

        // 考虑怪物的攻击动画（手臂抬起时会更高）
        float attackAnim = mob.getAttackAnim(0.0F);
        if (attackAnim > 0) {
            // 攻击时攻击点抬高约 15%
            heightRatio += 0.15 * attackAnim;
        }

        return mobY + mobHeight * heightRatio;
    }

    /**
     * 获取生物实体攻击点高度（世界坐标 Y）
     * <p>
     * 通用方法，支持所有 LivingEntity（怪物、玩家等）
     * <p>
     * 对于玩家，会根据瞄准角度调整攻击点位置
     *
     * @param entity 生物实体
     * @return 攻击点的 Y 坐标
     */
    public static double getLivingEntityAttackPointHeight(net.minecraft.world.entity.LivingEntity entity) {
        double entityY = entity.getY();
        double entityHeight = entity.getBbHeight();

        double heightRatio;
        if (entity instanceof Mob mob) {
            // 怪物使用特定的攻击高度比例
            heightRatio = getMobAttackHeightRatio(mob);
            // 考虑攻击动画
            float attackAnim = mob.getAttackAnim(0.0F);
            if (attackAnim > 0) {
                heightRatio += 0.15 * attackAnim;
            }
        } else if (entity instanceof Player player) {
            // 玩家根据瞄准角度判断攻击部位
            heightRatio = getPlayerAttackHeightRatioByAim(player);
        } else {
            // 其他生物默认 55%
            heightRatio = 0.55;
        }

        return entityY + entityHeight * heightRatio;
    }

    /**
     * 根据玩家瞄准角度获取攻击高度比例
     * <p>
     * 瞄准角度（XRot/俯仰角）:
     * - 向上瞄准（正数，如 +30°）→ 瞄准头部 → 高比例
     * - 水平瞄准（0°）→ 瞄准胸部 → 中等比例
     * - 向下瞄准（负数，如 -30°）→ 瞄准腿部/脚部 → 低比例
     *
     * @param player 攻击的玩家
     * @return 攻击点占身高的比例（0.0 ~ 1.0）
     */
    public static double getPlayerAttackHeightRatioByAim(Player player) {
        // 获取玩家俯仰角（向上为正，向下为负，单位：度）
        float xRot = player.getXRot();

        // 将角度转换为比例：
        // +30°（向上）→ 头部 → 约 80%
        //   0°（水平）→ 胸部 → 约 60%
        // -30°（向下）→ 腿部 → 约 35%
        // -60°（向下）→ 脚部 → 约 10%

        // 基础比例 60%（胸部位置）
        // 每向上1度，比例增加约 0.67%（30度增加20%）
        // 每向下1度，比例减少约 0.83%（60度减少50%）
        double ratio = 0.62;

        if (xRot > 0) {
            // 向上瞄准，最多到头部顶部
            ratio += Math.min(xRot / 30.0 * 0.20, 0.20);
        } else {
            // 向下瞄准，最多到脚部
            ratio += Math.max(xRot / 60.0 * 0.50, -0.55);
        }

        // 限制范围在 [0.05, 0.95] 之间
        return Math.max(0.05, Math.min(0.95, ratio));
    }

    /**
     * 根据玩家摄像机视线精确判断击中部位
     * <p>
     * 使用射线追踪（Ray Trace）计算玩家视线与受害者碰撞盒的精确交点
     * 这是最准确的方法，直接对应玩家看到的部位
     * <p>
     * 原理：
     * 1. 从玩家眼睛位置发射射线
     * 2. 沿玩家视角方向延伸
     * 3. 计算射线与受害者碰撞盒的交点
     * 4. 根据交点高度判断击中部位
     *
     * @param victim    受害者
     * @param attacker  攻击者玩家
     * @return 击中的肢体部位
     */
    public static LimbType getHitPartByPlayerAim(Player victim, Player attacker) {
        // ========== 获取射线起点和方向 ==========
        // 玩家眼睛位置（射线起点）
        Vec3 eyePos = attacker.getEyePosition(1.0F);  // partialTicks = 1.0

        // 玩家视线方向（单位向量）
        Vec3 lookVec = attacker.getLookAngle();

        // 射线终点（眼睛位置 + 视线方向 * 攻击距离）
        double reachDistance = 4.5;  // 玩家攻击距离约 4.5 格
        Vec3 endPos = eyePos.add(lookVec.scale(reachDistance));

        // ========== 计算射线与受害者碰撞盒的交点 ==========
        AABB victimBox = victim.getBoundingBox();
        java.util.Optional<Vec3> hitPos = victimBox.clip(eyePos, endPos);

        if (hitPos.isEmpty()) {
            // 射线没有击中碰撞盒（理论上不应该发生，因为伤害已经造成了）
            // 回退到眼睛高度判断
            return getHitPartByAttackerHeight(victim, attacker);
        }

        Vec3 exactHitPos = hitPos.get();

        // ========== 根据精确击中点的 Y 坐标判断部位 ==========
        double hitY = exactHitPos.y;
        double victimFeetY = victim.getY();
        double relativeHeight = hitY - victimFeetY;

        BodyPartRanges ranges = getBodyPartRanges(victim);

        if (relativeHeight >= ranges.headBottom) {
            return LimbType.HEAD;
        } else if (relativeHeight >= ranges.chestBottom) {
            return LimbType.CHEST;
        } else if (relativeHeight >= ranges.legsBottom) {
            return LimbType.LEGS;
        } else {
            return LimbType.FEET;
        }
    }

    /**
     * 获取怪物攻击高度比例
     *
     * @param mob 怪物实体
     * @return 攻击点占怪物高度的比例（0.0 ~ 1.0）
     */
    private static double getMobAttackHeightRatio(Mob mob) {
        String entityId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(mob.getType())
                .toString();

        // 根据怪物类型返回不同的攻击高度比例
        return switch (entityId) {
            // ========== 双足类人怪物 ==========
            case "minecraft:zombie", "minecraft:skeleton", "minecraft:husk",
                 "minecraft:drowned", "minecraft:pillager", "minecraft:vindicator",
                 "minecraft:villager", "minecraft:witch", "minecraft:illusioner",
                 "economy_system:hive_zombie" -> 0.62;  // 模组的蜂巢丧尸

            // ========== 小僵尸 ==========
            case "minecraft:baby_zombie" -> 0.6;

            // ========== 高个子怪物 ==========
            case "minecraft:enderman" -> 0.52;  // 手很长
            case "minecraft:ravager" -> 0.55;  // 劫掠兽

            // ========== 四足动物 ==========
            case "minecraft:wolf", "minecraft:cat", "minecraft:fox" -> 0.42;
            case "minecraft:horse", "minecraft:donkey", "minecraft:mule",
                 "minecraft:skeleton_horse", "minecraft:zombie_horse" -> 0.48;

            // ========== 蜘蛛类 ==========
            case "minecraft:spider", "minecraft:cave_spider" -> 0.55;

            // ========== 滴+ 爬行者 ==========
            case "minecraft:creeper" -> 0.6;
            case "minecraft:slime", "minecraft:magma_cube" -> 0.5;

            // ========== 守卫者 ==========
            case "minecraft:guardian", "minecraft:elder_guardian" -> 0.55;

            // ========== 末影螨/小僵尸 ==========
            case "minecraft:endermite" -> 0.45;
            case "minecraft:silverfish" -> 0.4;

            // ========== 铁傀儡 ==========
            case "minecraft:iron_golem" -> 0.7;

            // ========== 默认 ==========
            default -> 0.55;
        };
    }

    /**
     * 获取玩家当前姿势下的身体部位高度范围
     *
     * @param player 玩家实体
     * @return 身体部位范围
     */
    public static BodyPartRanges getBodyPartRanges(Player player) {
        if (player.isSwimming()) {
            // 游泳时身体是水平的，高度范围压缩
            return new BodyPartRanges(0.24, 0.48, 0.72, 0.96);
        } else if (player.isCrouching()) {
            // 潜行时压低 0.15 格
            return new BodyPartRanges(0.29, 0.60, 0.85, 1.0);
        } else if (player.isFallFlying()) {
            // 滑翔时身体水平
            return new BodyPartRanges(0.2, 0.4, 0.6, 0.8);
        } else {
            // 正常站立
            return new BodyPartRanges(HEAD_BOTTOM, CHEST_BOTTOM, LEGS_BOTTOM, FEET_TOP);
        }
    }

    /**
     * 身体部位高度范围
     */
    public record BodyPartRanges(
            double headBottom,    // 头部下界
            double chestBottom,   // 胸部下界
            double legsBottom,    // 腿部下界
            double feetTop        // 脚部上界
    ) {
        /**
         * 打印调试信息
         */
        public String debugString() {
            return String.format("头[%.2f,∞) 胸[%.2f,%.2f) 腿[%.2f,%.2f) 脚[0,%.2f)",
                    headBottom, chestBottom, headBottom, legsBottom, chestBottom, feetTop, legsBottom);
        }
    }
}
