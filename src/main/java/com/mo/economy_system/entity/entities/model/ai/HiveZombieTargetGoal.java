package com.mo.economy_system.entity.entities.model.ai;

import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class HiveZombieTargetGoal  extends NearestAttackableTargetGoal<Player> {
    private final HiveZombieEntity zombie;
    private boolean broadcasted = false;

    public HiveZombieTargetGoal(HiveZombieEntity zombie) {
        super(zombie, Player.class, true);
        this.zombie = zombie;
    }

    @Override
    public void start() {
        super.start();

        if (!broadcasted && this.target != null) {
            broadcasted = true;
            zombie.broadcastAggro(this.target);
        }
    }

    @Override
    public void stop() {
        super.stop();
        broadcasted = false;
    }
}
