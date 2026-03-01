package com.mo.economy_system.item.items;

import com.mo.economy_system.core.storybook_system.StoryBookDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class Item_StoryBook extends Item {
    public Item_StoryBook(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            StoryBookDataManager.openStoryBook(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§7一切的见证。在你的续笔下，它仍将见证。"));
        tooltip.add(Component.literal("§8右键后可打开章节目录与片段排序界面"));
    }
}
