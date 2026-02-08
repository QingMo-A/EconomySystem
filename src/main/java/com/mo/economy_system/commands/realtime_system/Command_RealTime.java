package com.mo.economy_system.commands.realtime_system;

import com.mo.economy_system.core.realtime_system.RealTimeManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 实时时间系统命令
 * /realtime <true/false> - 开启/关闭实时时间同步
 */
public class Command_RealTime {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realtime")
                .requires(source -> source.hasPermission(2)) // 需要管理员权限
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(Command_RealTime::setEnabled)
                )
                .executes(Command_RealTime::showStatus)
        );
    }

    /**
     * 设置实时时间系统开关
     */
    private static int setEnabled(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        CommandSourceStack source = context.getSource();

        RealTimeManager.setEnabled(source.getLevel(), enabled);

        String status = enabled ? "开启" : "关闭";
        source.sendSuccess(() -> Component.literal("实时时间同步已" + status), true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 显示当前状态
     */
    private static int showStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean enabled = RealTimeManager.isEnabled(source.getLevel());

        String status = enabled ? "开启" : "关闭";
        source.sendSuccess(() -> Component.literal("实时时间同步当前状态: " + status), false);

        return Command.SINGLE_SUCCESS;
    }
}
