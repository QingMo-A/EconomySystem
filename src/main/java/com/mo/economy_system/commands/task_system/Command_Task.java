package com.mo.economy_system.commands.task_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.story_system.StoryStageManager;
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
                        // 子指令：查询统计信息 /task stats ...
                        .then(Commands.literal("stats")
                                // 查询全服总统计 /task stats total
                                .then(Commands.literal("total")
                                        .executes(Command_Task::executeStatsTotal)
                                )
                                // 查询阶段统计 /task stats stage <阶段ID>
                                .then(Commands.literal("stage")
                                        .then(Commands.argument("stageId", IntegerArgumentType.integer(1))
                                                .executes(Command_Task::executeStatsStage)
                                        )
                                )
                                // 查询任务统计 /task stats task <任务ID>
                                .then(Commands.literal("task")
                                        .then(Commands.argument("taskId", IntegerArgumentType.integer(1))
                                                .executes(Command_Task::executeStatsTask)
                                        )
                                )
                        )
        );
    }

    // 执行创建服务器任务
    private static int executeCreateServerTask(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(
                net.minecraft.network.chat.Component.literal("服务器任务已改为故事阶段系统，请通过配置文件 config/economy_system/story_stage_data.json 来管理任务")
        );
        return 0;
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

    // 执行标记完成故事任务
    private static int executeCompleteServerTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int taskId = IntegerArgumentType.getInteger(context, "taskId");
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

        TaskDataManager.playerCompleteStoryTask(taskId, targetPlayer.getName().getString(), targetPlayer.getUUID());
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("已标记玩家 " + targetPlayer.getName().getString() + " 完成故事任务ID：" + taskId),
                true
        );
        return 1;
    }

    // 执行查询全服总统计
    private static int executeStatsTotal(CommandContext<CommandSourceStack> context) {
        int totalCompletions = StoryStageManager.getTotalTaskCompletions();
        int totalUniquePlayers = StoryStageManager.getTotalUniquePlayers();
        int stageCount = StoryStageManager.getStageCount();

        String stats = String.format(
                "=== 全服任务统计 ===\n" +
                "总阶段数: %d\n" +
                "总任务完成次数: %d\n" +
                "参与玩家数: %d 人",
                stageCount, totalCompletions, totalUniquePlayers
        );

        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(stats), true);
        return 1;
    }

    // 执行查询阶段统计
    private static int executeStatsStage(CommandContext<CommandSourceStack> context) {
        int stageId = IntegerArgumentType.getInteger(context, "stageId");
        String stats = StoryStageManager.getStageStatisticsString(stageId);

        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(stats), true);
        return 1;
    }

    // 执行查询任务统计
    private static int executeStatsTask(CommandContext<CommandSourceStack> context) {
        int taskId = IntegerArgumentType.getInteger(context, "taskId");
        String stats = StoryStageManager.getTaskStatisticsString(taskId);

        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(stats), true);

        // 同时显示完成该任务的玩家列表
        java.util.List<String> players = StoryStageManager.getTaskFinishedPlayers(taskId);
        if (!players.isEmpty()) {
            String playerList = "完成玩家: " + String.join(", ", players);
            context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(playerList), true);
        }

        return 1;
    }
}