package com.mo.economy_system.item.items;

import com.mo.economy_system.core.storybook_system.FragmentData;
import com.mo.economy_system.core.storybook_system.StoryBookDataManager;
import com.mo.economy_system.item.EconomySystem_Items;
import net.minecraft.nbt.CompoundTag;
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

public class Item_FragmentPage extends Item {
    private static final String FRAGMENT_PAGE_TAG = "FragmentPage";
    private static final String FRAGMENT_ID_KEY = "fragmentId";

    public Item_FragmentPage(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            boolean used = StoryBookDataManager.useFragmentPage(serverPlayer, getFragmentId(stack));
            if (used && !player.isCreative()) {
                stack.shrink(1);
            }
            return used ? InteractionResultHolder.sidedSuccess(stack, false) : InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("§7一张残破的纸。需要拼成完整的才能真正读懂。"));
        tooltip.add(Component.literal("§8右键后获得§f随记本§8，并直接阅读这次整理出的内容"));

        Integer fragmentId = getFragmentId(stack);
        if (fragmentId != null) {
            FragmentData fragmentData = StoryBookDataManager.getFragment(fragmentId);
            if (fragmentData != null) {
                tooltip.add(Component.literal("§7编号: §f" + fragmentId));
                tooltip.add(Component.literal("§7标题: §f" + fragmentData.getTitle()));
            } else {
                tooltip.add(Component.literal("§c无效编号: §f" + fragmentId));
            }
        } else {
            tooltip.add(Component.literal("§c未绑定片段编号，无法解锁内容"));
        }
    }

    public static ItemStack createFragmentPage(int fragmentId) {
        ItemStack stack = new ItemStack(EconomySystem_Items.FRAGMENT_PAGE.get());
        setFragmentId(stack, fragmentId);
        return stack;
    }

    public static void setFragmentId(ItemStack stack, int fragmentId) {
        CompoundTag rootTag = com.mo.economy_system.utils.ItemStackCompat.getTag(stack);
        if (rootTag == null) {
            rootTag = new CompoundTag();
        }
        CompoundTag fragmentPageTag = rootTag.getCompound(FRAGMENT_PAGE_TAG);
        fragmentPageTag.putInt(FRAGMENT_ID_KEY, fragmentId);
        rootTag.put(FRAGMENT_PAGE_TAG, fragmentPageTag);
        com.mo.economy_system.utils.ItemStackCompat.setTag(stack, rootTag);
    }

    public static Integer getFragmentId(ItemStack stack) {
        if (!com.mo.economy_system.utils.ItemStackCompat.hasTag(stack)) {
            return null;
        }

        CompoundTag rootTag = com.mo.economy_system.utils.ItemStackCompat.getTag(stack);
        if (rootTag == null || !rootTag.contains(FRAGMENT_PAGE_TAG)) {
            return null;
        }

        CompoundTag fragmentPageTag = rootTag.getCompound(FRAGMENT_PAGE_TAG);
        if (!fragmentPageTag.contains(FRAGMENT_ID_KEY)) {
            return null;
        }

        int fragmentId = fragmentPageTag.getInt(FRAGMENT_ID_KEY);
        return fragmentId > 0 ? fragmentId : null;
    }
}
