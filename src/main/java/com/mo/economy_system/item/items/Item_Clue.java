package com.mo.economy_system.item.items;

import com.mo.economy_system.core.clue_system.ClueData;
import com.mo.economy_system.core.clue_system.ClueDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.clue_system.Packet_OpenClueGUI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * 线索物品 - 荒野大镖客2风格的线索系统
 * 右键打开线索查看界面
 *
 * NBT数据结构:
 * - ClueItem: 包含线索ID
 *   - clueId: 线索ID（用于从ClueDataManager获取数据）
 *   - isRead: 是否已阅读
 */
public class Item_Clue extends Item {

    private static final String CLUE_ITEM_KEY = "ClueItem";
    private static final String CLUE_ID_KEY = "clueId";
    private static final String IS_READ_KEY = "isRead";

    private static Supplier<Item> clueItemSupplier;

    public Item_Clue(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /**
     * 设置线索物品的Supplier（用于避免循环引用）
     */
    public static void setClueItemSupplier(Supplier<Item> supplier) {
        clueItemSupplier = supplier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // 获取线索ID
        Integer clueId = getClueId(stack);
        if (clueId == null) {
            player.sendSystemMessage(Component.literal("§c无效的线索物品！"));
            return InteractionResultHolder.fail(stack);
        }

        // 从管理器获取线索数据
        ClueData clueData = ClueDataManager.getClue(clueId);
        if (clueData == null) {
            player.sendSystemMessage(Component.literal("§c线索数据不存在！ID: " + clueId));
            return InteractionResultHolder.fail(stack);
        }

        // 标记为已读
        markAsRead(stack);

        // 发送打开GUI的数据包
        EconomySystem_NetworkManager.sendToClient(
                new Packet_OpenClueGUI(clueData),
                (net.minecraft.server.level.ServerPlayer) player
        );
        return InteractionResultHolder.sidedSuccess(stack, true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        Integer clueId = getClueId(stack);
        boolean isRead = isRead(stack);

        if (clueId != null) {
            ClueData clueData = ClueDataManager.getClue(clueId);
            if (clueData != null) {
                tooltip.add(Component.literal(""));
                if (isRead) {
                    tooltip.add(Component.literal("§7已读过"));
                    tooltip.add(Component.literal("§8" + truncateString(clueData.getClueTitle(), 20)));
                } else {
                    tooltip.add(Component.literal("§e§l未阅读"));
                    tooltip.add(Component.literal("§f" + truncateString(clueData.getClueTitle(), 20)));
                }
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("§7阶段: §f" + clueData.getClueStage()));
                tooltip.add(Component.literal("§7作者: §f" + clueData.getClueAuthor()));
            } else {
                tooltip.add(Component.literal("§c无效的线索 ID: " + clueId));
            }
        } else {
            tooltip.add(Component.literal("§c未设置线索数据"));
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e右键点击查看线索"));
    }

    /**
     * 获取物品的线索ID
     */
    public static Integer getClueId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(CLUE_ITEM_KEY)) {
            CompoundTag clueTag = tag.getCompound(CLUE_ITEM_KEY);
            if (clueTag.contains(CLUE_ID_KEY)) {
                return clueTag.getInt(CLUE_ID_KEY);
            }
        }
        return null;
    }

    /**
     * 创建线索物品ItemStack
     */
    public static ItemStack createClueItem(int clueId) {
        if (clueItemSupplier == null) {
            throw new IllegalStateException("ClueItemSupplier not set. Call Item_Clue.setClueItemSupplier() first.");
        }
        ItemStack stack = new ItemStack(clueItemSupplier.get());
        setClueId(stack, clueId);
        return stack;
    }

    /**
     * 为ItemStack设置线索ID
     */
    public static void setClueId(ItemStack stack, int clueId) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag clueTag = new CompoundTag();
        clueTag.putInt(CLUE_ID_KEY, clueId);
        clueTag.putBoolean(IS_READ_KEY, false);
        tag.put(CLUE_ITEM_KEY, clueTag);
    }

    /**
     * 检查线索是否已读
     */
    public static boolean isRead(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(CLUE_ITEM_KEY)) {
            return tag.getCompound(CLUE_ITEM_KEY).getBoolean(IS_READ_KEY);
        }
        return false;
    }

    /**
     * 标记线索为已读
     */
    private static void markAsRead(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(CLUE_ITEM_KEY)) {
            return;
        }
        tag.getCompound(CLUE_ITEM_KEY).putBoolean(IS_READ_KEY, true);
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
