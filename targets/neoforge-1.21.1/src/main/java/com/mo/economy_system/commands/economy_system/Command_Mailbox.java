package com.mo.economy_system.commands.economy_system;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mo.economy_system.core.economy_system.mailbox.NeoForge1211MailboxAdminService;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Administrator command surface for mailbox announcements, notices and compensation. */
public final class Command_Mailbox {
  private Command_Mailbox() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("economy_system")
            .then(Commands.literal("mailbox").requires(source -> source.hasPermission(2))
                .then(Commands.literal("announce")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> announce(context.getSource(),
                            StringArgumentType.getString(context, "message")))))
                .then(Commands.literal("notice")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            context.getSource().getOnlinePlayerNames(), builder))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(context -> notice(context.getSource(),
                                StringArgumentType.getString(context, "target"),
                                StringArgumentType.getString(context, "message"))))))
                .then(Commands.literal("compensate")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                            context.getSource().getOnlinePlayerNames(), builder))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(context -> compensate(context.getSource(),
                                StringArgumentType.getString(context, "target"),
                                StringArgumentType.getString(context, "message"))))))));
  }

  private static int announce(CommandSourceStack source, String body) {
    NeoForge1211MailboxAdminService.announce(source.getLevel(), "", body, 0);
    source.sendSuccess(() -> Component.literal("已发布邮箱系统公告"), true);
    return 1;
  }

  private static int notice(CommandSourceStack source, String targetName, String body) {
    UUID target = NeoForge1211MailboxAdminService.resolvePlayer(source.getServer(), targetName);
    if (target == null) {
      source.sendFailure(Component.literal("找不到该玩家: " + targetName));
      return 0;
    }
    NeoForge1211MailboxAdminService.sendNotice(source.getLevel(), target, "", body);
    source.sendSuccess(() -> Component.literal("已发送系统邮件给 " + targetName), false);
    return 1;
  }

  private static int compensate(CommandSourceStack source, String targetName, String body) {
    ServerPlayer operator;
    try {
      operator = source.getPlayerOrException();
    } catch (Exception failure) {
      source.sendFailure(Component.literal("补偿附件命令需要由玩家执行"));
      return 0;
    }
    if (operator.getMainHandItem().isEmpty()) {
      source.sendFailure(Component.literal("请先在主手拿着要作为补偿附件发送的物品"));
      return 0;
    }
    UUID target = NeoForge1211MailboxAdminService.resolvePlayer(source.getServer(), targetName);
    if (target == null) {
      source.sendFailure(Component.literal("找不到该玩家: " + targetName));
      return 0;
    }
    NeoForge1211MailboxAdminService.sendCompensation(
        source.getLevel(), target, "", body, operator.getMainHandItem().copy());
    source.sendSuccess(() -> Component.literal("已向 " + targetName + " 发送补偿邮件（主手物品副本）"), false);
    return 1;
  }
}
