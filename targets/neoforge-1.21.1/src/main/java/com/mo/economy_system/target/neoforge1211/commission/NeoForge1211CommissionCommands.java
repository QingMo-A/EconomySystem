package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Minimal command adapter for personal commissions. */
public final class NeoForge1211CommissionCommands {
  private NeoForge1211CommissionCommands() {}
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    var commission = Commands.literal("commission");
    commission.then(Commands.literal("refresh").executes(c -> {
      var player = c.getSource().getPlayerOrException();
      var view = NeoForge1211CommissionRuntime.refresh(player);
      c.getSource().sendSuccess(() -> Component.literal("委托已刷新，新增 " + view.generation().added().size() + " 条"), false);
      return 1;
    }).then(Commands.literal("player").requires(s -> s.hasPermission(2))
        .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
            .executes(c -> {
              var target = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "target");
              var view = NeoForge1211CommissionRuntime.refresh(target);
              c.getSource().sendSuccess(() -> Component.literal("已刷新 " + target.getName().getString()
                  + "，新增 " + view.generation().added().size() + " 条。"), true);
              return 1;
            }))));
    commission.then(Commands.literal("list").executes(c -> {
      var player = c.getSource().getPlayerOrException();
      var state = NeoForge1211CommissionRuntime.state(player);
      c.getSource().sendSuccess(() -> Component.literal("当前委托：" + state.commissions().size()), false);
      for (var x : state.commissions()) c.getSource().sendSuccess(() -> Component.literal(x.commissionId()+" | "+x.type()+" | "+x.targetSnapshot()+" | "+x.progress()+"/"+x.requiredAmount()+" | "+x.status()), false);
      return state.commissions().size();
    }));
    commission.then(Commands.literal("reload").requires(s -> s.hasPermission(2))
        .executes(c -> NeoForge1211CommissionRuntime.reloadCommand(c.getSource())));
    commission.then(Commands.literal("template")
        .then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(c -> {
          for (String id : NeoForge1211CommissionRuntime.templateIds(c.getSource().getServer())) {
            c.getSource().sendSuccess(() -> Component.literal(id), false);
          }
          return NeoForge1211CommissionRuntime.templateIds(c.getSource().getServer()).size();
        })));
    var submit = Commands.literal("submit").then(Commands.argument("commissionId", UuidArgument.uuid())
        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
            .executes(c -> submit(c.getSource(), UuidArgument.getUuid(c, "commissionId"), IntegerArgumentType.getInteger(c, "amount")))));
    commission.then(submit);
    var publicCommands = Commands.literal("public")
        .then(Commands.literal("list").executes(c -> listPublic(c.getSource())))
        .then(Commands.literal("info")
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> infoPublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("create").requires(s -> s.hasPermission(2))
            .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.string())
                .then(Commands.argument("requesterId", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .then(Commands.argument("requesterName", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .then(Commands.argument("target", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(Commands.argument("targetAmount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("unitReward", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("durationSeconds", LongArgumentType.longArg(1))
                                        .then(Commands.argument("description", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                            .executes(c -> createPublic(c)))))))))))
        .then(Commands.literal("end").requires(s -> s.hasPermission(2))
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> endPublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> removePublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("submit")
            .then(Commands.argument("id", UuidArgument.uuid())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(c -> submitPublic(c.getSource(), UuidArgument.getUuid(c, "id"),
                        IntegerArgumentType.getInteger(c, "amount"))))));
    commission.then(publicCommands);
    dispatcher.register(Commands.literal("economy_system").then(commission));
  }

  private static int createPublic(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
    try {
      long now = Math.max(1L, System.currentTimeMillis());
      long duration = LongArgumentType.getLong(c, "durationSeconds");
      long expires = Math.addExact(now, Math.multiplyExact(duration, 1000L));
      var result = NeoForge1211CommissionRuntime.createPublic(c.getSource().getServer(),
          com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name"),
          com.mojang.brigadier.arguments.StringArgumentType.getString(c, "requesterId"),
          com.mojang.brigadier.arguments.StringArgumentType.getString(c, "requesterName"),
          com.mojang.brigadier.arguments.StringArgumentType.getString(c, "target"),
          IntegerArgumentType.getInteger(c, "targetAmount"), IntegerArgumentType.getInteger(c, "unitReward"),
          expires, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "description"));
      c.getSource().sendSuccess(() -> Component.literal("已创建公共委托 " + result.commissionId()
          + "，总预算 " + result.remainingBudget()), true);
      return 1;
    } catch (RuntimeException e) {
      c.getSource().sendFailure(Component.literal("创建公共委托失败：" + e.getMessage())); return 0;
    }
  }

  private static int listPublic(CommandSourceStack source) {
    var entries = NeoForge1211CommissionRuntime.listPublic(source.getServer());
    if (entries.isEmpty()) { source.sendSuccess(() -> Component.literal("当前暂无公共大型委托。"), false); return 1; }
    for (var c : entries) source.sendSuccess(() -> Component.literal(format(c)), false);
    return entries.size();
  }

  private static int infoPublic(CommandSourceStack source, UUID id) {
    var c = NeoForge1211CommissionRuntime.findPublic(source.getServer(), id);
    if (c.isEmpty()) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    source.sendSuccess(() -> Component.literal(format(c.get())), false); return 1;
  }

  private static int endPublic(CommandSourceStack source, UUID id) {
    if (!NeoForge1211CommissionRuntime.cancelPublic(source.getServer(), id)) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    source.sendSuccess(() -> Component.literal("公共委托已结束。"), true); return 1;
  }

  private static int removePublic(CommandSourceStack source, UUID id) {
    if (NeoForge1211CommissionRuntime.findPublic(source.getServer(), id).isEmpty()) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    NeoForge1211CommissionRuntime.removePublic(source.getServer(), id);
    source.sendSuccess(() -> Component.literal("公共委托已删除。"), true); return 1;
  }

  private static int submitPublic(CommandSourceStack source, UUID id, int amount) {
    try {
      var player = source.getPlayerOrException();
      var result = NeoForge1211CommissionRuntime.submitPublicItem(player, id, amount);
      source.sendSuccess(() -> Component.literal("公共委托提交：" + result.outcome()
          + "，接受 " + result.acceptedAmount() + "，奖励 " + result.payout() + "（邮件领取）"), false);
      return result.acceptedAmount() > 0 ? 1 : 0;
    } catch (CommandSyntaxException | RuntimeException e) {
      source.sendFailure(Component.literal("提交公共委托失败：" + e.getMessage())); return 0;
    }
  }

  private static String format(PublicCommission c) {
    return c.commissionId() + " | " + c.name() + " | " + c.requesterName() + " | "
        + c.targetSnapshot() + " | " + (c.targetAmount() - c.remainingAmount()) + "/" + c.targetAmount()
        + " | 单价 " + c.unitReward() + " | 剩余预算 " + c.remainingBudget() + " | " + c.status();
  }

  private static int submit(CommandSourceStack source, UUID id, int amount) {
    try {
      var player = source.getPlayerOrException();
      var result = NeoForge1211CommissionRuntime.submitItem(player, id, amount);
      source.sendSuccess(() -> Component.literal("委托提交结果：" + result.outcome()), false);
      return result.accepted() ? 1 : 0;
    } catch (CommandSyntaxException | RuntimeException e) {
      source.sendFailure(Component.literal("提交失败：" + e.getMessage()));
      return 0;
    }
  }
}
