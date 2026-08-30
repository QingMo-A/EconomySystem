package com.mo.economy_system.target.forge1201.network;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mo.economy_system.EconomyConstants;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Administrator command surface for mailbox announcements, notices and compensation. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201MailboxCommands {
  private Forge1201MailboxCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    var mailbox = Commands.literal("mailbox").requires(source -> source.hasPermission(2));
    mailbox.then(Commands.literal("announce")
        .then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(context -> announce(context.getSource(),
                StringArgumentType.getString(context, "message")))));

    var noticeTarget = Commands.argument("target", StringArgumentType.word())
        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            context.getSource().getOnlinePlayerNames(), builder));
    noticeTarget.then(Commands.argument("amount", IntegerArgumentType.integer(1))
        .then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(context -> notice(context.getSource(),
                StringArgumentType.getString(context, "target"),
                StringArgumentType.getString(context, "message"),
                IntegerArgumentType.getInteger(context, "amount")))));
    noticeTarget.then(Commands.argument("message", StringArgumentType.greedyString())
        .executes(context -> notice(context.getSource(),
            StringArgumentType.getString(context, "target"),
            StringArgumentType.getString(context, "message"))));
    mailbox.then(Commands.literal("notice").then(noticeTarget));

    var moneyTarget = Commands.argument("target", StringArgumentType.word())
        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            context.getSource().getOnlinePlayerNames(), builder));
    moneyTarget.then(Commands.argument("amount", IntegerArgumentType.integer(1))
        .then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(context -> notice(context.getSource(),
                StringArgumentType.getString(context, "target"),
                StringArgumentType.getString(context, "message"),
                IntegerArgumentType.getInteger(context, "amount")))));
    mailbox.then(Commands.literal("money").then(moneyTarget));

    var compensateTarget = Commands.argument("target", StringArgumentType.word())
        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            context.getSource().getOnlinePlayerNames(), builder));
    compensateTarget.then(Commands.argument("amount", IntegerArgumentType.integer(1))
        .then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(context -> compensate(context.getSource(),
                StringArgumentType.getString(context, "target"),
                StringArgumentType.getString(context, "message"),
                IntegerArgumentType.getInteger(context, "amount")))));
    compensateTarget.then(Commands.argument("message", StringArgumentType.greedyString())
        .executes(context -> compensate(context.getSource(),
            StringArgumentType.getString(context, "target"),
            StringArgumentType.getString(context, "message"))));
    mailbox.then(Commands.literal("compensate").then(compensateTarget));

    var compensateMoneyTarget = Commands.argument("target", StringArgumentType.word())
        .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
            context.getSource().getOnlinePlayerNames(), builder));
    compensateMoneyTarget.then(Commands.argument("amount", IntegerArgumentType.integer(1))
        .then(Commands.argument("message", StringArgumentType.greedyString())
            .executes(context -> compensate(context.getSource(),
                StringArgumentType.getString(context, "target"),
                StringArgumentType.getString(context, "message"),
                IntegerArgumentType.getInteger(context, "amount")))));
    mailbox.then(Commands.literal("compensate-money").then(compensateMoneyTarget));

    event.getDispatcher().register(Commands.literal("economy_system").then(mailbox));
  }

  private static int announce(net.minecraft.commands.CommandSourceStack source, String body) {
    Forge1201MailboxAdminService.announce(source.getLevel(), "", body, 0);
    source.sendSuccess(() -> Component.literal("已发布邮箱系统公告"), true);
    return 1;
  }

  private static int notice(net.minecraft.commands.CommandSourceStack source, String targetName, String body) {
    return notice(source, targetName, body, 0);
  }

  private static int notice(net.minecraft.commands.CommandSourceStack source, String targetName,
      String body, int moneyAmount) {
    UUID target = Forge1201MailboxAdminService.resolvePlayer(source.getServer(), targetName);
    if (target == null) {
      source.sendFailure(Component.literal("找不到该玩家: " + targetName));
      return 0;
    }
    try {
      Forge1201MailboxAdminService.sendNotice(source.getLevel(), target, "", body, moneyAmount);
      source.sendSuccess(() -> Component.literal(moneyAmount > 0
          ? "已发送系统邮件给 " + targetName + "（金额 " + moneyAmount + "）"
          : "已发送系统邮件给 " + targetName), false);
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("系统邮件发送失败: " + failure.getMessage()));
      return 0;
    }
  }

  private static int compensate(net.minecraft.commands.CommandSourceStack source, String targetName, String body) {
    return compensate(source, targetName, body, 0);
  }

  private static int compensate(net.minecraft.commands.CommandSourceStack source, String targetName,
      String body, int moneyAmount) {
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
    UUID target = Forge1201MailboxAdminService.resolvePlayer(source.getServer(), targetName);
    if (target == null) {
      source.sendFailure(Component.literal("找不到该玩家: " + targetName));
      return 0;
    }
    try {
      Forge1201MailboxAdminService.sendCompensation(
          source.getLevel(), target, "", body, operator.getMainHandItem().copy(), moneyAmount);
      source.sendSuccess(() -> Component.literal(moneyAmount > 0
          ? "已向 " + targetName + " 发送补偿邮件（主手物品副本，金额 " + moneyAmount + "）"
          : "已向 " + targetName + " 发送补偿邮件（主手物品副本）"), false);
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("补偿邮件发送失败: " + failure.getMessage()));
      return 0;
    }
  }
}
