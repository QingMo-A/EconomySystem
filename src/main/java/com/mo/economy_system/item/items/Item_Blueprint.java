package com.mo.economy_system.item.items;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.item.item_renderer.BlueprintItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;


import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class Item_Blueprint extends Item {
    public Item_Blueprint(Properties props) {
        // 设置最大堆叠数为1
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 获取蓝图要解锁的物品ID
        String itemId = getUnlockedItemId(stack);

        // 检查是否是客户端（客户端和服务端都要处理）
        if (level.isClientSide()) {
            // 客户端逻辑（通常用于显示效果）
            return InteractionResultHolder.success(stack);
        } else {

            // 服务端逻辑（处理实际效果）
            if (itemId != null && !itemId.isEmpty()) {
                // 验证物品是否存在
                if (!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemId))) {
                    player.sendSystemMessage(Component.literal("§c蓝图指向不存在的物品！"));
                    return InteractionResultHolder.fail(stack);
                }

                // 检查是否已经解锁
                if (PlayerBlueprintData.canCraftItem(player, itemId)) {
                    player.sendSystemMessage(Component.literal("§c你已经可以制作这个物品了！"));
                    return InteractionResultHolder.fail(stack);
                }

                // 解锁物品制作权限
                PlayerBlueprintData.unlockItem(player, itemId);

                // 获取物品显示名称
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
                String itemName = item != null ?
                        new ItemStack(item).getHoverName().getString() : itemId;

                // 发送成功消息
                player.sendSystemMessage(
                        Component.literal("§a恭喜！你现在可以制作 §e" + itemName + " §a了！")
                );

                // 播放使用音效
                level.playSound(null, player.blockPosition(),
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.PLAYERS,
                        1.0F, 1.0F);

                // 消耗物品（如果不是创造模式）
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            } else {
                player.sendSystemMessage(Component.literal("§c这个蓝图是空白的！"));
                return InteractionResultHolder.fail(stack);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
    }

    /*@Override
    public InteractionResult useOn(UseOnContext context) {

        return InteractionResult.SUCCESS;
    }*/

    // ===================================================================

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        String itemId = getUnlockedItemId(stack);
        if (itemId != null && !itemId.isEmpty()) {
            // 获取物品并显示名称
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item != null) {
                ItemStack displayStack = new ItemStack(item);
                String itemName = displayStack.getHoverName().getString();

                tooltip.add(Component.literal("§7解锁物品: §f" + itemName));
                tooltip.add(Component.literal("§7物品ID: §8" + itemId));
            } else {
                tooltip.add(Component.literal("§7解锁物品: §c未知物品"));
            }

            tooltip.add(Component.literal("§8右键点击学习制作这个物品"));
            tooltip.add(Component.literal("§8使用后消失"));
        } else {
            tooltip.add(Component.literal("§7空白的蓝图"));
            tooltip.add(Component.literal("§8无法使用"));
        }
    }

    // 创建蓝图
    public static ItemStack createBlueprint(String itemId) {
        ItemStack stack = new ItemStack(EconomySystem_Items.BLUEPRINT_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("unlocks_item", itemId);
        com.mo.economy_system.utils.ItemStackDataHelper.setTag(stack, tag);
        return stack;
    }

    // 获取蓝图中要解锁的物品ID
    public static String getUnlockedItemId(ItemStack stack) {
        if (com.mo.economy_system.utils.ItemStackDataHelper.hasTag(stack) && com.mo.economy_system.utils.ItemStackDataHelper.getTag(stack).contains("unlocks_item")) {
            return com.mo.economy_system.utils.ItemStackDataHelper.getTag(stack).getString("unlocks_item");
        }
        return null;
    }

    // 设置要解锁的物品ID到蓝图中
    public static void setUnlockedItemId(ItemStack stack, String itemId) {
        CompoundTag tag = com.mo.economy_system.utils.ItemStackDataHelper.getTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putString("unlocks_item", itemId);

        // 可选：添加一些额外信息
        tag.putString("blueprint_name", "制作图纸");
        com.mo.economy_system.utils.ItemStackDataHelper.setTag(stack, tag);
        // tag.putInt("tier", getTierForItem(itemId));
    }

    // 自定义渲染器
    private static BlueprintItemRenderer renderer = null;

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new BlueprintItemRenderer();
                }
                return renderer;
            }
        });
    }
}
