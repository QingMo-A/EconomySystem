package com.mo.economy_system.commands.task_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.task_system.TaskDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Task {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 根指令 /task，创建任务需要管理员权限（2），完成任务测试可放宽权限
        dispatcher.register(
                Commands.literal("task")
                        .requires(source -> source.hasPermission(2)) // 管理员权限
                        // 子指令：创建服务器任务 /task create server <任务名> <任务内容> <结束时间(秒)>
                        .then(Commands.literal("create")
                                .then(Commands.literal("server")
                                        .then(Commands.argument("taskName", StringArgumentType.string())
                                                .then(Commands.argument("taskContent", StringArgumentType.string())
                                                        .then(Commands.argument("endTimeSec", IntegerArgumentType.integer(1))
                                                                .executes(Command_Task::executeCreateServerTask)
                                                        )
                                                )
                                        )
                                )
                                // 子指令：创建通用玩家任务 /task create player common <任务名> <任务内容> <结束时间(秒)>
                                .then(Commands.literal("player")
                                        .then(Commands.literal("common")
                                                .then(Commands.argument("taskName", StringArgumentType.string())
                                                        .then(Commands.argument("taskContent", StringArgumentType.string())
                                                                .then(Commands.argument("endTimeSec", IntegerArgumentType.integer(1))
                                                                        .executes(Command_Task::executeCreateCommonPlayerTask)
                                                                )
                                                        )
                                                )
                                        )
                                        // 子指令：创建专属玩家任务 /task create player exclusive <玩家> <任务名> <任务内容> <结束时间(秒)>
                                        .then(Commands.literal("exclusive")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .then(Commands.argument("taskName", StringArgumentType.string())
                                                                .then(Commands.argument("taskContent", StringArgumentType.string())
                                                                        .then(Commands.argument("endTimeSec", IntegerArgumentType.integer(1))
                                                                                .executes(Command_Task::executeCreateExclusivePlayerTask)
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        // 子指令：标记完成任务 /task complete player <任务ID> <玩家>
                        .then(Commands.literal("complete")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("taskId", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(Command_Task::executeCompletePlayerTask)
                                                )
                                        )
                                )
                                // 子指令：标记完成服务器任务 /task complete server <任务ID> <玩家>
                                .then(Commands.literal("server")
                                        .then(Commands.argument("taskId", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(Command_Task::executeCompleteServerTask)
                                                )
                                        )
                                )
                        )
        );
    }

    // 执行创建服务器任务
    private static int executeCreateServerTask(CommandContext<CommandSourceStack> context) {
        String taskName = StringArgumentType.getString(context, "taskName");
        String taskContent = StringArgumentType.getString(context, "taskContent");
        int endTimeSec = IntegerArgumentType.getInteger(context, "endTimeSec");
        // 转换为毫秒：当前时间 + 秒数*1000
        long endTime = System.currentTimeMillis() + (long) endTimeSec * 1000;

        TaskDataManager.createServerTask(taskName, taskContent, endTime);
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("成功创建服务器任务：" + taskName + "，结束时间：" + endTimeSec + "秒后"),
                true
        );
        return 1;
    }

    // 执行创建通用玩家任务
    private static int executeCreateCommonPlayerTask(CommandContext<CommandSourceStack> context) {
        String taskName = StringArgumentType.getString(context, "taskName");
        String taskContent = StringArgumentType.getString(context, "taskContent");
        int endTimeSec = IntegerArgumentType.getInteger(context, "endTimeSec");
        long endTime = System.currentTimeMillis() + (long) endTimeSec * 1000;

        TaskDataManager.createPlayerTask(taskName, taskContent, endTime);
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("成功创建通用玩家任务：" + taskName + "，结束时间：" + endTimeSec + "秒后"),
                true
        );
        return 1;
    }

    // 执行创建专属玩家任务
    private static int executeCreateExclusivePlayerTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        String taskName = StringArgumentType.getString(context, "taskName");
        String taskContent = StringArgumentType.getString(context, "taskContent");
        int endTimeSec = IntegerArgumentType.getInteger(context, "endTimeSec");
        long endTime = System.currentTimeMillis() + (long) endTimeSec * 1000;

        TaskDataManager.createOnlyOnePlayerTask(taskName, taskContent, endTime, targetPlayer.getName().getString(), targetPlayer.getUUID());
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("成功为玩家 " + targetPlayer.getName().getString() + " 创建专属任务：" + taskName),
                true
        );
        return 1;
    }

    // 执行标记完成个人任务
    private static int executeCompletePlayerTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int taskId = IntegerArgumentType.getInteger(context, "taskId");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

        TaskDataManager.playerCompleteOwnTask(taskId, targetPlayer.getName().getString(), targetPlayer.getUUID());
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("已标记玩家 " + targetPlayer.getName().getString() + " 完成个人任务ID：" + taskId),
                true
        );
        return 1;
    }

    // 执行标记完成服务器任务
    private static int executeCompleteServerTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int taskId = IntegerArgumentType.getInteger(context, "taskId");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

        TaskDataManager.playerCompleteServerTask(taskId, targetPlayer.getName().getString(), targetPlayer.getUUID());
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("已标记玩家 " + targetPlayer.getName().getString() + " 完成服务器任务ID：" + taskId),
                true
        );
        return 1;
    }
}