package com.mo.economy_system.commands.level_system;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.handler.BiomeExplorationHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 生物群系探索查询命令
 * 用法：/biomes 或 /biomes <玩家名>
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Biomes {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        // 查询自己的生物群系探索进度
        dispatcher.register(Commands.literal("biomes")
                .executes(Command_Biomes::checkSelf)
                // 查询指定玩家的生物群系探索进度（管理员权限）
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(source -> source.hasPermission(2))
                        .executes(Command_Biomes::checkOther)
                )
        );
    }

    /**
     * 查询自己的生物群系探索进度
     */
    private static int checkSelf(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("§c只有玩家才能使用此命令！"));
            return Command.SINGLE_SUCCESS;
        }

        BiomeExplorationHandler.showExploredBiomes(player);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 查询其他玩家的生物群系探索进度
     */
    private static int checkOther(CommandContext<CommandSourceStack> context) {
        ServerPlayer sender = context.getSource().getPlayer();
        if (sender == null) {
            context.getSource().sendFailure(Component.literal("§c只有玩家才能使用此命令！"));
            return Command.SINGLE_SUCCESS;
        }

        try {
            String playerName = StringArgumentType.getString(context, "player");
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);

            if (targetPlayer == null) {
                context.getSource().sendFailure(Component.literal("§c找不到玩家：" + playerName));
                return Command.SINGLE_SUCCESS;
            }

            sender.sendSystemMessage(Component.literal("§e" + playerName + " §7的生物群系探索进度："));
            BiomeExplorationHandler.showExploredBiomes(targetPlayer);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c命令执行失败：" + e.getMessage()));
        }

        return Command.SINGLE_SUCCESS;
    }
}
