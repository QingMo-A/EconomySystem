package com.mo.economy_system.core.playerattributes_system.courage;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.entity.EconomySystem_Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerCourageManager {
    private static final int PLAYER_DISTANCE = 30; //一定距离内是否有玩家
    private static final int COURAGE_REDUCE_TICK_INTERVAL = 20; //多少tick一次检测
    private static final int COURAGE_REDUCE_INTERVAL = 200; //多少tick一次扣除（夜晚无人）
    private static final int COURAGE_REDUCE_AMOUNT = 1; //夜晚无人勇气值一次降低多少
    private static final int NORMAL_COURAGE = 50; // 常态恢复到的勇气值
    private static final int MAX_PLAYER_COURAGE = 70; // 附近有多名玩家时恢复到的勇气值
    private static final int PLAYER_COUNT_THRESHOLD = 4; // 大于等于该数量的玩家才会添加常态勇气值
    private static final int COURAGE_CHANGE_INTERVAL = 100; //多少tick一次恢复/扣除
    private static final int COURAGE_CHANGE_AMOUNT = 2; //每次恢复/扣除的勇气值点数（白天）
    private static final int NIGHT_WITH_PLAYER_COURAGE_CHANGE_AMOUNT = 1; //夜晚有玩家时每次恢复点数

    private static final long KILL_TIME_WINDOW = 10000L; //击杀时间窗口（10秒，毫秒级）
    private static final int KILL_THRESHOLD = 5; //时间窗口内需要击杀的敌对生物数量
    private static final int COURAGE_ADD_AMOUNT = 10; //满足条件后增加的勇气值

    private static int CLIENT_CURRENT_COURAGE = 50; //客户端默认值
    private static int CLIENT_MAX_COURAGE = 100; // 客户端默认值
    //存储击杀记录
    private static final Map<UUID, List<Long>> PLAYER_KILL_RECORDS = new ConcurrentHashMap<>();

    //敌对生物集合
    private static final Set<EntityType<?>> HOSTILE_ENTITY_TYPES = new HashSet<>();
    //被排除的敌对生物
    private static final Set<EntityType<?>> EXCLUDED_HOSTILE_ENTITY = new HashSet<>();
    //初始化敌对生物集合
    static {
        HOSTILE_ENTITY_TYPES.add(EntityType.BLAZE);
        HOSTILE_ENTITY_TYPES.add(EntityType.CAVE_SPIDER);
        HOSTILE_ENTITY_TYPES.add(EntityType.CREEPER);
        HOSTILE_ENTITY_TYPES.add(EntityType.DROWNED);
        HOSTILE_ENTITY_TYPES.add(EntityType.ELDER_GUARDIAN);
        HOSTILE_ENTITY_TYPES.add(EntityType.ENDERMAN);
        HOSTILE_ENTITY_TYPES.add(EntityType.ENDERMITE);
        HOSTILE_ENTITY_TYPES.add(EntityType.EVOKER);
        HOSTILE_ENTITY_TYPES.add(EntityType.GHAST);
        HOSTILE_ENTITY_TYPES.add(EntityType.GIANT);
        HOSTILE_ENTITY_TYPES.add(EntityType.GUARDIAN);
        HOSTILE_ENTITY_TYPES.add(EntityType.HOGLIN);
        HOSTILE_ENTITY_TYPES.add(EntityType.HUSK);
        HOSTILE_ENTITY_TYPES.add(EntityType.ILLUSIONER);
        HOSTILE_ENTITY_TYPES.add(EntityType.MAGMA_CUBE);
        HOSTILE_ENTITY_TYPES.add(EntityType.PHANTOM);
        HOSTILE_ENTITY_TYPES.add(EntityType.PIGLIN);
        HOSTILE_ENTITY_TYPES.add(EntityType.PIGLIN_BRUTE);
        HOSTILE_ENTITY_TYPES.add(EntityType.PILLAGER);
        HOSTILE_ENTITY_TYPES.add(EntityType.RAVAGER);
        HOSTILE_ENTITY_TYPES.add(EntityType.SHULKER);
        HOSTILE_ENTITY_TYPES.add(EntityType.SILVERFISH);
        HOSTILE_ENTITY_TYPES.add(EntityType.SKELETON);
        HOSTILE_ENTITY_TYPES.add(EntityType.SLIME);
        HOSTILE_ENTITY_TYPES.add(EntityType.SPIDER);
        HOSTILE_ENTITY_TYPES.add(EntityType.STRAY);
        HOSTILE_ENTITY_TYPES.add(EntityType.VEX);
        HOSTILE_ENTITY_TYPES.add(EntityType.VINDICATOR);
        HOSTILE_ENTITY_TYPES.add(EntityType.WARDEN);
        HOSTILE_ENTITY_TYPES.add(EntityType.WITCH);
        HOSTILE_ENTITY_TYPES.add(EntityType.WITHER);
        HOSTILE_ENTITY_TYPES.add(EntityType.WITHER_SKELETON);
        HOSTILE_ENTITY_TYPES.add(EntityType.ZOGLIN);
        HOSTILE_ENTITY_TYPES.add(EntityType.ZOMBIE);
        HOSTILE_ENTITY_TYPES.add(EntityType.ZOMBIE_VILLAGER);
        HOSTILE_ENTITY_TYPES.add(EntityType.ZOMBIFIED_PIGLIN);
    }

    //延迟注册丧尸
    @Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class EntityRegisterHandler {
        @SubscribeEvent
        public static void registerEntityAttributes(FMLCommonSetupEvent event) {
            EconomySystem.LOGGER.info("延迟注册丧尸");
            HOSTILE_ENTITY_TYPES.add(EconomySystem_Entities.HIVE_ZOMBIE.get());
        }
    }

    @SubscribeEvent
    //判断玩家状态，执行勇气值变更（夜晚无人扣除）与buff逻辑
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || !event.player.isAlive() || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        UUID playerUUID = serverPlayer.getUUID();
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
        if (attributesData == null) {
            return; // 空值防护，避免空指针
        }

        if (serverPlayer.tickCount % COURAGE_REDUCE_TICK_INTERVAL != 0) {
            return;
        }

        boolean isNight = isNightTime(serverPlayer);
        boolean hasNoPlayerAround = !hasPlayerAround(serverPlayer);
        int nearbyPlayerCount = getNearbyPlayerCount(serverPlayer);
        int maxCourage = attributesData.getMaxCourage();
        int currentCourage = attributesData.getCurrentCourage();
        // 动态获取目标勇气值（根据附近玩家数量）
        int targetCourage = nearbyPlayerCount >= PLAYER_COUNT_THRESHOLD ? MAX_PLAYER_COURAGE : NORMAL_COURAGE;
        // 目标值不超过最大勇气值
        targetCourage = Math.min(targetCourage, maxCourage);
        targetCourage = Math.max(targetCourage, 0);

        //buff判断
        if (maxCourage <= 0) {
            return;
        }
        float courageRatio = (float) attributesData.getCurrentCourage() / (float) maxCourage;

        // 勇气值低于20%：施加虚弱I + 缓慢I
        if (courageRatio < 0.2F) {
            MobEffectInstance weaknessEffect = new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true);
            MobEffectInstance slownessEffect = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true);
            serverPlayer.addEffect(weaknessEffect);
            serverPlayer.addEffect(slownessEffect);
