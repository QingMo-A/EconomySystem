package com.mo.economy_system.server.rank;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 最简单的测试指令：/testrank <玩家> <rankId>
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RankTestCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 指令：/testrank 玩家名 FISH（或FISH+/FISH++/OPERATOR）
        dispatcher.register(Commands.literal("testrank")
                .requires(source -> source.hasPermission(4)) // 只有OP能执行
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rankId", StringArgumentType.string())
                                .executes(context -> {
                                    // 1. 获取指令参数
                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                    String rankId = StringArgumentType.getString(context, "rankId").toUpperCase();

                                    // 2. 匹配Rank
                                    Rank targetRank = switch (rankId) {
                                        case "FISH" -> RankRegistry.FISH;
                                        case "FISH+" -> RankRegistry.FISH_PLUS;
                                        case "FISH++" -> RankRegistry.FISH_PLUS_PLUS;
                                        case "OPERATOR" -> RankRegistry.OPERATOR;
                                        default -> RankRegistry.NO_RANK;
                                    };

                                    // 3. 设置Rank（调用Capability的工具方法）
                                    RankCapabilityProvider.setPlayerRank(player, targetRank);

                                    // 4. 给指令执行者发提示
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§a成功给玩家§6" + player.getName().getString() + "§a设置Rank：§6" + targetRank.getRankId()),
                                            true
                                    );
                                    return 1;
                                }))));
    }
}