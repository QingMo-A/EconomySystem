package com.mo.economy_system.core.auth_system;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Command_Register {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.string())
                        .then(Commands.argument("confirmPassword", StringArgumentType.string())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String password = StringArgumentType.getString(context, "password");
                                    String confirmPassword = StringArgumentType.getString(context, "confirmPassword");

                                    if (password.length() < 4) {
                                        player.sendSystemMessage(Component.literal("§c密码长度至少需要4个字符！"));
                                        return 0;
                                    }

                                    if (!password.equals(confirmPassword)) {
                                        player.sendSystemMessage(Component.literal("§c两次输入的密码不一致！"));
                                        return 0;
                                    }

                                    AuthSavedData data = AuthSavedData.getInstance(player.serverLevel());
                                    if (data.isRegistered(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal("§c你已经注册过了！请使用 /login <密码> 登录。"));
                                        return 0;
                                    }

                                    if (data.register(player.getUUID(), password)) {
                                        player.sendSystemMessage(Component.literal("§a注册成功！请使用 /login <密码> 登录。"));
                                        return 1;
                                    } else {
                                        player.sendSystemMessage(Component.literal("§c注册失败！"));
                                        return 0;
                                    }
                                }))));
    }
}
