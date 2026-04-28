package com.mo.economy_system.commands.economy_system;

import com.mo.economy_system.core.economy_system.red_packet.RedPacket;
import com.mo.economy_system.core.economy_system.red_packet.RedPacketManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

public class Command_RedPacket {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("redpacket")
                .then(Commands.literal("create")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                        .then(Commands.literal("lucky")
                                                .executes(context -> createRedPacket(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"), IntegerArgumentType.getInteger(context, "duration"), true)))
                                        .then(Commands.literal("even")
                                                .executes(context -> createRedPacket(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"), IntegerArgumentType.getInteger(context, "duration"), false))))))
                .then(Commands.literal("claim")
                        .executes(context -> claimRedPacket(context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> claimRedPacket(context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("cancel")
                        .executes(context -> cancelRedPacket(context.getSource().getPlayerOrException()))));
    }


    private static int cancelRedPacket(ServerPlayer sender) {
        // 获取当前玩家的红包
        RedPacket redPacket = RedPacketManager.getRedPacketBySender(sender.getUUID());

        if (redPacket == null) {
            sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_NO_ACTIVE)); // 没有可取消的红包
            return 0;
        }

        // 计算未被领取的金额
        int remainingAmount = redPacket.totalAmount - redPacket.claimedAmount;

        // 将未被领取的金额返还给玩家
        if (remainingAmount > 0) {
            EconomySavedData data = EconomySavedData.getInstance(sender.serverLevel());
            data.addBalance(sender.getUUID(), remainingAmount);
        }

        // 从红包管理器中移除该红包
        RedPacketManager.removeRedPacket(sender.getUUID());

        // 通知发送者红包已取消
        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_CANCELLED, remainingAmount));

        /*// 广播给其他玩家
        sender.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§c" + sender.getName().getString() + "取消了自己的红包！"), false);*/

        return 1;
    }


    private static int createRedPacket(ServerPlayer sender, int amount, int duration, boolean isLucky) {
        EconomySavedData data = EconomySavedData.getInstance(sender.serverLevel());

        if (data.getBalance(sender.getUUID()) < amount) {
            sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_INSUFFICIENT_BALANCE));
            return 0;
        }

        int totalCount = sender.server.getPlayerCount();

        if (!RedPacketManager.addRedPacket(sender.getUUID(), new RedPacket(sender.getUUID(), sender.getName().getString(), amount, isLucky, duration, totalCount))) {
            sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_ALREADY_ACTIVE));
            return 0;
        }

        data.minBalance(sender.getUUID(), amount);

        Component claimButton = Component.translatable(Util_MessageKeys.RED_PACKET_CLAIM_BUTTON)
                .withStyle(style -> style
                        .withColor(0x55FF55)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/redpacket claim " + sender.getName().getString()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§eClick to claim!"))));

        sender.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable(Util_MessageKeys.RED_PACKET_BROADCAST, sender.getName().getString()).append(claimButton), false);

        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_CREATED_SUCCESSFULLY));
        return 1;
    }

    private static int claimRedPacket(ServerPlayer player) {
        return claimRedPacket(player, null);
    }

    private static int claimRedPacket(ServerPlayer player, ServerPlayer sender) {
        RedPacket redPacket;
        if (sender == null) {
            redPacket = RedPacketManager.getRecentRedPacket();
        } else {
            redPacket = RedPacketManager.getRedPacketBySender(sender.getUUID());
        }

        // 检查红包是否可领取
        if (redPacket == null || !redPacket.isClaimable()) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_NO_AVAILABLE));
            return 0;
        }

        // 检查是否已经领取过
        if (redPacket.claimedPlayers.contains(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_ALREADY_CLAIMED));
            return 0;
        }

        // 动态计算剩余的玩家数
        int totalPlayers = redPacket.totalCount; // 红包总人数（可以在红包生成时记录总人数）
        int remainingPlayers = totalPlayers - redPacket.claimedPlayers.size();
        int remainingAmount = redPacket.totalAmount - redPacket.claimedAmount;

        // 检查是否已经分配完毕
        if (remainingPlayers <= 0 || remainingAmount <= 0) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_NO_AVAILABLE));
            return 0;
        }

        int amount;
        if (remainingPlayers == 1) {
            // 最后一人领取剩余金额
            amount = remainingAmount;
        } else if (redPacket.isLucky) {
            // 拼手气红包：加权随机分配
            amount = Math.max(1, new Random().nextInt(remainingAmount - (remainingPlayers - 1)) + 1);
        } else {
            // 普通红包：平均分配
            amount = Math.max(1, remainingAmount / remainingPlayers);
        }

        // 更新红包状态
        redPacket.claimedAmount += amount;
        redPacket.claimedPlayers.add(player.getUUID());

        // 更新玩家余额
        EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
        data.addBalance(player.getUUID(), amount);

        // 通知领取者
        player.sendSystemMessage(Component.translatable(Util_MessageKeys.RED_PACKET_CLAIM_SUCCESS, redPacket.senderName, amount));

        // 广播信息
        broadcastClaimMessage(player, redPacket.senderName, amount);

        // 检查红包是否已被领完
        if (redPacket.claimedAmount >= redPacket.totalAmount) {
            // 广播红包已被领完
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable(Util_MessageKeys.RED_PACKET_FULLY_CLAIMED, redPacket.senderName), false);

            // 从管理器中移除红包
            RedPacketManager.removeRedPacket(sender.getUUID());
        }

        return 1;
    }



    private static void broadcastClaimMessage(ServerPlayer claimer, String senderName, int amount) {
        String claimerName = claimer.getName().getString();

        // 构建消息内容
        Component message = Component.translatable(Util_MessageKeys.RED_PACKET_CLAIM_BROADCAST, claimerName, senderName, amount);

        // 向所有其他玩家发送消息
        claimer.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (!player.getUUID().equals(claimer.getUUID())) { // 排除抢红包的玩家
                player.sendSystemMessage(message);
            }
        });
    }

}
