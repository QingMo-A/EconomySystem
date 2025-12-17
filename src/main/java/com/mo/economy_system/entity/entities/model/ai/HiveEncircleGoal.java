package com.mo.economy_system.entity.entities.model.ai;

import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

import static com.mo.economy_system.entity.entities.HiveZombieEntity.MIN_ENCIRCLE_DISTANCE;

public class HiveEncircleGoal extends Goal {

    private final HiveZombieEntity zombie;

    public HiveEncircleGoal(HiveZombieEntity zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return zombie.getTarget() != null
                && zombie.hasEncirclePos()
                && zombie.distanceToSqr(zombie.getEncirclePos()) > MIN_ENCIRCLE_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        // 玩家仍存在+仍有包围点+玩家速度仍慢（避免玩家加速后僵尸还在原地）
        return zombie.getTarget() != null
                && zombie.hasEncirclePos()
                && ((HiveZombieEntity) zombie).isTargetSlow(zombie.getTarget());
    }

    // 优化tick方法，到达位置后减速停留
    @Override
    public void tick() {
        Vec3 currentEncirclePos = zombie.getEncirclePos();
        if (currentEncirclePos == null || zombie.getTarget() == null) return;

        // 玩家速度变化时重新生成包围点
        if (zombie.tickCount % 5 == 0) { // 每5tick检测一次
            zombie.broadcastAggro(zombie.getTarget());
        }

        double distanceToPoint = zombie.distanceToSqr(currentEncirclePos);
        // 到达包围点附近时减速（避免来回晃动）
        double speed = distanceToPoint < 2.0 ? 0.5 : 1.2; // 近点减速50%

        if (!zombie.getNavigation().moveTo(
                currentEncirclePos.x,
                currentEncirclePos.y,
                currentEncirclePos.z,
                speed
        )) {
            // 导航失败时缓慢移动到目标点
            Vec3 dir = currentEncirclePos.subtract(zombie.position()).normalize().scale(0.05);
            zombie.setDeltaMovement(dir.x, zombie.getDeltaMovement().y, dir.z);
        }
    }

    @Override
    public void stop() {
        zombie.clearEncirclePos(); // 到位后解除包围，进入攻击
    }
}
