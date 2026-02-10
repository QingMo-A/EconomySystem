package com.mo.economy_system.entity.entities.model.ai;

import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

public class HiveZombieTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final HiveZombieEntity zombie;
    private final Predicate<LivingEntity> selector;
    private int retargetTick = 0;

    public HiveZombieTargetGoal(HiveZombieEntity zombie) {
        super(zombie, LivingEntity.class, 10, false, false,
            target -> target != null && target != zombie && !(target instanceof HiveZombieEntity));
        this.zombie = zombie;
        this.selector = target -> target != null && target != zombie && !(target instanceof HiveZombieEntity);
    }

    @Override
    public void start() {
        super.start();
        if (this.target != null) {
            zombie.broadcastAggro(this.target);
        }
    }

    @Override
    public void stop() {
        super.stop();
        retargetTick = 0;
    }

    @Override
    public boolean canUse() {
        if (++retargetTick < 10) {
            return super.canUse();
        }
        retargetTick = 0;
        LivingEntity nearest = findNearestTarget();
        if (nearest != null) {
            this.target = nearest;
            this.mob.setTarget(nearest);
            zombie.broadcastAggro(nearest);
            return true;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) {
            return false;
        }
        return true;
    }

    private LivingEntity findNearestTarget() {
        double range = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB box = this.mob.getBoundingBox().inflate(range, 4.0D, range);
        TargetingConditions conditions = TargetingConditions.forCombat()
            .range(range)
            .selector(this.selector);
        return this.mob.level().getNearestEntity(LivingEntity.class, conditions, this.mob,
            this.mob.getX(), this.mob.getEyeY(), this.mob.getZ(), box);
    }
}
