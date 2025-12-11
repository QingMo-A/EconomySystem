package com.mo.economy_system.commands.rank_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import com.mojang.brigadier.CommandDispatcher;
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

// 注册到模组事件总线，触发指令注册逻辑
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Rank {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /rank set <玩家> <等级名>
        dispatcher.register(
                Commands.literal("rank")
                        .requires(source -> source.hasPermission(2)) // 仅OP可使用
                        .then(Commands.literal("set") // 子指令：/rank set
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("rankName", StringArgumentType.string())
                                                .executes(Command_Rank::executeSetRank) // 执行指令的核心逻辑
                                        )
                                )
                        )
        );

        // 查询等级指令 /rank get <玩家>
        dispatcher.register(
                Commands.literal("rank")
                        .requires(source -> source.hasPermission(0)) // 所有人可查询
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(Command_Rank::executeGetRank)
                                )
                        )
        );
    }

    private static int executeSetRank(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        String rankName = StringArgumentType.getString(context, "rankName");
        Rank targetRank = switch (rankName.toUpperCase()) {
            case "NO_RANK" -> RankRegistry.NO_RANK;
            case "FISH" -> RankRegistry.FISH;
            case "FISH+" -> RankRegistry.FISH_PLUS;
            case "FISH++" -> RankRegistry.FISH_PLUS_PLUS;
            case "OPERATOR" -> RankRegistry.OPERATOR;
            default -> null;
        };

        if (targetRank == null) {
            context.getSource().sendFailure(net.minecraft.network.chat.Component.literal("无效的等级名！"));
            return 0;
        }

        RankCapabilityProvider.setPlayerRank(targetPlayer, targetRank);
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("已将玩家 " + targetPlayer.getName().getString() + " 的等级设置为：" + rankName),
                true // 是否向所有玩家广播（true=广播，false=仅执行者可见）
        );
        return 1;
    }

    private static int executeGetRank(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        Rank currentRank = RankCapabilityProvider.getPlayerRank(targetPlayer);
        context.getSource().sendSuccess(
                () -> net.minecraft.network.chat.Component.literal("玩家 " + targetPlayer.getName().getString() + " 的当前等级：" + currentRank.getRankId()),
                false
        );
        return 1;
    }
}