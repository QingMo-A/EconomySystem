package com.mo.economy_system.commands.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.check_system.Packet_Check;
import com.mo.economy_system.network.packets.check_system.Packet_Get;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 *  这是一个 查询 / 获取指定玩家 Mods / Shaderpacks / Resourcepacks 文件夹下文件数据的指令
 */
public class Command_Check {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 父指令
        dispatcher.register(Commands.literal("check")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限
                // 第一个参数 -- 玩家
                .then(Commands.argument("playerName", EntityArgument.player())
                        // 第二个参数 -- 要查询的类型 ( Mods / Shaderpacks / resourcepacks )
                        .then(Commands.literal("mods")
                                .executes(context -> {
                                    // 获取 玩家
                                    ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                    return checkPlayer(context.getSource(), player, "mods");
                                }))
                        .then(Commands.literal("shaderpacks")
                                .executes(context -> {
                                    // 获取 玩家
                                    ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                    return checkPlayer(context.getSource(), player, "shaderpacks");
                                }))
                        .then(Commands.literal("resourcepacks")
                                .executes(context -> {
                                    // 获取 玩家
                                    ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                    return checkPlayer(context.getSource(), player, "resourcepacks");
                                }))
                )
        );
        // 父指令
        dispatcher.register(Commands.literal("get")
                .requires(source -> source.hasPermission(2))
                // 第一个参数 -- 玩家
                .then(Commands.argument("playerName", EntityArgument.player())
                        // 第二个参数 -- 要获取的文件名
                        .then(Commands.argument("fileName", StringArgumentType.string())
                                // 第三个参数 -- 要获取的类型 ( Mods / Shaderpacks / resourcepacks )
                                .then(Commands.literal("mods")
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                            String fileName = StringArgumentType.getString(context, "fileName");
                                            return getPlayerFile(context.getSource(), player, "mods", fileName);
                                        }))
                                .then(Commands.literal("shaderpacks")
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                            String fileName = StringArgumentType.getString(context, "fileName");
                                            return getPlayerFile(context.getSource(), player, "shaderpacks", fileName);
                                        }))
                                .then(Commands.literal("resourcepacks")
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "playerName");
                                            String fileName = StringArgumentType.getString(context, "fileName");
                                            return getPlayerFile(context.getSource(), player, "resourcepacks", fileName);
                                        }))
                        )
                )
        );
    }

    /**
     * 检查玩家指定路径的指令
     *
     * @param source 指令源
     * @param player ServerPlayer的变量
     * @param type 操作种类
     */
    private static int checkPlayer(CommandSourceStack source, ServerPlayer player, String type) {
        String playerName = player.getName().getString();
        UUID playerUUID = player.getUUID();

        ServerPlayer sender = source.getPlayer();
        String senderName = sender.getName().getString();
        UUID senderUUID = sender.getUUID();

        if (player == null) {
            source.sendFailure(Component.literal("Player not found!"));
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("检查请求已发送至 " + playerName), false);

        // 向目标玩家发送一个网络包
        EconomySystem_NetworkManager.sendToClient(player,
                new Packet_Check(playerName, String.valueOf(playerUUID), senderName, String.valueOf(senderUUID), type));


        return Command.SINGLE_SUCCESS;
    }

    /**
     * 获取玩家本地文件的指令
     *
     * @param source 指令源
     * @param player ServerPlayer的变量
     * @param type 操作种类
     * @param fileName 文件名
     */
    private static int getPlayerFile(CommandSourceStack source, ServerPlayer player, String type, String fileName) {
        String playerName = player.getName().getString();
        UUID playerUUID = player.getUUID();

        ServerPlayer sender = source.getPlayer();
        String senderName = sender.getName().getString();
        UUID senderUUID = sender.getUUID();

        if (player == null) {
            source.sendFailure(Component.literal("Player not found!"));
            return Command.SINGLE_SUCCESS;
        }

        source.sendSuccess(() -> Component.literal("获取请求已发送至 " + playerName), false);

        // 向目标玩家发送一个网络包
        EconomySystem_NetworkManager.sendToClient(player,
                new Packet_Get(playerName, String.valueOf(playerUUID), senderName, String.valueOf(senderUUID), type, fileName));


        return Command.SINGLE_SUCCESS;
    }
}

