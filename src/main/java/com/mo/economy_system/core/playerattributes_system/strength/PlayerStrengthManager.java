package com.mo.economy_system.core.playerattributes_system.strength;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家体力（力量）系统核心管理器
 * 负责体力消耗、恢复、疾跑拦截、提示显示等全逻辑
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerStrengthManager {

    private static final int SPRINT_COST_PER_5TICK = 1;    // 疾跑每5tick耗1点体力（每秒4点）
    private static final int JUMP_COST_ONCE = 3;           // 跳跃单次消耗体力
    private static final int COOLDOWN_TICK = 100;          // 体力恢复冷却时间【5秒=100tick】
    private static final int RESTORE_AMOUNT = 5;          // 每次恢复体力值
    private static final int RESTORE_INTERVAL = 20;        // 体力恢复间隔【1秒=20tick】
    public static final int MIN_RESPRINT_STRENGTH = 20;   // 体力耗尽后，重新疾跑的最低体力值
    private static final int SPRINT_STOP_STRENGTH = 0;     // 强制停跑的体力阈值（归0才停）
    private static final float LOW_STRENGTH_PERCENT = 0.3f; // 低体力提示阈值（30%）

    //服务端缓存
    private static final Map<UUID, Long> LAST_CONSUME_TICK = new ConcurrentHashMap<>(); // 玩家最后消耗体力的tick
    private static final Map<UUID, Boolean> IS_SPRINTING_CACHE = new ConcurrentHashMap<>(); // 玩家疾跑状态缓存
    public static final Map<UUID, Boolean> IS_STRENGTH_EXHAUSTED = new ConcurrentHashMap<>(); // 服务端体力耗尽标记
    private static final Map<UUID, Boolean> HAS_SHOWN_LOW_STRENGTH_TIP = new ConcurrentHashMap<>(); // 30%体力提示是否已显示
    private static final Map<UUID, Boolean> HAS_SHOWN_EXHAUSTED_TIP = new ConcurrentHashMap<>(); // 无法奔跑提示是否已显示

    //客户端缓存
    @OnlyIn(Dist.CLIENT)
    public static final Map<UUID, Boolean> IS_STRENGTH_EXHAUSTED_CLIENT = new ConcurrentHashMap<>(); // 客户端体力耗尽标记

    //Tick监听
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 仅在服务端执行，且玩家存活
        if (event.side.isClient() || !event.player.isAlive() || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        //创造模式强制设为满体力并直接返回
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            UUID uuid = serverPlayer.getUUID();
            PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(uuid);
            if (attrData != null) {
                // 临时赋予满体力值
                int maxStrength = attrData.getMaxStrength();
                if (attrData.getCurrentStrength() != maxStrength) {
                    attrData.setCurrentStrength(maxStrength);
                    PlayerAttributesDataManager.updatePlayerAttributesData(serverPlayer, attrData);
                    StrengthSyncManager.syncStrengthToClient(serverPlayer); // 同步到客户端显示满体力
                }
            }
            return; // 跳过所有体力消耗/恢复逻辑
        }

        UUID uuid = serverPlayer.getUUID();
        PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(uuid);
        if (attrData == null) return; // 空值防护

        //优先检查疾跑状态
        checkSprintState(serverPlayer, attrData);
        //消耗 + 恢复
        handleSprintConsume(serverPlayer, attrData, event.player.tickCount);
        handleStrengthRestore(serverPlayer, attrData, event.player.tickCount);
    }

    //跳跃事件监听
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity livingEntity = event.getEntity();
        // 仅在服务端执行，且是存活的玩家
        if (livingEntity.level().isClientSide() || !(livingEntity instanceof ServerPlayer serverPlayer) || !serverPlayer.isAlive()) {
            return;
        }

        //创造模式跳过跳跃体力消耗
        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }

        UUID uuid = serverPlayer.getUUID();
        PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(uuid);
        if (attrData == null) return;

        // 跳跃无条件消耗体力
        boolean consumeSuccess = attrData.consumeStrength(JUMP_COST_ONCE);
        // 跳跃重置冷却CD
        LAST_CONSUME_TICK.put(uuid, (long) serverPlayer.tickCount);
        PlayerAttributesDataManager.updatePlayerAttributesData(serverPlayer, attrData);
        //同步客户端，渲染体力条
        StrengthSyncManager.syncStrengthToClient(serverPlayer);
    }

    //检查并控制疾跑状态
    private static void checkSprintState(ServerPlayer player, PlayerAttributesData data) {
        UUID uuid = player.getUUID();
        int currentStrength = data.getCurrentStrength();
        int maxStrength = data.getMaxStrength();
        boolean isSprinting = player.isSprinting();
        boolean isExhausted = IS_STRENGTH_EXHAUSTED.getOrDefault(uuid, false);
        // 计算体力百分比
        float strengthPercent = maxStrength == 0 ? 0 : (float) currentStrength / maxStrength;
        // 提示显示标记
        boolean hasShownLowTip = HAS_SHOWN_LOW_STRENGTH_TIP.getOrDefault(uuid, false);
        boolean hasShownExhaustedTip = HAS_SHOWN_EXHAUSTED_TIP.getOrDefault(uuid, false);

        if (strengthPercent <= LOW_STRENGTH_PERCENT && !hasShownLowTip) {
            player.displayClientMessage(
                    Component.literal("§b老己～体力告急啦，悠着点跑哦"), // 硬编码提示文本（物品栏上方）
                    true // true=动作栏（物品栏上方），false=聊天框
            );
            HAS_SHOWN_LOW_STRENGTH_TIP.put(uuid, true);
//            EconomySystem.LOGGER.debug("[体力提示] 玩家{}体力≤30%，已显示低体力提示", player.getScoreboardName());
        }
        if (strengthPercent > LOW_STRENGTH_PERCENT && hasShownLowTip) {
            HAS_SHOWN_LOW_STRENGTH_TIP.put(uuid, false);
        }

        if (currentStrength <= SPRINT_STOP_STRENGTH && isSprinting) {
            player.setSprinting(false);
            IS_STRENGTH_EXHAUSTED.put(uuid, true);
            IS_SPRINTING_CACHE.put(uuid, false);
//            EconomySystem.LOGGER.warn("[体力] 玩家{}体力已耗尽（0点），强制停止疾跑", player.getScoreboardName());

            // 显示无法奔跑提示（仅一次）
            if (!hasShownExhaustedTip) {
                player.displayClientMessage(
                        Component.literal("§c老己~，跑不动啦歇会儿吧，休息就能恢复体力啦❤"), // 硬编码提示文本
                        true
                );
                HAS_SHOWN_EXHAUSTED_TIP.put(uuid, true);
//                EconomySystem.LOGGER.debug("[体力提示] 玩家{}体力耗尽，已显示无法奔跑提示", player.getScoreboardName());
            }
        }

        //耗尽标记未解除（体力<20）→ 禁止疾跑
        if (isExhausted && currentStrength < MIN_RESPRINT_STRENGTH && isSprinting) {
            player.setSprinting(false);
            player.displayClientMessage(
                    Component.literal("§c老己~，跑不动啦歇会儿吧，休息就能恢复体力啦❤"), // 硬编码提示文本
                    true
            );
//            EconomySystem.LOGGER.debug("[体力] 玩家{}体力未恢复到{}（当前{}），后台禁止疾跑",
//                    player.getScoreboardName(), MIN_RESPRINT_STRENGTH, currentStrength);
        }

        //体力≥20 → 解除耗尽标记 + 重置提示标记
        if (currentStrength >= MIN_RESPRINT_STRENGTH && isExhausted) {
            IS_STRENGTH_EXHAUSTED.put(uuid, false);
            HAS_SHOWN_EXHAUSTED_TIP.put(uuid, false); // 重置无法奔跑提示标记
//            EconomySystem.LOGGER.info("[标记解除] 玩家{}体力≥20，IS_STRENGTH_EXHAUSTED已设为false",
//                    player.getScoreboardName());
        }
    }

    private static void handleSprintConsume(ServerPlayer player, PlayerAttributesData data, int currentTick) {
        UUID uuid = player.getUUID();
        boolean nowSprint = player.isSprinting();
        boolean preSprint = IS_SPRINTING_CACHE.getOrDefault(uuid, false);
        boolean isExhausted = IS_STRENGTH_EXHAUSTED.getOrDefault(uuid, false);

        // 体力耗尽标记未解除 → 不执行任何消耗
        if (isExhausted) {
            IS_SPRINTING_CACHE.put(uuid, false);
            return;
        }

        if (nowSprint) {
            // 每5tick消耗一次体力（性能优化）
            if (currentTick % 5 == 0) {
                // 体力>0才允许消耗
                if (data.getCurrentStrength() > SPRINT_STOP_STRENGTH) {
                    boolean consumeOk = data.consumeStrength(SPRINT_COST_PER_5TICK);
                    if (consumeOk) {
                        LAST_CONSUME_TICK.put(uuid, (long) currentTick);
                        IS_SPRINTING_CACHE.put(uuid, true);
//                        EconomySystem.LOGGER.debug("[体力] 玩家{}疾跑消耗体力，剩余{}",
//                                player.getScoreboardName(), data.getCurrentStrength());
                        PlayerAttributesDataManager.updatePlayerAttributesData(player, data);
                        //同步到客户端，用于渲染体力条
                        StrengthSyncManager.syncStrengthToClient(player);
                    }
                }
            }
        } else {
            // 玩家停止疾跑，更新缓存
            if (preSprint) {
                IS_SPRINTING_CACHE.put(uuid, false);
            }
        }
    }

    //体力恢复核心逻辑
    private static void handleStrengthRestore(ServerPlayer player, PlayerAttributesData data, int currentTick) {
        UUID uuid = player.getUUID();
        long lastTick = LAST_CONSUME_TICK.getOrDefault(uuid, 0L);

        if (currentTick - lastTick >= COOLDOWN_TICK
                && !player.isSprinting()
                && currentTick % RESTORE_INTERVAL == 0) {
            data.restoreStrength(RESTORE_AMOUNT);
//            EconomySystem.LOGGER.debug("[体力] 玩家{}体力恢复，当前{}",
//                    player.getScoreboardName(), data.getCurrentStrength());
            PlayerAttributesDataManager.updatePlayerAttributesData(player, data);
            //同步到客户端，渲染体力条
            StrengthSyncManager.syncStrengthToClient(player);

            //若体力≥20，解除耗尽标记
            if (data.getCurrentStrength() >= MIN_RESPRINT_STRENGTH) {
                IS_STRENGTH_EXHAUSTED.put(uuid, false);
                HAS_SHOWN_EXHAUSTED_TIP.put(uuid, false); // 重置提示标记
            }
        }
    }

    //玩家登出：清理所有缓存
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            //清理所有缓存
            LAST_CONSUME_TICK.remove(uuid);
            IS_SPRINTING_CACHE.put(uuid, false);
            IS_SPRINTING_CACHE.remove(uuid);
            IS_STRENGTH_EXHAUSTED.remove(uuid);
            HAS_SHOWN_LOW_STRENGTH_TIP.remove(uuid);
            HAS_SHOWN_EXHAUSTED_TIP.remove(uuid);
        }
    }

    //手动消耗体力
    public static boolean consumeStrength(ServerPlayer player, int amount) {
        //创造模式直接返回false
        if (player == null || !player.isAlive() || player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return false;
        }
        PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
        if (data == null) return false;

        boolean success = data.consumeStrength(amount);
        if (success) {
            LAST_CONSUME_TICK.put(player.getUUID(), (long) player.tickCount);
            PlayerAttributesDataManager.updatePlayerAttributesData(player, data);

            if (data.getCurrentStrength() <= SPRINT_STOP_STRENGTH) {
                player.setSprinting(false);
                IS_STRENGTH_EXHAUSTED.put(player.getUUID(), true);

                if (!HAS_SHOWN_EXHAUSTED_TIP.getOrDefault(player.getUUID(), false)) {
                    player.displayClientMessage(
                            Component.literal("§c老己~，跑不动啦歇会儿吧，休息就能恢复体力啦❤"),
                            true
                    );
                    HAS_SHOWN_EXHAUSTED_TIP.put(player.getUUID(), true);
                }
            }
        }
        StrengthSyncManager.syncStrengthToClient(player);
        return success;
    }

    //手动恢复体力
    public static void restoreStrength(ServerPlayer player, int amount) {
        //创造模式直接返回，不执行恢复
        if (player == null || !player.isAlive() || player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
            return;
        }
        PlayerAttributesData data = PlayerAttributesDataManager.getPlayerAttributesData(player.getUUID());
        if (data == null) return;

        data.restoreStrength(amount);
        PlayerAttributesDataManager.updatePlayerAttributesData(player, data);

        //若体力≥20，解除耗尽标记 + 重置提示
        if (data.getCurrentStrength() >= MIN_RESPRINT_STRENGTH) {
            IS_STRENGTH_EXHAUSTED.put(player.getUUID(), false);
            HAS_SHOWN_EXHAUSTED_TIP.put(player.getUUID(), false);
            HAS_SHOWN_LOW_STRENGTH_TIP.put(player.getUUID(), false); // 重置低体力提示
        }
        StrengthSyncManager.syncStrengthToClient(player);
    }

    //客户端tick监听器：检查体力耗尽标记，强制停止疾跑
    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientTickHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || !player.isAlive()) return;

            UUID uuid = player.getUUID();
            boolean isExhausted = IS_STRENGTH_EXHAUSTED_CLIENT.getOrDefault(uuid, false);

            // 如果体力耗尽，强制停止疾跑（使用更高优先级的方式）
            if (isExhausted) {
                // 强制设置疾跑为false（每次tick开始时设置，在输入处理之前）
                player.setSprinting(false);
                // 强制设置移动速度为步行速度
                player.setSpeed(Math.min(player.getSpeed(), 0.1f));
            }
        }

        // 监听鼠标和键盘输入事件，阻止疾跑输入
        @SubscribeEvent
        public static void onInput(InputEvent.MouseButton event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || !player.isAlive()) return;

            UUID uuid = player.getUUID();
            boolean isExhausted = IS_STRENGTH_EXHAUSTED_CLIENT.getOrDefault(uuid, false);

            // 如果体力耗尽且正在疾跑，强制停止并取消事件
            if (isExhausted && player.isSprinting()) {
                player.setSprinting(false);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || !player.isAlive()) return;

            UUID uuid = player.getUUID();
            boolean isExhausted = IS_STRENGTH_EXHAUSTED_CLIENT.getOrDefault(uuid, false);

            // 如果体力耗尽且正在疾跑，强制停止
            if (isExhausted && player.isSprinting()) {
                player.setSprinting(false);
            }
        }
    }
}