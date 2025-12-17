package com.mo.economy_system.entity.entities.model.ai;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class HiveInterceptGoal extends Goal {
    private final HiveZombieEntity zombie;
    private LivingEntity target; // 目标玩家
    private Vec3 lastTargetPos; // 上一时刻玩家位置（用于计算移动向量）
    private final double minDistance = 1.5D; // 离玩家过近时不拦截（直接近战）
    private final double mediumDistance = 3.5D;
    //预测时间
    private static final double PREDICTION_TIME = 0.53;
    //最大预测距离
    private static final double MAX_PREDICT_DISTANCE = 8.0;

    public HiveInterceptGoal(HiveZombieEntity zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    //计算拦截点
    private Vec3 calculateInterceptPoint(Vec3 targetPos, Vec3 targetMotion) {
        LivingEntity currentTarget = zombie.getTarget();
        if (currentTarget == null) {
            return targetPos;
        }
        //玩家静止状态，移动向量小
        if (targetMotion.lengthSqr() < 0.001) {
            //默认向前方1格
            return targetPos.add(Vec3.directionFromRotation(zombie.getTarget().getRotationVector()).scale(1.0));
        }

        //标准化玩家移动方向，然后计算预测移动距离
        Vec3 moveDirection = targetMotion.normalize(); // 移动方向（单位向量）
        double targetSpeed = targetMotion.length() * 20.0; // 玩家实际速度（格/秒，20tick=1秒）

        //基于预测时间计算玩家前方位置，未来多少秒会到达的点
        double predictDistance = targetSpeed * PREDICTION_TIME;
        //限制最大预测距离，避免过远
        predictDistance = Math.min(predictDistance, MAX_PREDICT_DISTANCE);

        //计算最终拦截点——玩家当前位置 + 前方预测距离
        Vec3 interceptPos = targetPos.add(moveDirection.scale(predictDistance));

        //确保拦截点在地面上，避免空中拦截
        BlockPos predictedBlock = BlockPos.containing(interceptPos);
        if (zombie.level().getBlockState(predictedBlock.below()).isAir()) {
            double groundY = zombie.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, predictedBlock).getY();
            interceptPos = new Vec3(interceptPos.x, groundY, interceptPos.z);
        }

        return interceptPos;
    }

    @Override
    public boolean canUse() {
        this.target = zombie.getTarget();
        // 目标存在且距离足够远
        if (target == null || zombie.distanceTo(target) < mediumDistance) {
            return false;
        }
        double distance = zombie.distanceTo(target);
        //初始化lastTargetPos，避免后续null
        if (lastTargetPos == null) {
            lastTargetPos = target.position();
        }

        //仅当距离大于最小距离时激活拦截
        return distance > minDistance;
    }

    @Override
    public boolean canContinueToUse() {
        //持续条件为目标存在 + 无包围点 + 仍在拦截范围内
        if (target != null && zombie.distanceTo(target) > mediumDistance)
        {
            System.out.println("可以追踪！！！！！");
        }
        return target != null && zombie.distanceTo(target) > mediumDistance;
    }

    @Override
    public void tick() {
        if (target == null) return;

        Vec3 currentTargetPos = target.position();
        Vec3 targetMotion = currentTargetPos.subtract(lastTargetPos);
        lastTargetPos = currentTargetPos;

        Vec3 interceptPos = calculateInterceptPoint(currentTargetPos, targetMotion);
        System.out.println("计算出的拦截点：" + interceptPos);

        double fixedSpeed;
        if (zombie.level().isDay() && zombie.level().canSeeSky(zombie.blockPosition()) && !zombie.level().isRaining()) {
            fixedSpeed = 0.23 * 0.8; //白天
        } else {
            fixedSpeed = 0.23 * 2; //夜间固定速度
        }

        zombie.getNavigation().moveTo(interceptPos.x, interceptPos.y, interceptPos.z, fixedSpeed);
        //无视导航，直接移动
        Vec3 dir = interceptPos.subtract(zombie.position()).normalize();
        zombie.setDeltaMovement(
                dir.x * fixedSpeed * 0.8,
                zombie.onGround() ? 0.0 : zombie.getDeltaMovement().y, // 地面移动，避免浮空
                dir.z * fixedSpeed * 0.8
        );
        //强制看向目标（视觉+移动方向对齐）
        zombie.lookAt(target, 30.0F, 30.0F);
    }

    @Override
    public void stop() {
        lastTargetPos = null;
    }
}