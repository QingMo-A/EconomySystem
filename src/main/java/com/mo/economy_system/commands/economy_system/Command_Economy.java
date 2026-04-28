package com.mo.economy_system.commands.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class Command_Economy {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("coin")
                // 查询余额
                .then(Commands.literal("balance")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ServerLevel serverLevel = player.serverLevel(); // 获取服务器世界实例
                            EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                            int balance = data.getBalance(player.getUUID());
                            context.getSource().sendSuccess(() -> Component.translatable(Util_MessageKeys.COIN_COMMAND_BALANCE, balance), false);
                            return 1;
                        }))
                // 增加余额
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2)) // 设置需要权限等级 2
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            ServerLevel serverLevel = player.serverLevel(); // 获取服务器世界实例
                                            EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                                            data.addBalance(player.getUUID(), amount);
                                            context.getSource().sendSuccess(() -> Component.translatable(Util_MessageKeys.COIN_COMMAND_ADD, amount), false);
                                            return 1;
                                        }))))
                // 减少余额
                .then(Commands.literal("min")
                        .requires(source -> source.hasPermission(2)) // 设置需要权限等级 2
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ServerLevel serverLevel = player.serverLevel(); // 获取服务器世界实例
                                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                                    if (data.minBalance(player.getUUID(), amount)) {
                                        context.getSource().sendSuccess(() -> Component.translatable(Util_MessageKeys.COIN_COMMAND_MIN, amount), false);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable(Util_MessageKeys.COIN_COMMAND_INSUFFICIENT_BALANCE));
                                    }
                                    return 1;
                                }))))
                // 设置余额
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2)) // 设置需要权限等级 2
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ServerLevel serverLevel = player.serverLevel(); // 获取服务器世界实例
                                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                                    data.setBalance(player.getUUID(), amount);
                                    context.getSource().sendSuccess(() -> Component.translatable(Util_MessageKeys.COIN_COMMAND_SET, amount), false);
                                    return 1;
                                }))))
                // 转账功能
                .then(Commands.literal("transfer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer receiver = EntityArgument.getPlayer(context, "target");
                                            UUID receiverUUID = receiver.getUUID();
                                            ServerPlayer sender = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            if (sender != null) {
                                                ServerLevel serverLevel = sender.serverLevel(); // 使用 sender.serverLevel() 获取 ServerLevel
                                                if (serverLevel != null) {
                                                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                                                    Player target = serverLevel.getPlayerByUUID(receiverUUID); // 根据 UUID 获取目标玩家

                                                    if (target != null && data.minBalance(sender.getUUID(), amount) && target.getUUID() != sender.getUUID()) {
                                                        data.addBalance(target.getUUID(), amount);
                                                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TRANSFER_SUCCESSFULLY_MESSAGE_KEY, amount, target.getName().getString()));
                                                        target.sendSystemMessage(Component.translatable(Util_MessageKeys.RECEIVE_SUCCESSFULLY_MESSAGE_KEY, sender.getName().getString(), amount));
                                                    } else {
                                                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TRANSFER_FAILED_MESSAGE_KEY));
                                                    }
                                                }
                                            }
                                            // EconomyNetwork.INSTANCE.sendToServer(new TransferPacket(receiver.getUUID(), amount));
                                            return 1;
                                        }))))
        );
    }
}
