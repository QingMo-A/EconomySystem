package com.mo.economy_system.item.items.medicine;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.health.PlayerCustomHealthManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

public class Easy_Aid_Kit extends Item {
    // 状态标记
    private static final String USING_FIRST_AID = "UsingEasyAidKit";
    // 耐久消耗计数器
    private static final String DURABILITY_CONSUME_TICK = "AidKitDurabilityTick";
    // 急救包启动时间
    private static final String FIRST_AID_START_TIME = "FirstAidStartTime";
    // 配置参数
    private static final int HEAL_INTERVAL = 20; // 20刻=1秒，回血效果更明显
    private static final double PER_HEAL_AMOUNT = 1.0; // 自定义血量回血值（double适配自定义系统）
    private static final int DURABILITY_CONSUME_INTERVAL = 20; //20刻消耗一点耐久
    private static final long MEDICINE_COOLDOWN = 1000; // 1秒冷却
    private static final int DURABILITY_COST_PER_TIME = 1; // 每次消耗1点耐久
    private static final int START_DELAY_TICKS = 100; //启动延迟时间

    public Easy_Aid_Kit() {
        super(new Item.Properties().stacksTo(1).durability(10));
    }

    // 禁用原版使用流程
    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 0;
    }

    //none无原版使用动画
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // 右键切换使用状态
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldItemStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            //返回右键操作成功
            return InteractionResultHolder.sidedSuccess(heldItemStack, true);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        //读取 NBT 中存储的 “是否正在使用急救包” 状态
        CompoundTag playerData = serverPlayer.getPersistentData();
        boolean isUsing = playerData.getBoolean(USING_FIRST_AID);

        //读取出来的是false，说明刚才是没有使用，现在按下右键准备要使用了
        if (!isUsing) {
            //耐久读判断
            boolean hasDurability = heldItemStack.getDamageValue() < heldItemStack.getMaxDamage();
            boolean needHeal = player.getHealth() < player.getMaxHealth();

            if (hasDurability && needHeal) {
                //把标记设置为正在使用
                playerData.putBoolean(USING_FIRST_AID, true);
                //耐久消耗计数器
                playerData.putInt(DURABILITY_CONSUME_TICK, 0);
                //先删除旧的启动时间
                playerData.remove(FIRST_AID_START_TIME);
                //记录急救包启动时的游戏时间戳
                playerData.putLong(FIRST_AID_START_TIME, serverPlayer.level().getGameTime());
                serverPlayer.sendSystemMessage(Component.literal("§b正在使用简易急救包"), true);
                return InteractionResultHolder.success(heldItemStack);
            } else if (!needHeal) {
                serverPlayer.sendSystemMessage(Component.literal("§a你的生命值已满，无需使用急救包！"), true);
            } else if (!hasDurability) {
                serverPlayer.sendSystemMessage(Component.literal("§c急救包已损坏，无法使用！"), true);
            }
        } else {
            // 清除状态
            FirstAidTickHandler.clearUsingState(serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("§c简易急救包已手动停止使用！"), true);
            return InteractionResultHolder.success(heldItemStack);
        }

        return InteractionResultHolder.pass(heldItemStack);
    }

    @EventBusSubscriber(modid = EconomySystem.MODID)
    public static class FirstAidTickHandler {
        @SubscribeEvent
        public static void onPlayerTick(PlayerTickEvent.Post event) {
            if (event.getEntity().level().isClientSide() || !event.getEntity().isAlive()) { return; }

            ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
            CompoundTag playerData = serverPlayer.getPersistentData();
            boolean isUsing = playerData.getBoolean(USING_FIRST_AID);

            if (!isUsing) {
                //若玩家未处于使用状态，重置耐久计数器并返回，不执行后续逻辑。
                playerData.putInt(DURABILITY_CONSUME_TICK, 0);
                //未使用时，清空启动时间戳
                playerData.remove(FIRST_AID_START_TIME);
                //移除效果
                serverPlayer.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                return;
            }

            if (!serverPlayer.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 0, false, false, false));
            }

            ItemStack aidKitStack = getHeldAidKit(serverPlayer);
            if (aidKitStack.isEmpty()) {
                clearUsingState(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal("§c简易急救包已手动停止使用！"), true);
                return;
            }

            boolean isHealthFull = serverPlayer.getHealth() >= serverPlayer.getMaxHealth(); //如果满血，停止使用
            boolean isDurabilityEmpty = aidKitStack.getDamageValue() >= aidKitStack.getMaxDamage(); //如果没有耐久，停止使用
            if (isHealthFull || isDurabilityEmpty) {
                if (isHealthFull) {
                    serverPlayer.sendSystemMessage(Component.literal("§a生命值已恢复至满值，急救包自动停止！"), true);
                }
                if (isDurabilityEmpty) {
                    serverPlayer.sendSystemMessage(Component.literal("§c急救包耐久耗尽，自动停止！"), true);
                }
                clearUsingState(serverPlayer);
                return;
            }

            //调用自定义回血方法
            int durabilityTick = playerData.getInt(DURABILITY_CONSUME_TICK);
            //获取急救包启动时间和当前世界游戏时间
            long startTime = playerData.getLong(FIRST_AID_START_TIME);
            long currentGameTime = serverPlayer.level().getGameTime();
            //判断是否已过启动延迟时间（当前时间 >= 启动时间 + 延迟时间）
            boolean isDelayOver = currentGameTime >= startTime + START_DELAY_TICKS;

            //药包只有过了启动时间后才能启用
            if (isDelayOver && event.getEntity().tickCount % HEAL_INTERVAL == 0) {
                boolean healSuccess = PlayerCustomHealthManager.handleMedicineHeal(
                        serverPlayer,
                        PER_HEAL_AMOUNT,
                        MEDICINE_COOLDOWN,
                        DURABILITY_COST_PER_TIME
                );
            }

            // 耐久消耗逻辑
            durabilityTick++;
            if (durabilityTick >= DURABILITY_CONSUME_INTERVAL && !isDurabilityEmpty) {
                durabilityTick = 0;
            }
            playerData.putInt(DURABILITY_CONSUME_TICK, durabilityTick);
        }

        @SubscribeEvent
        public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                clearUsingState(serverPlayer);
            }
        }

        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
                clearUsingState(serverPlayer);
            }
        }

        // 公共方法：清除急救包使用状态
        public static void clearUsingState(ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            if (playerData.getBoolean(USING_FIRST_AID)) {
                playerData.putBoolean(USING_FIRST_AID, false);
                playerData.putInt(DURABILITY_CONSUME_TICK, 0);
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                //清除使用状态时，清空启动时间戳
                playerData.remove(FIRST_AID_START_TIME);
            }
        }

        private static ItemStack getHeldAidKit(ServerPlayer player) {
            if (player.getMainHandItem().getItem() instanceof Easy_Aid_Kit) {
                return player.getMainHandItem();
            }
            if (player.getOffhandItem().getItem() instanceof Easy_Aid_Kit) {
                return player.getOffhandItem();
            }
            return ItemStack.EMPTY;
        }
    }

    // 自定义物品名称
    @Override
    public Component getName(ItemStack stack) {
        int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        return Component.literal("简易急救包（剩余：" + remainingDurability + "/" + maxDurability + "）");
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        // 调用父类方法，保留默认提示（如耐久显示）
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        // 添加自定义Tooltip信息，按你的需求补充
        tooltipComponents.add(Component.literal("§7单次回血：§a" + PER_HEAL_AMOUNT + "点"));
        tooltipComponents.add(Component.literal("§7启动时间：§b" + (START_DELAY_TICKS / 20) + "秒"));
        tooltipComponents.add(Component.literal("§7一秒回血一次，消耗一点耐久"));
        tooltipComponents.add(Component.literal("§e单击右键使用")); // 黄色提示
        tooltipComponents.add(Component.literal("§7§o你知道吗，这个血包可以用来恢复体力"));
    }
}
