package com.mo.economy_system.entity.entities;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.entity.entities.model.ai.HiveEncircleGoal;
import com.mo.economy_system.entity.entities.model.ai.HiveInterceptGoal;
import com.mo.economy_system.entity.entities.model.ai.HiveZombieTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HiveZombieEntity extends Monster {
    // 定义变种的数据访问器
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(HiveZombieEntity.class, EntityDataSerializers.INT);

    /* =========================
       Attribute Modifier UUIDs
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


    public HiveZombieEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    // =========================
// Encircle Data
// =========================

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
       AI
       ========================= */

    //注册ai行为
    @Override
    protected void registerGoals() {
        super.registerGoals();

        // 包围移动（优先于近战）
        this.goalSelector.addGoal(3, new HiveEncircleGoal(this));
        //拦截
        this.goalSelector.addGoal(2, new HiveInterceptGoal(this));

        // 近战
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.5D, false){
            @Override
            protected int getAttackInterval() {
                return 15; //0.75s就攻击一次
            }
        });

        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new HiveZombieTargetGoal(this));
    }


    // 创建属性构建器
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)  // 生命值
                .add(Attributes.MOVEMENT_SPEED, 0.23D)  // 移动速度
                .add(Attributes.ATTACK_DAMAGE, 3.0D)  // 攻击伤害
                .add(Attributes.ARMOR, 2.0D)  // 护甲
                .add(Attributes.FOLLOW_RANGE, 35.0D);  // 跟随范围
    }

    /* =========================
       Tick
       ========================= */
    private Vec3 lastTargetPos = null;
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateDayNightEffects();
            LivingEntity target = getTarget();
            if (target != null) {
                //当丧尸进入发现范围时
                double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
                if (this.distanceTo(target) <= followRange) {
                    this.clearEncirclePos(); //进入范围后清除包围点，让拦截系统生效
                }
                //向其他不在发现范围内的丧尸广播
                double motionSpeed = target.getDeltaMovement().lengthSqr();
                int updateInterval = motionSpeed > 0.1 ? 5 : (motionSpeed > 0.01 ? 10 : 20);
                if (tickCount % updateInterval == 0) {
                    broadcastAggro(target);
                }
            }
        }
    }

    /* =========================
       Day / Night Logic
       ========================= */

    private void updateDayNightEffects() {
        var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        var range = this.getAttribute(Attributes.FOLLOW_RANGE);

        if (speed == null || damage == null || range == null) return;

        damage.removeModifier(DAY_DAMAGE_MODIFIER);
        damage.removeModifier(NIGHT_DAMAGE_MODIFIER);
        range.removeModifier(NIGHT_RANGE_MODIFIER);

        if (isInDirectSunlight()) {
            damage.addTransientModifier(new AttributeModifier(
                    DAY_DAMAGE_MODIFIER,
                    "Day weak",
                    -0.40,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!this.level().isDay()) {
            damage.addTransientModifier(new AttributeModifier(
                    NIGHT_DAMAGE_MODIFIER,
                    "Night damage",
                    0.50,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            range.addTransientModifier(new AttributeModifier(
                    NIGHT_RANGE_MODIFIER,
                    "Night vision",
                    20.0,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    /* =========================
       Sunlight Detection
       ========================= */

    private boolean isInDirectSunlight() {
        if (!this.level().isDay()) return false;
        if (this.level().isRaining()) return false;

        BlockPos pos = this.blockPosition();
        return this.level().canSeeSky(pos);
    }

    /* =========================
       Disable Burning
       ========================= */

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }


    /* =========================
       Hive Broadcast
       ========================= */
    private List<Vec3> targetPositionHistory = new ArrayList<>(); // 记录玩家最近位置
    private static final int HISTORY_SIZE = 5; // 保留5个历史位置
    private static final float PREDICTION_TIME = 0.8F; // 预测0.8秒后的位置（可调整）

    private void updateTargetHistory(LivingEntity target) {
        Vec3 currentPos = target.position();
        targetPositionHistory.add(currentPos);
        if (targetPositionHistory.size() > HISTORY_SIZE) {
            targetPositionHistory.remove(0); // 保持固定长度
        }
    }

    private Vec3 predictTargetPosition(LivingEntity target) {
        if (targetPositionHistory.size() < 2) {
            return target.position();
        }

        // 计算移动向量（最后两个位置的差）
        Vec3 lastPos = targetPositionHistory.get(targetPositionHistory.size() - 2);
        Vec3 currentPos = targetPositionHistory.get(targetPositionHistory.size() - 1);
        Vec3 motion = currentPos.subtract(lastPos);

        // 计算预测位置（当前位置 + 移动向量 * 预测时间（秒）* 20（tick/秒））
        // 先计算标量系数（确保是double类型）
        double scalar = PREDICTION_TIME * 20.0D;
        // 对motion进行缩放
        Vec3 scaledMotion = motion.multiply(scalar, scalar, scalar);
        // 计算预测位置
        Vec3 predicted = currentPos.add(scaledMotion);

        // 确保预测位置在地面上
        BlockPos predictedBlock = BlockPos.containing(predicted);
        if (level().getBlockState(predictedBlock.below()).isAir()) {
            predicted = new Vec3(predicted.x, level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, predictedBlock).getY(), predicted.z);
        }

        return predicted;
    }

    double swarmRange = 30.0;
    private static final double ENCIRCLE_RADIUS = 2.5D; // 包围半径（玩家周围2.5格）
    public static final double MIN_ENCIRCLE_DISTANCE = 1.2D; // 到达包围点的判定距离
    private int encircleIndex = -1; // 该丧尸在包围圈中的索引（用于分配角度）

    // 新增getter/setter
    public int getEncircleIndex() { return encircleIndex; }
    public void setEncircleIndex(int index) { this.encircleIndex = index; }

    // 重置包围状态（用于目标丢失时）
    public void resetEncircleState() {
        clearEncirclePos();
        setEncircleIndex(-1);
    }

    // 玩家速度阈值（小于阈值则视为“慢速”，单位：格/tick，1格/tick ≈ 20格/秒）
    private static final double SLOW_SPEED_THRESHOLD = 0.09;
    // 慢速时的包围半径（更大，确保分散）
    private static final double SLOW_SPEED_ENCIRCLE_RADIUS = 4.0;
    // 快速时的包围半径（更小，避免脱节）
    private static final double FAST_SPEED_ENCIRCLE_RADIUS = 2.0;

    // 判断玩家是否处于慢速状态
    public boolean isTargetSlow(LivingEntity target) {
        if (target == null) return false;
        Vec3 motion = target.getDeltaMovement();
        // 计算水平方向移动速度（忽略Y轴上下移动）
        double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        return horizontalSpeed < SLOW_SPEED_THRESHOLD;
    }

    // 为周围僵尸生成分散的包围点（环形分布）
    private List<Vec3> generateEncirclePoints(LivingEntity target, int zombieCount) {
        List<Vec3> points = new ArrayList<>();
        if (target == null || zombieCount <= 0) return points;

        // 根据玩家速度选择包围半径
        double radius = isTargetSlow(target) ? SLOW_SPEED_ENCIRCLE_RADIUS : FAST_SPEED_ENCIRCLE_RADIUS;
        Vec3 targetPos = target.position();

        // 计算地面Y坐标（避免包围点在半空）
        BlockPos targetBlock = BlockPos.containing(targetPos);
        double groundY = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, targetBlock).getY();

        // 环形均匀N个均匀分布的点（N=参与包围的僵尸数量）
        for (int i = 0; i < zombieCount; i++) {
            // 角度均匀分配（0~360度）
            double angle = (2 * Math.PI / zombieCount) * i;
            // 计算环形上的坐标
            double x = targetPos.x + radius * Math.cos(angle);
            double z = targetPos.z + radius * Math.sin(angle);
            points.add(new Vec3(x, groundY, z));
        }
        return points;
    }

    public void broadcastAggro(LivingEntity target) {
        if (target == null || level().isClientSide || level().isDay()) {
            return;
        }

        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        double broadcastRange = followRange + swarmRange;
        AABB searchBox = AABB.ofSize(target.position(), broadcastRange * 2, broadcastRange * 2, broadcastRange * 2);
        List<HiveZombieEntity> distantZombies = level().getEntitiesOfClass(HiveZombieEntity.class, searchBox);

        // 筛选符合条件的僵尸（非自身+超出跟随范围）
        List<HiveZombieEntity> validZombies = new ArrayList<>();
        for (HiveZombieEntity zombie : distantZombies) {
            if (zombie != this && zombie.distanceTo(target) > followRange) {
                validZombies.add(zombie);
            }
        }

        // 如果玩家速度慢，生成分散包围点并分配给僵尸
        if (isTargetSlow(target) && !validZombies.isEmpty()) {
            List<Vec3> encirclePoints = generateEncirclePoints(target, validZombies.size());
            for (int i = 0; i < validZombies.size(); i++) {
                HiveZombieEntity zombie = validZombies.get(i);
                Vec3 point = encirclePoints.get(i % encirclePoints.size()); // 循环分配点
                zombie.setEncirclePos(point); // 设置该僵尸的包围点
                zombie.setTarget(target);
                zombie.getNavigation().moveTo(point.x, point.y, point.z, zombie.getSpeed());
            }
        } else {
            // 玩家速度快时，保持原逻辑（引导至目标附近，不强制分散）
            for (HiveZombieEntity zombie : validZombies) {
                zombie.clearEncirclePos();
                zombie.setTarget(target);
                Vec3 guidePos = new Vec3(target.position().x,
                        level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(target.position())).getY(),
                        target.position().z);
                zombie.getNavigation().moveTo(guidePos.x, guidePos.y, guidePos.z, zombie.getSpeed());
            }
        }
    }

    // 计算单个丧尸的包围点（基于圆周均匀分布）
    public Vec3 calculateEncirclePoint(LivingEntity target, int index, int total) {
        if (total == 0) {
            return target.position(); // 只有1只时直接靠近目标
        }

        //计算角度（圆周均匀分配）
        double angle = (index * 2 * Math.PI) / total; // 每个丧尸间隔相同角度
        //随机微调角度，避免绝对对称导致的轻微扎堆
        angle += level().getRandom().nextDouble() * 0.3 - 0.15;

        //基于角度计算相对玩家的偏移量
        double offsetX = Math.cos(angle) * ENCIRCLE_RADIUS;
        double offsetZ = Math.sin(angle) * ENCIRCLE_RADIUS;

        //计算最终包围点（基于玩家位置偏移）
        Vec3 targetPos = target.position();
        Vec3 encirclePos = new Vec3(
                targetPos.x + offsetX,
                targetPos.y,
                targetPos.z + offsetZ
        );

        //确保包围点在地面上
        BlockPos pos = BlockPos.containing(encirclePos);
        double groundY = level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY();
        return new Vec3(encirclePos.x, groundY, encirclePos.z);
    }


    // 注册实体属性
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier supplier = HiveZombieEntity.createAttributes().build();
        EconomySystem.LOGGER.info("属性注册：移动速度=" + supplier.getValue(Attributes.MOVEMENT_SPEED));
        event.put(EconomySystem_Entities.HIVE_ZOMBIE.get(), supplier);
    }

    // 变种类型枚举
    public enum Variant {
        NORMAL(0, true, true, true, true),       // 完整丧尸
        NO_HEAD(1, false, true, true, true),     // 无头丧尸
        NO_LEFT_ARM(2, true, false, true, true), // 无左臂
        NO_RIGHT_ARM(3, true, true, false, true), // 无右臂
        NO_ARMS(4, true, false, false, true),    // 无双臂
        NO_LEGS(5, true, true, true, false),     // 无腿（拖着走）
        CRAWLER(6, true, true, true, true),      // 爬行者（特殊动画）
        BLOODY(7, true, true, true, true),       // 血腥版本
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
