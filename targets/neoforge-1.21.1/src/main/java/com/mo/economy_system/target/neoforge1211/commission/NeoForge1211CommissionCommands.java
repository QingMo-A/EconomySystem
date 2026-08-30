package com.mo.economy_system.target.neoforge1211.commission;

import com.mo.economy_system.common.commission.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
    }));
    commission.then(Commands.literal("list").executes(c -> {
      var player = c.getSource().getPlayerOrException();
      var state = NeoForge1211CommissionRuntime.state(player);
      c.getSource().sendSuccess(() -> Component.literal("当前委托：" + state.commissions().size()), false);
      for (var x : state.commissions()) c.getSource().sendSuccess(() -> Component.literal(x.commissionId()+" | "+x.type()+" | "+x.targetSnapshot()+" | "+x.progress()+"/"+x.requiredAmount()+" | "+x.status()), false);
      return state.commissions().size();
    }));
    var submit = Commands.literal("submit").then(Commands.argument("commissionId", UuidArgument.uuid())
        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
            .executes(c -> submit(c.getSource(), UuidArgument.getUuid(c, "commissionId"), IntegerArgumentType.getInteger(c, "amount")))));
    commission.then(submit);
    dispatcher.register(Commands.literal("economy_system").then(commission));
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