//            EconomySystem.LOGGER.debug("玩家 {} 勇气值不足20%，施加虚弱I和缓慢I效果", serverPlayer.getScoreboardName());
        }
        // 勇气值≥70%：施加力量I
        else if (courageRatio >= 0.7F) {
            MobEffectInstance strengthEffect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, true);
            serverPlayer.addEffect(strengthEffect);
            EconomySystem.LOGGER.debug("玩家 {} 勇气值≥70%，施加力量I效果", serverPlayer.getScoreboardName());
        }

        // 主逻辑
        //处理夜晚场景
        if (isNight) {
            // 夜晚无人：10秒一次扣除勇气值(扣完为止）
            if (hasNoPlayerAround) {
                if (serverPlayer.tickCount % COURAGE_REDUCE_INTERVAL != 0) {
                    return;
                }
                changeCourageValue(serverPlayer, attributesData, -COURAGE_REDUCE_AMOUNT, 0, maxCourage);
            }
            // 夜晚有人：5秒一次缓慢恢复到70
            else {
                // 达到变更间隔才执行
                if (serverPlayer.tickCount % COURAGE_CHANGE_INTERVAL != 0) {
                    return;
                }
                // 当前勇气值 < 目标值：恢复1点，不超过目标值（70）
                if (currentCourage < targetCourage) {
                    changeCourageValue(serverPlayer, attributesData, NIGHT_WITH_PLAYER_COURAGE_CHANGE_AMOUNT, targetCourage, maxCourage);
                } else if (currentCourage > targetCourage) {
                     changeCourageValue(serverPlayer, attributesData, -NIGHT_WITH_PLAYER_COURAGE_CHANGE_AMOUNT, targetCourage, maxCourage);
                }
            }
        }
        //白天
        else {
            // 达到5秒一次的变更间隔才执行
            if (serverPlayer.tickCount % COURAGE_CHANGE_INTERVAL != 0) {
                return;
            }
            // 当前勇气值 < 目标值：恢复2点，不超过目标值
            if (currentCourage < targetCourage) {
                changeCourageValue(serverPlayer, attributesData, COURAGE_CHANGE_AMOUNT, targetCourage, maxCourage);
            }
            // 当前勇气值 > 目标值：扣除2点，不低于目标值
            else if (currentCourage > targetCourage) {
                changeCourageValue(serverPlayer, attributesData, -COURAGE_CHANGE_AMOUNT, targetCourage, maxCourage);
            }
            // 当前勇气值 == 目标值：不处理，保持不变
        }
    }

    @SubscribeEvent
    //监听玩家短时间内是否击杀多只生物，是，增加勇气值
    public static void onLivingDeath(LivingDeathEvent event) {
        //过滤条件：服务端 + 有击杀者 + 击杀者是LivingEntity
        if (event.getEntity().level().isClientSide() || event.getSource().getEntity() == null) {
            return;
        }
        LivingEntity targetEntity = event.getEntity(); // 被击杀的生物
        LivingEntity killerEntity = (LivingEntity) event.getSource().getEntity(); // 击杀者

        //过滤条件：击杀者是存活的ServerPlayer + 非创造模式
        if (!(killerEntity instanceof ServerPlayer serverPlayer) || !serverPlayer.isAlive()) {
            return;
        }
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        //过滤条件：被击杀的是敌对生物（怪物）
        EntityType<?> targetType = targetEntity.getType();
        //是否在敌对生物集合中，且不在排除列表中
        if (!HOSTILE_ENTITY_TYPES.contains(targetType) || EXCLUDED_HOSTILE_ENTITY.contains(targetType)) {
            return;
        }

        //处理击杀记录，判断是否增加勇气值
        handleKillCourageGain(serverPlayer);
    }

    /**
     * 统计附近30格内的存活服务端玩家数量（排除自身）
     */
    private static int getNearbyPlayerCount(ServerPlayer player) {
        if (player == null || player.level() == null || PLAYER_DISTANCE < 0) {
            return 0;
        }
        int count = 0;
        double detectionRangeSqr = PLAYER_DISTANCE * PLAYER_DISTANCE;
        UUID currentPlayerUUID = player.getUUID();

        for (Player onlinePlayer : player.level().players()) {
            // 过滤：自身、非存活、非服务端玩家
            if (onlinePlayer.getUUID().equals(currentPlayerUUID)
                    || !onlinePlayer.isAlive()
                    || !(onlinePlayer instanceof ServerPlayer)) {
                continue;
            }
            // 距离判断
            if (player.distanceToSqr(onlinePlayer) <= detectionRangeSqr) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断玩家是否处于夜晚的野外
     * @return true=夜晚野外，false=非夜晚/非野外
     */
    public static boolean isInNightWild(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }
        if (!isNightTime(player)) {
            return false;
        }
        if (!isInOpenWild(player)) {
            return false;
        }
        return true;
    }

    /**
     * 判断是否是夜晚
     */
    private static boolean isNightTime(ServerPlayer player) {
        long worldDayTime = player.level().getDayTime() % 24000L;
        return worldDayTime >= 13000L && worldDayTime <= 23999L;
    }

    /**
     * 判断是否是野外（露天）
     */
    private static boolean isInOpenWild(ServerPlayer player) {
        Level level = player.level();
        int playerX = (int) Math.floor(player.getX());
        int playerY = (int) Math.floor(player.getY());
        int playerZ = (int) Math.floor(player.getZ());
        BlockPos playerBlockPos = new BlockPos(playerX, playerY, playerZ);
        return level.canSeeSky(playerBlockPos.above());
    }

    //判断周围是否有至少1名玩家
    public static boolean hasPlayerAround(ServerPlayer player) {
        return getNearbyPlayerCount(player) >= 1;
    }

    //勇气值降低方法（保留原有方法，内部可复用通用方法）
    private static void handleCourageReduction(ServerPlayer player, PlayerAttributesData attrData) {
        UUID playerUUID = player.getUUID();
        int currentCourage = attrData.getCurrentCourage();
        int maxCourage = attrData.getMaxCourage();

        if (currentCourage <= 0) {
            return;
        }
        changeCourageValue(player, attrData, -COURAGE_REDUCE_AMOUNT, 0, maxCourage);
    }

    private static void handleKillCourageGain(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        long currentKillTime = System.currentTimeMillis();

        List<Long> playerKillTimestamps = PLAYER_KILL_RECORDS.getOrDefault(playerUUID, new ArrayList<>());
        playerKillTimestamps.add(currentKillTime);

        //清理过期记录
        Iterator<Long> iterator = playerKillTimestamps.iterator();
        while (iterator.hasNext()) {
            long oldKillTime = iterator.next();
            if (currentKillTime - oldKillTime > KILL_TIME_WINDOW) {
                iterator.remove();
            }
        }

        PLAYER_KILL_RECORDS.put(playerUUID, playerKillTimestamps);

        //判断是否满足触发条件
        if (playerKillTimestamps.size() >= KILL_THRESHOLD) {
            PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
            if (attrData == null) {
                return;
            }

            int maxCourage = attrData.getMaxCourage();
            changeCourageValue(player, attrData, COURAGE_ADD_AMOUNT, 0, maxCourage);

            EconomySystem.LOGGER.debug("玩家 {} 短时间内击杀{}只敌对生物，勇气值+{}，当前{}点",
                    player.getScoreboardName(), KILL_THRESHOLD, COURAGE_ADD_AMOUNT, attrData.getCurrentCourage());

            //清空击杀记录
            PLAYER_KILL_RECORDS.put(playerUUID, new ArrayList<>());
        }
    }

    /**
     * 玩家登出时清理其击杀记录，防止内存泄漏
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID playerUUID = serverPlayer.getUUID();
        PLAYER_KILL_RECORDS.remove(playerUUID);
    }

    /**
     * 勇气值调整方法
     * @param serverPlayer 玩家实例
     * @param attributesData 玩家勇气值数据
     * @param changeAmount 变更数量（正数=增加，负数=减少）
     * @param targetCourage 目标勇气值（用于限制不超过/不低于该值，0则不限制目标，仅限制最大/最小）
     * @param maxCourage 玩家最大勇气值
     */
    private static void changeCourageValue(ServerPlayer serverPlayer, PlayerAttributesData attributesData, int changeAmount, int targetCourage, int maxCourage) {
        int currentCourage = attributesData.getCurrentCourage();
        int newCourage = currentCourage + changeAmount;

        //限制不低于0，不高于最大勇气值
        newCourage = Math.max(newCourage, 0);
        newCourage = Math.min(newCourage, maxCourage);

        //若目标勇气值>0，限制不超过/不低于目标值（根据变更方向）
        if (targetCourage > 0) {
            if (changeAmount > 0) {
                //增加勇气值：不超过目标值
                newCourage = Math.min(newCourage, targetCourage);
            } else if (changeAmount < 0) {
                //减少勇气值：不低于目标值
                newCourage = Math.max(newCourage, targetCourage);
            }
        }

        // 若勇气值未变化，直接返回，避免无效持久化
        if (currentCourage == newCourage) {
            return;
        }

        //更新勇气值并持久化
        attributesData.setCurrentCourage(newCourage);
        PlayerAttributesDataManager.updatePlayerAttributesData(serverPlayer, attributesData);

        //————————————客户端
        // 获取最新的当前勇气值和最大勇气值
        int syncCurrentCourage = attributesData.getCurrentCourage();
        int syncMaxCourage = attributesData.getMaxCourage();
        // 调用同步方法，发送给客户端
        PlayerCourageClientSync.sendCourageDataToClient(serverPlayer, syncCurrentCourage, syncMaxCourage);
    }


    //——————————————————————————客户端
    /**
     * 客户端：设置当前勇气值（供Packet_SyncCourageData调用）
     */
    public static void setCurrentCourage(Player player, int currentCourage) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        CLIENT_CURRENT_COURAGE = currentCourage;
    }

    /**
     * 客户端：设置最大勇气值（供Packet_SyncCourageData调用）
     */
    public static void setMaxCourage(Player player, int maxCourage) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        CLIENT_MAX_COURAGE = maxCourage;
    }

    /**
     * 客户端：获取当前勇气值（供CustomStatueGUI调用）
     */
    public static int getCurrentCourageClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 50; // 兜底默认值
        }
        return CLIENT_CURRENT_COURAGE;
    }

    /**
     * 客户端：获取最大勇气值（供CustomStatueGUI调用）
     */
    public static int getMaxCourageClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 100; // 兜底默认值
        }
        return CLIENT_MAX_COURAGE;
    }
}