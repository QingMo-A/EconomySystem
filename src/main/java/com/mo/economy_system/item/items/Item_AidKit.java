package com.mo.economy_system.item.items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class Item_AidKit extends Item {
    // 状态标记 - 改为基于物品ID的唯一标记，避免多个急救包冲突
    private final String USING_KEY;
    private final String DURABILITY_TICK_KEY;
    private final String START_TIME_KEY;

    // 配置参数
    private final int HEAL_INTERVAL;
    private final double PER_HEAL_AMOUNT;
    private final int DURABILITY_CONSUME_INTERVAL;
    private final long MEDICINE_COOLDOWN;
    private final int START_DELAY_TICKS;
    private final int DURABILITY_COST_PER_TIME = 1;

    // 用于显示的物品名称（不同等级）
    private final String displayName;

    public Item_AidKit(int healInterval, double perHealAmount, int durabilityConsumeInterval,
                       long cooldown, int startDelay, String displayName) {
        super(new Item.Properties().stacksTo(1).durability(10));
        HEAL_INTERVAL = healInterval;
        PER_HEAL_AMOUNT = perHealAmount;
        DURABILITY_CONSUME_INTERVAL = durabilityConsumeInterval;
        MEDICINE_COOLDOWN = cooldown;
        START_DELAY_TICKS = startDelay;
        this.displayName = displayName;

        // 为每个急救包类型生成唯一的NBT键名
        String itemKey = displayName.toLowerCase().replace(" ", "_").replace("（", "").replace("）", "");
        USING_KEY = "Using_" + itemKey;
        DURABILITY_TICK_KEY = "DurabilityTick_" + itemKey;
        START_TIME_KEY = "StartTime_" + itemKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack heldItemStack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            // 客户端播放动画
            player.swing(hand);
            return InteractionResultHolder.sidedSuccess(heldItemStack, true);
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        CompoundTag playerData = serverPlayer.getPersistentData();

        // 使用动态生成的键名
        boolean isUsing = playerData.getBoolean(USING_KEY);

        if (!isUsing) {
            // 检查耐久和生命值
            boolean hasDurability = heldItemStack.getDamageValue() < heldItemStack.getMaxDamage();
            boolean needHeal = player.getHealth() < player.getMaxHealth();

            if (hasDurability && needHeal) {
                // 设置使用状态
                playerData.putBoolean(USING_KEY, true);
                playerData.putInt(DURABILITY_TICK_KEY, 0);
                playerData.remove(START_TIME_KEY);
                playerData.putLong(START_TIME_KEY, serverPlayer.level().getGameTime());

                serverPlayer.sendSystemMessage(Component.literal("§b正在使用" + displayName + "§b..."), true);
                return InteractionResultHolder.success(heldItemStack);
            } else if (!needHeal) {
                serverPlayer.sendSystemMessage(Component.literal("§a你的生命值已满，无需使用急救包！"), true);
            } else if (!hasDurability) {
                serverPlayer.sendSystemMessage(Component.literal("§c急救包已损坏，无法使用！"), true);
            }
        } else {
            // 手动停止使用
            clearUsingState(serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("§c已手动停止使用急救包！"), true);
            return InteractionResultHolder.success(heldItemStack);
        }

        return InteractionResultHolder.pass(heldItemStack);
    }

    /**
     * 清除使用状态
     */
    public void clearUsingState(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        if (playerData.getBoolean(USING_KEY)) {
            playerData.putBoolean(USING_KEY, false);
            playerData.putInt(DURABILITY_TICK_KEY, 0);
            playerData.remove(START_TIME_KEY);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    /**
     * 检查玩家是否正在使用这个急救包
     */
    public boolean isPlayerUsing(ServerPlayer player) {
        return player.getPersistentData().getBoolean(USING_KEY);
    }

    // 提供getter方法给事件处理器使用
    public int getHealInterval() { return HEAL_INTERVAL; }
    public double getPerHealAmount() { return PER_HEAL_AMOUNT; }
    public int getDurabilityConsumeInterval() { return DURABILITY_CONSUME_INTERVAL; }
    public long getMedicineCooldown() { return MEDICINE_COOLDOWN; }
    public int getStartDelayTicks() { return START_DELAY_TICKS; }
    public String getUsingKey() { return USING_KEY; }
    public String getDurabilityTickKey() { return DURABILITY_TICK_KEY; }
    public String getStartTimeKey() { return START_TIME_KEY; }

    /**
     * 获取玩家手持的急救包（任何类型的Item_AidKit）
     */
    public static ItemStack getHeldAidKit(ServerPlayer player) {
        // 检查主手
        if (player.getMainHandItem().getItem() instanceof Item_AidKit) {
            return player.getMainHandItem();
        }
        // 检查副手
        if (player.getOffhandItem().getItem() instanceof Item_AidKit) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getName(ItemStack stack) {
        int remainingDurability = stack.getMaxDamage() - stack.getDamageValue();
        int maxDurability = stack.getMaxDamage();
        return Component.literal(displayName + "（剩余：" + remainingDurability + "/" + maxDurability + "）");
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<Component> tooltipComponents,
                                net.minecraft.world.item.TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        tooltipComponents.add(Component.literal("§7等级: " + getTierName()));
        tooltipComponents.add(Component.literal("§7单次治疗: §a" + PER_HEAL_AMOUNT + "§7点"));
        tooltipComponents.add(Component.literal("§7治疗间隔: §e" + (HEAL_INTERVAL / 20.0) + "§7秒"));
        tooltipComponents.add(Component.literal("§7启动延迟: §b" + (START_DELAY_TICKS / 20.0) + "§7秒"));
        tooltipComponents.add(Component.literal("§7冷却时间: §6" + (MEDICINE_COOLDOWN / 20.0) + "§7秒"));
        tooltipComponents.add(Component.literal("§8右键开始/停止使用"));
    }

    private String getTierName() {
        if (displayName.contains("简易")) return "§a简易";
        if (displayName.contains("高级")) return "§6高级";
        if (displayName.contains("专业")) return "§5专业";
        return "§f普通";
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}