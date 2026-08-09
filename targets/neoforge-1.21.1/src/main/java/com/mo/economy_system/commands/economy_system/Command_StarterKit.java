package com.mo.economy_system.commands.economy_system;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mo.economy_system.common.starter.StarterKitFeedback;
import com.mo.economy_system.common.starter.StarterKitService;
import com.mo.economy_system.target.neoforge1211.starter.NeoForge1211StarterKitRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** NeoForge command and message adapter for the common starter-kit transaction. */
public final class Command_StarterKit {
  private Command_StarterKit() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("starterkit")
            .requires(source -> source.hasPermission(0))
            .executes(Command_StarterKit::execute));
  }

  private static int execute(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception error) {
      source.sendFailure(Component.translatable("message.command.player_only"));
      return 0;
    }
    MinecraftServer server = player.getServer();
    if (server == null) return 0;

    StarterKitService.Outcome outcome =
        NeoForge1211StarterKitRuntime.service(server).claim(player.getUUID());
    switch (outcome.result()) {
      case SUCCESS -> {
        player.sendSystemMessage(Component.translatable(StarterKitFeedback.SUCCESS, outcome.amount()));
        return Command.SINGLE_SUCCESS;
      }
      case ALREADY_CLAIMED -> player.sendSystemMessage(
          Component.translatable(StarterKitFeedback.ALREADY_CLAIMED));
      case BALANCE_LIMIT -> player.sendSystemMessage(
          Component.translatable(StarterKitFeedback.BALANCE_LIMIT));
      case PERSIST_FAILED -> player.sendSystemMessage(
          Component.translatable(StarterKitFeedback.PERSIST_FAILED));
      case STATE_UNKNOWN -> player.sendSystemMessage(
          Component.translatable(StarterKitFeedback.STATE_UNKNOWN));
    }
    return 0;
  }
}
