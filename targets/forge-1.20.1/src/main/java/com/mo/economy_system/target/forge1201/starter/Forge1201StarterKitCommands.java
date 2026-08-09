package com.mo.economy_system.target.forge1201.starter;

import com.mojang.brigadier.Command;
import com.mo.economy_system.common.starter.StarterKitFeedback;
import com.mo.economy_system.common.starter.StarterKitService;
import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge Brigadier adapter for the common starter-kit transaction. */
@Mod.EventBusSubscriber(
    modid = EconomySystemForge1201.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201StarterKitCommands {
  private Forge1201StarterKitCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(
        Commands.literal("starterkit")
            .requires(source -> source.hasPermission(0))
            .executes(context -> execute(context.getSource())));
  }

  private static int execute(CommandSourceStack source) {
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception error) {
      source.sendFailure(Component.translatable("message.command.player_only"));
      return 0;
    }
    if (player.getServer() == null) return 0;
    StarterKitService.Outcome outcome =
        Forge1201StarterKitRuntime.service(player.getServer()).claim(player.getUUID());
    switch (outcome.result()) {
      case SUCCESS -> {
        player.sendSystemMessage(Component.translatable(StarterKitFeedback.SUCCESS, outcome.amount()));
        return Command.SINGLE_SUCCESS;
      }
      case ALREADY_CLAIMED -> player.sendSystemMessage(Component.translatable(StarterKitFeedback.ALREADY_CLAIMED));
      case BALANCE_LIMIT -> player.sendSystemMessage(Component.translatable(StarterKitFeedback.BALANCE_LIMIT));
      case PERSIST_FAILED -> player.sendSystemMessage(Component.translatable(StarterKitFeedback.PERSIST_FAILED));
      case STATE_UNKNOWN -> player.sendSystemMessage(Component.translatable(StarterKitFeedback.STATE_UNKNOWN));
    }
    return 0;
  }
}
