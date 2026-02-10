package com.mo.economy_system.commands.time_system;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 时间系统命令组
 * /time realtime <true/false> - 开启/关闭实时时间同步
 * /time realtime - 查看实时时间状态
 * /time - 显示所有时间系统状态
 */
public class Command_Time {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("time")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限
                // 实时时间子命令
                .then(Commands.literal("realtime")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(Command_Time::setRealTimeEnabled)
                        )
                        .executes(Command_Time::showRealTimeStatus)
                )
                // 默认显示所有状态
                .executes(Command_Time::showAllStatus)
        );
    }

    /**
     * 设置实时时间系统开关
     */
    private static int setRealTimeEnabled(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        CommandSourceStack source = context.getSource();

        com.mo.economy_system.core.realtime_system.RealTimeManager.setEnabled(source.getLevel(), enabled);

        String status = enabled ? "开启" : "关闭";
        source.sendSuccess(() -> Component.literal("§a实时时间同步已" + status), true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 显示实时时间状态
     */
    private static int showRealTimeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = com.mo.economy_system.core.realtime_system.RealTimeManager.isEnabled(source.getLevel());

        String status = enabled ? "§a开启" : "§c关闭";
        source.sendSuccess(() -> Component.literal("实时时间同步当前状态: " + status), false);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 显示所有时间系统状态
     */
    private static int showAllStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // 显示实时时间状态
        boolean rtEnabled = com.mo.economy_system.core.realtime_system.RealTimeManager.isEnabled(source.getLevel());
        String rtStatus = rtEnabled ? "§a开启" : "§c关闭";
        source.sendSuccess(() -> Component.literal("实时时间同步: " + rtStatus), false);

        return Command.SINGLE_SUCCESS;
    }
}
