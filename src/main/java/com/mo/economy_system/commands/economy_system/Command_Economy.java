package com.mo.economy_system.commands.economy_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.armor.armors.SupporterHat;
import com.mo.economy_system.core.economy_system.shop.ShopPricingConfig;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.settings.GameSettingsManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.Map;

public class Command_Economy {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("economy_system")
                .then(Commands.literal("shop")
                        .then(Commands.literal("addhand")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("basePrice", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    int basePrice = IntegerArgumentType.getInteger(context, "basePrice");
                                                    String description = StringArgumentType.getString(context, "description");
                                                    ItemStack handStack = player.getMainHandItem();
                                                    if (handStack.isEmpty()) {
                                                        context.getSource().sendFailure(Component.literal("请先将要添加的商品拿在主手"));
                                                        return 0;
                                                    }

                                                    ItemStack savedStack = handStack.copy();
                                                    savedStack.setCount(1);
                                                    ShopItem shopItem = EconomySystem.SHOP_MANAGER.addItemFromStack(
                                                            savedStack,
                                                            basePrice,
                                                            description,
                                                            player.serverLevel().registryAccess()
                                                    );
                                                    context.getSource().sendSuccess(() -> Component.literal(
                                                            "已将 " + savedStack.getHoverName().getString()
                                                                    + " 添加到系统商店，基础价格：" + shopItem.getBasePrice()
                                                                    + " 梦鱼币，描述：" + shopItem.getDescription()
                                                    ), false);
                                                    return 1;
                                                }))))));

        dispatcher.register(Commands.literal("economy_system")
                .then(Commands.literal("supporter_hat")
                        .then(Commands.literal("bind")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("supporter", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer executor = context.getSource().getPlayerOrException();
                                            ServerPlayer supporter = EntityArgument.getPlayer(context, "supporter");
                                            return bindSupporterHat(context.getSource(), executor, supporter.getUUID(), supporter.getGameProfile().getName());
                                        }))
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer executor = context.getSource().getPlayerOrException();
                                                    UUID uuid = UuidArgument.getUuid(context, "uuid");
                                                    String name = StringArgumentType.getString(context, "name");
                                                    return bindSupporterHat(context.getSource(), executor, uuid, name);
                                                }))))));

        dispatcher.register(Commands.literal("economy_system")
                .then(Commands.literal("settings")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    Map<String, String> settings = GameSettingsManager.getAll();
                                    context.getSource().sendSuccess(() -> Component.literal("EconomySystem 设置项:"), false);
                                    for (Map.Entry<String, String> entry : settings.entrySet()) {
                                        String description = GameSettingsManager.description(entry.getKey());
                                        context.getSource().sendSuccess(() -> Component.literal(
                                                entry.getKey() + " = " + entry.getValue() + (description.isBlank() ? "" : " | " + description)
                                        ), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("get")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .executes(context -> {
                                            String key = StringArgumentType.getString(context, "key");
                                            String value = GameSettingsManager.get(key);
                                            if (value == null) {
                                                context.getSource().sendFailure(Component.literal("未知设置项: " + key));
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal(key + " = " + value), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.word())
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    String value = StringArgumentType.getString(context, "value");
                                                    try {
                                                        if (!GameSettingsManager.set(key, value)) {
                                                            context.getSource().sendFailure(Component.literal("未知设置项: " + key));
                                                            return 0;
                                                        }
                                                        context.getSource().sendSuccess(() -> Component.literal("已设置 " + key + " = " + GameSettingsManager.get(key)), false);
                                                        return 1;
                                                    } catch (IllegalArgumentException e) {
                                                        context.getSource().sendFailure(Component.literal("设置失败: " + e.getMessage()));
                                                        return 0;
                                                    }
                                                }))))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    GameSettingsManager.load();
                                    ShopPricingConfig.reload();
                                    EconomySystem.SHOP_MANAGER.reloadPricingConfig();
                                    context.getSource().sendSuccess(() -> Component.literal("已重载 EconomySystem 设置"), false);
                                    return 1;
                                }))));

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
                                            data.addBalance(player.getUUID(), amount, "指令", "管理员增加余额");
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
                                    if (data.minBalance(player.getUUID(), amount, "指令", "管理员减少余额")) {
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
                                    data.setBalance(player.getUUID(), amount, "指令", "管理员设置余额");
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

                                                    if (target != null && !target.getUUID().equals(sender.getUUID()) && data.minBalance(sender.getUUID(), amount, "转账", "赠与 " + target.getName().getString())) {
                                                        data.addBalance(target.getUUID(), amount, "转账", "来自 " + sender.getName().getString() + " 的赠与");
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

    private static int bindSupporterHat(CommandSourceStack source, ServerPlayer executor, UUID supporterUuid, String supporterName) {
        ItemStack stack = executor.getMainHandItem();
        if (!(stack.getItem() instanceof SupporterHat)) {
            source.sendFailure(Component.literal("请先将赞助者帽子拿在主手"));
            return 0;
        }
        SupporterHat.setSupporter(stack, supporterUuid, supporterName);
        source.sendSuccess(() -> Component.literal("已将赞助者帽子绑定到 " + supporterName + " (" + supporterUuid + ")"), false);
        return 1;
    }
}
