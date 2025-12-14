package com.mo.economy_system.entity.entities.model.ai;

import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

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
                && zombie.distanceToSqr(zombie.getEncirclePos()) > 1.2;
    }

    @Override
    public void tick() {
        Vec3 pos = zombie.getEncirclePos();
        zombie.getNavigation().moveTo(
                pos.x,
                pos.y,
                pos.z,
                1.15
        );
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        zombie.clearEncirclePos(); // 到位后解除包围，进入攻击
    }
}
