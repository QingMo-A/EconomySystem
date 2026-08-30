package com.mo.economy_system.target.neoforge1211.recycle;

import com.mo.economy_system.common.recycle.RecycleResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Minimal command surface for the recycling station until the common UI is added. */
public final class NeoForge1211RecyclerCommands {
  private NeoForge1211RecyclerCommands() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("economy_system")
        .then(Commands.literal("recycle")
            .then(Commands.literal("reload").requires(source -> source.hasPermission(2))
                .executes(context -> reload(context.getSource())))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 2304))
                .executes(context -> execute(context.getSource().getPlayerOrException(),
                    IntegerArgumentType.getInteger(context, "amount"), context.getSource())))));
  }

  private static int reload(CommandSourceStack source) {
    try {
      NeoForge1211RecyclerAdapter.reload(source.getServer());
      source.sendSuccess(() -> Component.literal("回收站配置已重载。"), true);
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("回收站配置重载失败：" + failure.getMessage()));
      return 0;
    }
  }

  private static int execute(ServerPlayer player, int amount, CommandSourceStack source) {
    try {
      RecycleResult result = NeoForge1211RecyclerAdapter.recycleHeld(player, amount);
      if (!result.success()) {
        source.sendFailure(Component.literal(message(result.status())));
        return 0;
      }
      source.sendSuccess(() -> Component.literal("回收成功：" + result.acceptedAmount() + " 件，获得 " + result.payout() + " 梦鱼币"), false);
      return result.acceptedAmount();
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("回收失败：" + failure.getMessage()));
      return 0;
    }
  }

  private static String message(RecycleResult.Status status) {
    return switch (status) {
      case UNKNOWN_ITEM -> "该物品暂不支持回收";
      case INSUFFICIENT_ITEMS -> "背包中没有足够的物品";
      case HIGH_QUOTA_EXHAUSTED -> "本周期高价回收额度已用尽";
      case BALANCE_LIMIT -> "余额已达到上限";
      case DUPLICATE_SUBMISSION -> "重复的回收请求";
      default -> "回收请求未完成：" + status;
    };
  }
}
