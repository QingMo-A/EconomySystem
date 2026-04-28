package com.mo.economy_system.core.playerattributes_system.courage;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.entity.EconomySystem_Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class PlayerCourageManager {
    private static final int PLAYER_DISTANCE = 30; //一定距离内是否有玩家
    private static final int COURAGE_REDUCE_TICK_INTERVAL = 20; //多少tick一次检测
    private static final int COURAGE_REDUCE_INTERVAL = 200; //多少tick一次扣除（夜晚无人）

    //黑暗的光照等级阈值
    private static final int ENVIRONMENT_DARK_LIGHT_LEVEL = 7;

    private static final long KILL_TIME_WINDOW = 10000L; //击杀时间窗口（10秒，毫秒级）
    private static final int KILL_THRESHOLD = 5; //时间窗口内需要击杀的敌对生物数量
    private static final int COURAGE_ADD_AMOUNT = 10; //满足条件后增加的勇气值
    private static final float NORMAL_RECOVERY_CAP = 60.0F; //普通恢复上限，超过后只能靠快速击杀继续提升
    private static final float SAFE_DECAY_AMOUNT = 0.5F; //白天安全环境下，超出60的勇气会缓慢回落

    // 玩家死亡扣除勇气值配置
    private static final int NEARBY_PLAYER_DEATH_DISTANCE = 10; // 玩家死亡影响范围（格）
    private static final int NEARBY_PLAYER_DEATH_COURAGE_PENALTY = 10; // 附近玩家死亡扣除的勇气值

    //存储击杀记录
    private static final Map<UUID, List<Long>> PLAYER_KILL_RECORDS = new ConcurrentHashMap<>();

    //敌对生物集合
    private static final Set<EntityType<?>> HOSTILE_ENTITY_TYPES = new HashSet<>();
    //被排除的敌对生物
    private static final Set<EntityType<?>> EXCLUDED_HOSTILE_ENTITY = new HashSet<>();

    //消息冷却时间
    private static final Map<UUID, Integer> COURAGE_MSG_COOLDOWN = new ConcurrentHashMap<>();
    private static final int MSG_COOLDOWN_TICKS = 1200;


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
    @EventBusSubscriber(modid = EconomySystem.MODID)
    public static class EntityRegisterHandler {
        @SubscribeEvent
        public static void registerEntityAttributes(FMLCommonSetupEvent event) {
            EconomySystem.LOGGER.info("延迟注册丧尸");
            HOSTILE_ENTITY_TYPES.add(EconomySystem_Entities.HIVE_ZOMBIE.get());
        }
    }

    @SubscribeEvent
    //判断玩家状态，执行勇气值变更（夜晚无人扣除）与buff逻辑
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        
        if (event.getEntity().level().isClientSide() || !event.getEntity().isAlive() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
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

        //处理提示冷却
        int currentCooldown = COURAGE_MSG_COOLDOWN.getOrDefault(playerUUID, 0);
        if (currentCooldown > 0) {
            COURAGE_MSG_COOLDOWN.put(playerUUID, currentCooldown - 1);
        }
        //是否已触发过提示（冷却未结束则跳过）
        boolean canShowMsg = currentCooldown <= 0;

        if (serverPlayer.tickCount % COURAGE_REDUCE_TICK_INTERVAL != 0) {
            return;
        }

        boolean isDarkEnvironment = isEnvironmentDark(serverPlayer);
        boolean isGameNight = isNightTime(serverPlayer);
        int nearbyPlayerCount = getNearbyPlayerCount(serverPlayer);
        float maxCourage = attributesData.getMaxCourage();
        float currentCourage = attributesData.getCurrentCourage();
        int nearbyMonsterCount = getNearbyMonsterEntityCount(serverPlayer);

        double totalAdd = 0.0; // 所有增加类数值
        double totalReduce = 0.0;
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

            if (canShowMsg) {
                serverPlayer.displayClientMessage(
                        Component.literal("§c恐惧充斥了您的身体..."), // 自定义文本
                        true
                );
                COURAGE_MSG_COOLDOWN.put(playerUUID, MSG_COOLDOWN_TICKS); // 触发冷却
            }
//            EconomySystem.LOGGER.debug("玩家 {} 勇气值不足20%，施加虚弱I和缓慢I效果", serverPlayer.getScoreboardName());
        }
        // 勇气值高于一定值：施加力量I
        else if (courageRatio >= 0.85F) {
            MobEffectInstance strengthEffect = new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, true);
            serverPlayer.addEffect(strengthEffect);

            if (canShowMsg) {
                serverPlayer.displayClientMessage(
                        Component.literal("§a您浑身充满力量，无所畏惧！"), // 自定义文本
                        true
                );
                COURAGE_MSG_COOLDOWN.put(playerUUID, MSG_COOLDOWN_TICKS); // 触发冷却
            }
//            EconomySystem.LOGGER.debug("玩家 {} 勇气值≥70%，施加力量I效果", serverPlayer.getScoreboardName());
        }

        // 主逻辑（10s计算一次）
        // 白天————
        // 所有的增加 * 1.2
        // 所有的扣除 * 0.8
        // 晚上————
        // 所有的增加 * 0.8
        // 所有的扣除 * 1.2
        // 光照计算————
        // 光亮环境下，固定+1点
        // 黑暗环境下，固定-1点
        // 附近20格内有怪物计算————
        // 扣除 （怪物数量 * 0.5）点，5点封顶
        // 附近30格内有玩家计算————
        // 增加 （玩家数量 * 0.5） 点，3点封顶
        // 坐标计算————
        // y坐标 < 30 且黑暗  扣除0.5点
        double lightNum = 0.0d;
        double monsterNum = 0.0d;
        double playerNum = 0.0d;
        double yNum = 0.0d;
        double addMultiNum = 0.0d;
        double reduceMultiNum = 0.0d;
        if (isDarkEnvironment) {
            lightNum = -1.0d;
        } else {
            lightNum = 1.0d;
        }

        if (isGameNight) {
            addMultiNum = 0.8d;
            reduceMultiNum = 1.2d;
        } else {
            addMultiNum = 1.2d;
            reduceMultiNum = 0.8d;
        }

        monsterNum = -(nearbyMonsterCount * 0.5);
        monsterNum = Math.max(monsterNum, -3.0);

        playerNum = nearbyPlayerCount * 0.5;
        playerNum = Math.min(playerNum, 3.0);

        int playerY = serverPlayer.blockPosition().getY();
        if (playerY < 30 && isDarkEnvironment) {
            yNum = -0.5;
        } else {
            yNum = 0.0;
        }

        if (lightNum > 0) {
            totalAdd += lightNum;
        } else {
            totalReduce += Math.abs(lightNum);
        }
        totalAdd += playerNum;
        totalReduce += Math.abs(monsterNum);
        totalReduce += Math.abs(yNum);

        totalAdd = totalAdd * addMultiNum;
        totalReduce = totalReduce * reduceMultiNum;

        double finalChangeDouble = totalAdd - totalReduce;
        float finalChange =  (float) finalChangeDouble;

        if (isSafeEnvironmentForCourageDecay(isDarkEnvironment, isGameNight, nearbyMonsterCount)
                && currentCourage > NORMAL_RECOVERY_CAP) {
            finalChange = Math.min(finalChange, -SAFE_DECAY_AMOUNT);
        }

        if (serverPlayer.tickCount % COURAGE_REDUCE_INTERVAL == 0) {
            // 执行勇气值变更
            changeCourageValue(serverPlayer, attributesData, (float) finalChange, maxCourage, false);

            // 日志输出
//            EconomySystem.LOGGER.info("===== 玩家{}勇气值计算结果 =====", serverPlayer.getName().getString());
//            EconomySystem.LOGGER.info("当前勇气值：{} | 最大值：{}", currentCourage, maxCourage);
//            EconomySystem.LOGGER.info("基础值：光照={}, 怪物={}, 玩家={}, y坐标={}", lightNum, monsterNum, playerNum, yNum);
//            EconomySystem.LOGGER.info("倍率后：增加={}, 扣除={}", totalAdd, totalReduce);
//            EconomySystem.LOGGER.info("最终变化：{}点（浮点={}）", finalChange, finalChange);
        }
    }

    @SubscribeEvent
    //监听玩家短时间内是否击杀多只生物，是，增加勇气值
    //同时监听玩家死亡，扣除附近玩家的勇气值
    public static void onLivingDeath(LivingDeathEvent event) {
        //过滤条件：服务端
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity targetEntity = event.getEntity(); // 被击杀的生物

        // 1. 处理玩家死亡事件：附近玩家扣除勇气值
        if (targetEntity instanceof ServerPlayer deadPlayer) {
            handleNearbyPlayerDeathCouragePenalty(deadPlayer);
        }

        // 2. 处理击杀怪物事件：击杀者增加勇气值
        if (event.getSource().getEntity() == null) {
            return;
        }
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
     * 统计附近20格内的敌对生物数量
     */
    private static int getNearbyMonsterEntityCount(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return 0;
        }
        int count = 0;
        double detectionRangeSqr = 20 * 20;

        // 简化遍历逻辑，直接用玩家的AABB范围
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20))) {
            if (entity instanceof Player || entity.isDeadOrDying()) continue; // 排除玩家/死亡实体
            EntityType<?> entityType = entity.getType();
            if (HOSTILE_ENTITY_TYPES.contains(entityType) && !EXCLUDED_HOSTILE_ENTITY.contains(entityType)) {
                if (entity.distanceToSqr(player) <= detectionRangeSqr) {
                    count++;
                }
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
        if (!isEnvironmentDark(player)) {
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

    private static boolean isSafeEnvironmentForCourageDecay(boolean isDarkEnvironment, boolean isGameNight, int nearbyMonsterCount) {
        return !isDarkEnvironment && !isGameNight && nearbyMonsterCount <= 0;
    }

    /**
     * 判断玩家当前所在环境是否黑暗（替代原有的“游戏时间黑夜”判断）
     * @param player 玩家实例
     * @return true=环境黑暗（光照≤阈值），false=环境明亮
     */
    private static boolean isEnvironmentDark(ServerPlayer player) {
        if (player == null || player.level() == null) {
            return false;
        }
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();

        // 通过LevelLightEngine获取玩家位置的总光照亮度（适配你的Level类源码）
        LevelLightEngine lightEngine = level.getLightEngine();
        // 获取方块光照（光源：火把、熔岩等）+ 天空光照的最大值
        int totalLight = lightEngine.getRawBrightness(playerPos, 0);

        // 光照≤阈值则判定为环境黑暗
        return totalLight <= ENVIRONMENT_DARK_LIGHT_LEVEL;
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


    /**
     * 处理玩家死亡时附近玩家的勇气值扣除
     * @param deadPlayer 死亡的玩家
     */
    private static void handleNearbyPlayerDeathCouragePenalty(ServerPlayer deadPlayer) {
        if (deadPlayer == null || deadPlayer.level() == null) {
            return;
        }

        // 过滤创造模式玩家
        if (deadPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        Level level = deadPlayer.level();
        double detectionRangeSqr = NEARBY_PLAYER_DEATH_DISTANCE * NEARBY_PLAYER_DEATH_DISTANCE;
        UUID deadPlayerUUID = deadPlayer.getUUID();

        // 遍历所有在线玩家，找出附近10格内的玩家
        for (Player nearbyPlayer : level.players()) {
            // 过滤：死亡玩家自身、非存活、非服务端玩家
            if (nearbyPlayer.getUUID().equals(deadPlayerUUID)
                    || !nearbyPlayer.isAlive()
                    || !(nearbyPlayer instanceof ServerPlayer nearbyServerPlayer)) {
                continue;
            }

            // 距离判断：10格内
            if (deadPlayer.distanceToSqr(nearbyPlayer) <= detectionRangeSqr) {
                // 排除创造模式玩家
                if (nearbyServerPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    continue;
                }

                UUID nearbyPlayerUUID = nearbyServerPlayer.getUUID();
                PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(nearbyPlayerUUID);
                if (attributesData == null) {
                    continue;
                }

                float maxCourage = attributesData.getMaxCourage();
                // 扣除10点勇气值
                changeCourageValue(nearbyServerPlayer, attributesData, -NEARBY_PLAYER_DEATH_COURAGE_PENALTY, maxCourage, false);

                // 发送提示消息
                nearbyServerPlayer.displayClientMessage(
                        Component.literal("§c您目睹了玩家的死亡，恐惧席卷了您..."),
                        true
                );
            }
        }
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

            float maxCourage = attrData.getMaxCourage();
            changeCourageValue(player, attrData, COURAGE_ADD_AMOUNT , maxCourage, true);

            int currentCooldown = COURAGE_MSG_COOLDOWN.getOrDefault(playerUUID, 0);
            if (currentCooldown <= 0) {
                player.displayClientMessage(
                        Component.literal("§a连续击杀敌对生物让您信心倍增..."), // 自定义文本
                        true
                );
                COURAGE_MSG_COOLDOWN.put(playerUUID, MSG_COOLDOWN_TICKS); // 触发冷却
            }

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
        COURAGE_MSG_COOLDOWN.remove(playerUUID);
    }

    /**
     * 勇气值调整方法（无目标值，直接增减）
     * @param serverPlayer 玩家实例
     * @param attributesData 玩家勇气值数据
     * @param changeAmount 变更数量（正数=增加，负数=减少）
     * @param maxCourage 玩家最大勇气值
     */
    private static void changeCourageValue(ServerPlayer serverPlayer, PlayerAttributesData attributesData, float changeAmount, float maxCourage, boolean canBreakNormalRecoveryCap) {
        float currentCourage = attributesData.getCurrentCourage();
        float newCourage = currentCourage + changeAmount;

        if (changeAmount > 0 && !canBreakNormalRecoveryCap) {
            newCourage = Math.min(newCourage, Math.min(NORMAL_RECOVERY_CAP, maxCourage));
        }

        // 只保留基础限制：不低于0，不高于最大勇气值
        newCourage = Math.max(newCourage, 0);
        newCourage = Math.min(newCourage, maxCourage);

        // 若勇气值未变化，直接返回，避免无效持久化
        if (Math.abs(currentCourage - newCourage) < 0.001f) { // 浮点精度容错
            return;
        }

        //更新勇气值并持久化
        attributesData.setCurrentCourage(newCourage);
        PlayerAttributesDataManager.updatePlayerAttributesData(serverPlayer, attributesData);

        //————————————客户端同步
        float syncCurrentCourage = attributesData.getCurrentCourage();
        float syncMaxCourage = attributesData.getMaxCourage();
        PlayerCourageClientSync.sendCourageDataToClient(serverPlayer, syncCurrentCourage, syncMaxCourage);
    }


    // 客户端
    public static void setCurrentCourage(Player player, float currentCourage) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        PlayerAttributesData data = ClientCacheManager.getOrCreatePlayerAttributesData(player.getUUID());
        data.setCurrentCourage(currentCourage);
        ClientCacheManager.setPlayerAttributesData(player.getUUID(), data);
    }

    public static void setMaxCourage(Player player, float maxCourage) {
        if (player == null || !player.level().isClientSide()) {
            return;
        }
        PlayerAttributesData data = ClientCacheManager.getOrCreatePlayerAttributesData(player.getUUID());
        data.setMaxCourage(maxCourage);
        ClientCacheManager.setPlayerAttributesData(player.getUUID(), data);
    }

    public static float getCurrentCourageClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 50.0f;
        }
        PlayerAttributesData data = ClientCacheManager.getPlayerAttributesData(player.getUUID());
        return data != null ? data.getCurrentCourage() : 50.0f;
    }

    public static float getMaxCourageClient(Player player) {
        if (player == null || !player.level().isClientSide()) {
            return 100.0f;
        }
        PlayerAttributesData data = ClientCacheManager.getPlayerAttributesData(player.getUUID());
        return data != null ? data.getMaxCourage() : 100.0f;
    }
}
