package com.mo.economy_system.entity.entities;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.entity.entities.model.ai.HiveZombieTargetGoal;
import com.mo.economy_system.sound.EconomySystem_Sounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HiveZombieEntity extends Monster {
    // 定义变种的数据访问器
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(HiveZombieEntity.class, EntityDataSerializers.INT);

    private static boolean isPlayingSound = false;

    /* =========================
       Attribute Modifier UUIDs | 属性关键字UUID
       ========================= */

    private static final UUID DAY_SPEED_MODIFIER =
            UUID.fromString("11111111-aaaa-bbbb-cccc-000000000001");
    private static final UUID DAY_DAMAGE_MODIFIER =
            UUID.fromString("11111111-aaaa-bbbb-cccc-000000000002");

    private static final UUID NIGHT_SPEED_MODIFIER =
            UUID.fromString("22222222-aaaa-bbbb-cccc-000000000001");
    private static final UUID NIGHT_DAMAGE_MODIFIER =
            UUID.fromString("22222222-aaaa-bbbb-cccc-000000000002");
    private static final UUID NIGHT_RANGE_MODIFIER =
            UUID.fromString("22222222-aaaa-bbbb-cccc-000000000003");

    private final WaterBoundPathNavigation waterNavigation;
    private final GroundPathNavigation groundNavigation;


    public HiveZombieEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setMaxUpStep(1.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.waterNavigation = new WaterBoundPathNavigation(this, level);
        this.groundNavigation = new GroundPathNavigation(this, level);
    }

    /*  =========================
        Encircle Data | 包围数据
        ========================= */

    private net.minecraft.world.phys.Vec3 encirclePos = null;

    public void setEncirclePos(net.minecraft.world.phys.Vec3 pos) {
        this.encirclePos = pos;
    }

    public net.minecraft.world.phys.Vec3 getEncirclePos() {
        return encirclePos;
    }

    public boolean hasEncirclePos() {
        return encirclePos != null;
    }

    public void clearEncirclePos() {
        this.encirclePos = null;
    }


    /* =========================
       AI | AI行为
       ========================= */

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // 游泳

        // 嗜血追击：持续压迫目�?        this.goalSelector.addGoal(1, new HiveBloodlustGoal(this, 1.45D));

        // 逼近目标（中距离维持压迫感）
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal(this, 1.25D, 32.0F));

        // 受伤反击
        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));

        // 包围移动（优先于近战�?
        this.goalSelector.addGoal(2, new com.mo.economy_system.entity.entities.model.ai.HiveEncircleGoal(this));

        // 近战
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.3D, false));

        // 随机移动
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.9D));

        // 看向玩家
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 8.0F));

        // 随机环顾
        this.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        // 丧尸基础AI
        this.targetSelector.addGoal(1, new HiveZombieTargetGoal(this));
    }

    @Override
    public void travel(Vec3 p_32394_) {
        if (this.isControlledByLocalInstance() && this.isInWater() && this.economySystem$wantsToSwim()) {
            this.moveRelative(0.01F, p_32394_);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(p_32394_);
        }
    }

    @Override
    public void updateSwimming() {
        if (!this.level().isClientSide) {
            if (this.isEffectiveAi() && this.isInWater() && this.economySystem$wantsToSwim()) {
                this.navigation = this.waterNavigation;
                this.setSwimming(true);
            } else {
                this.navigation = this.groundNavigation;
                this.setSwimming(false);
            }
        }
    }

    private boolean economySystem$wantsToSwim() {
        if (this.isInWater()) {
            return true;
        }
        LivingEntity target = this.getTarget();
        return target != null && target.isInWater();
    }

    private static class HiveBloodlustGoal extends Goal {
        private final HiveZombieEntity zombie;
        private final double speed;
        private int repathTick = 0;

        private HiveBloodlustGoal(HiveZombieEntity zombie, double speed) {
            this.zombie = zombie;
            this.speed = speed;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.zombie.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.zombie.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.zombie.setAggressive(true);
            this.repathTick = 0;
        }

        @Override
        public void stop() {
            this.zombie.setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity target = this.zombie.getTarget();
            if (target == null) {
                return;
            }

            this.zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.repathTick-- <= 0 || this.zombie.getNavigation().isDone()) {
                this.repathTick = 10;
                this.zombie.getNavigation().moveTo(target, this.speed);
            }
        }
    }


    // 创建属性构建器
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)  // 生命�?
                .add(Attributes.MOVEMENT_SPEED, 2.0F)  // 移动速度
                .add(Attributes.ATTACK_DAMAGE, 4.0D)  // 攻击伤害
                .add(Attributes.ARMOR, 2.0D)  // 护甲
                .add(Attributes.FOLLOW_RANGE, 35.0D);  // 跟随范围
    }

    /* =========================
       Tick | 每Tick操作
       ========================= */

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            updateDayNightEffects();
        }
    }

    /* =========================
       Day / Night Logic | 昼夜逻辑
       ========================= */

    /**
     * 更新实体在白天和夜晚环境下的属性效�?
     * <p> 该方法根据当前光照条件动态调整实体的移动速度、攻击伤害和跟随范围属性：
     * <p> 在阳光直射下（白天）：削弱移动速度和攻击伤�?
     * <p> 在夜间：增强移动速度、攻击伤害和视野范围
     * <p> 在其他条件下：移除所有特殊修饰符，恢复默认状�?
     * <p> 方法会先清除之前添加的所有日夜修饰符，然后根据当前环境重新应用相应的修饰�?
     */
    private void updateDayNightEffects() {
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        var range = this.getAttribute(Attributes.FOLLOW_RANGE);

        if (speed == null || damage == null || range == null) return;

        // 清理所有已存在的日夜属性修饰符
        speed.removeModifier(DAY_SPEED_MODIFIER);
        speed.removeModifier(NIGHT_SPEED_MODIFIER);
        damage.removeModifier(DAY_DAMAGE_MODIFIER);
        damage.removeModifier(NIGHT_DAMAGE_MODIFIER);
        range.removeModifier(NIGHT_RANGE_MODIFIER);

        if (isInDirectSunlight()) {
            // 白天削弱（不燃烧�?
            speed.addTransientModifier(new AttributeModifier(
                    DAY_SPEED_MODIFIER,
                    "Day slow",
                    -0.30,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));

            damage.addTransientModifier(new AttributeModifier(
                    DAY_DAMAGE_MODIFIER,
                    "Day weak",
                    -0.40,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));

        } else if (!this.level().isDay()) {
            // 夜晚强化
            speed.addTransientModifier(new AttributeModifier(
                    NIGHT_SPEED_MODIFIER,
                    "Night speed",
                    0.75,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));

            damage.addTransientModifier(new AttributeModifier(
                    NIGHT_DAMAGE_MODIFIER,
                    "Night damage",
                    0.70,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));

            range.addTransientModifier(new AttributeModifier(
                    NIGHT_RANGE_MODIFIER,
                    "Night vision",
                    20.0,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    /* =========================
       Sunlight Detection | 阳光检�?
       ========================= */

    private boolean isInDirectSunlight() {
        if (!this.level().isDay()) return false;
        if (this.level().isRaining()) return false;

        BlockPos pos = this.blockPosition();
        return this.level().canSeeSky(pos);
    }

    /* =========================
       Disable Burning | 禁用阳光下燃�?
       ========================= */

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    /* =========================
       Hive Broadcast | 蜂巢效应
       ========================= */

    double swarmRange = 25.0;

    public void broadcastAggro(LivingEntity target) {
        if (this.level().isClientSide) return;
        if (this.level().isDay()) return;

        // 播放蜂巢呼叫音效（只播放一次）
        /*if (!hasPlayedCallSound) {
            this.playHiveCallSound();
            hasPlayedCallSound = true;

            // 3秒后重置，可以再次播�?
            this.level().getServer().execute(() -> {
                try {
                    Thread.sleep(3000); // 3�?
                    hasPlayedCallSound = false;
                } catch (InterruptedException e) {
                    // 忽略中断
                }
            });
        }*/

        AABB box = this.getBoundingBox().inflate(swarmRange);

        List<HiveZombieEntity> zombies =
                this.level().getEntitiesOfClass(HiveZombieEntity.class, box);

        int count = zombies.size();
        if (count <= 1) return;

        double radius = 2.8; // 包围半径
        var center = target.position();

        for (int i = 0; i < count; i++) {
            HiveZombieEntity zombie = zombies.get(i);

            zombie.setTarget(target);

            // 分配角度（形成包围）
            double angle = (2 * Math.PI / count) * i;

            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            zombie.setEncirclePos(
                    new net.minecraft.world.phys.Vec3(x, center.y, z)
            );
        }
    }

    /* =========================
       Sound System | 简化的音效系统
       ========================= */

    @Override
    protected SoundEvent getAmbientSound() {
        return EconomySystem_Sounds.HIVE_ZOMBIE_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 80;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return EconomySystem_Sounds.HIVE_ZOMBIE_DEATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_33034_) {
        return EconomySystem_Sounds.HIVE_ZOMBIE_HURT.get();
    }

    // 注册实体属�?
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EconomySystem_Entities.HIVE_ZOMBIE.get(), HiveZombieEntity.createAttributes().build());
    }

    // 变种类型枚举
    public enum Variant {
        NORMAL(0, true, true, true, true),       // 完整丧尸
        NO_HEAD(1, false, true, true, true),     // 无头丧尸
        NO_LEFT_ARM(2, true, false, true, true), // 无左�?
        NO_RIGHT_ARM(3, true, true, false, true), // 无右�?
        NO_ARMS(4, true, false, false, true),    // 无双�?
        NO_LEGS(5, true, true, true, false),     // 无腿（拖着走）
        CRAWLER(6, true, true, true, true),      // 爬行者（特殊动画�?
        BLOODY(7, true, true, true, true),       // 血腥版�?
        ROTTEN(8, true, true, true, true);       // 腐烂严重版本

        private final int id;
        private final boolean hasHead;
        private final boolean hasLeftArm;
        private final boolean hasRightArm;
        private final boolean hasLegs;

        Variant(int id, boolean hasHead, boolean hasLeftArm, boolean hasRightArm, boolean hasLegs) {
            this.id = id;
            this.hasHead = hasHead;
            this.hasLeftArm = hasLeftArm;
            this.hasRightArm = hasRightArm;
            this.hasLegs = hasLegs;
        }

        public int getId() { return id; }
        public boolean hasHead() { return hasHead; }
        public boolean hasLeftArm() { return hasLeftArm; }
        public boolean hasRightArm() { return hasRightArm; }
        public boolean hasLegs() { return hasLegs; }

        public static Variant byId(int id) {
            for (Variant variant : values()) {
                if (variant.id == id) return variant;
            }
            return NORMAL;
        }
    }
}
