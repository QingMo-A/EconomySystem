package com.mo.economy_system.commands.title_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleConfig;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Title {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("title")
                // 设置玩家称号：/title set <玩家> <称号ID>
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("titleId", IntegerArgumentType.integer())
                                        .executes(Command_Title::executeSetTitle)
                                )
                        )
                )
                // 删除称号：/title delete <称号ID>
                .then(Commands.literal("delete")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("titleId", IntegerArgumentType.integer())
                                .executes(Command_Title::executeDeleteTitle)
                        )
                )
                // 查询玩家称号：/title get <玩家>
                .then(Commands.literal("get")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(Command_Title::executeGetTitle)
                        )
                )
        );
    }

    private static int executeSetTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        int titleId = IntegerArgumentType.getInteger(context, "titleId");
        Title targetTitle = TitleRegistry.getTitleById(titleId);

        if (targetTitle == null) {
            context.getSource().sendFailure(Component.literal("错误：不存在ID为 " + titleId + " 的称号！"));
            return 0;
        }

        PlayerTitleManager.setPlayerTitle(targetPlayer, targetTitle);
        context.getSource().sendSuccess(
                () -> Component.literal("已将玩家 " + targetPlayer.getName().getString() + " 的称号设置为：" + targetTitle.getTitleName()),
                true
        );
        targetPlayer.sendSystemMessage(Component.literal("你的称号已更新为：" + targetTitle.getTitleName()));
        return 1;
    }

    private static int executeDeleteTitle(CommandContext<CommandSourceStack> context) {
        int titleId = IntegerArgumentType.getInteger(context, "titleId");

        if (titleId == 0) {
            context.getSource().sendFailure(Component.literal("错误：默认称号（ID=0）不允许删除！"));
            return 0;
        }

        Title titleToDelete = TitleRegistry.getTitleById(titleId);
        if (titleToDelete == null) {
            context.getSource().sendFailure(Component.literal("错误：不存在ID为 " + titleId + " 的称号！"));
            return 0;
        }

        TitleConfig.removeTitleById(titleId);
        TitleConfig.saveConfig();

        context.getSource().sendSuccess(
                () -> Component.literal("成功删除称号：" + titleToDelete.getTitleName() + "（ID=" + titleId + "）"),
                true
        );
        return 1;
    }

    private static int executeGetTitle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");
        Title currentTitle = PlayerTitleManager.getPlayerTitle(targetPlayer);
        context.getSource().sendSuccess(
                () -> Component.literal("玩家 " + targetPlayer.getName().getString() + " 的当前称号：" + currentTitle.getTitleName()),
                false
        );
        return 1;
    }
}