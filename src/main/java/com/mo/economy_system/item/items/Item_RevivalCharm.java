package com.mo.economy_system.item.items;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_OpenRevivalCharmGUI;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 复活护符 - 右键打开 GUI，可以输入被封禁玩家名称进行复活
 */
public class Item_RevivalCharm extends Item {

    public Item_RevivalCharm(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // 客户端：打开 GUI
            return InteractionResultHolder.success(stack);
        } else {
            // 服务端：发送数据包让客户端打开 GUI
            EconomySystem_NetworkManager.sendToClient(new Packet_OpenRevivalCharmGUI(), (net.minecraft.server.level.ServerPlayer) player);
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                                java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.literal("§7§o一件神秘的护符，蕴含着复活的魔法"));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§e右键点击使用"));
        tooltip.add(Component.literal("§7输入被封禁玩家的名称来复活他们"));
    }
}
