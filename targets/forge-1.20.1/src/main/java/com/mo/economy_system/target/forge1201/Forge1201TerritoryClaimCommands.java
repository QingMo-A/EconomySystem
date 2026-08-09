package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryClaimSessions;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command adapter for initial territory confirmation. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201TerritoryClaimCommands {
  private Forge1201TerritoryClaimCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(
        Commands.literal("confirm_claim")
            .then(Commands.argument("name", StringArgumentType.string())
                .executes(context -> {
                  final ServerPlayer player;
                  try {
                    player = context.getSource().getPlayerOrException();
                  } catch (Exception failure) {
                    context.getSource().sendFailure(
                        Component.translatable("message.command.player_only"));
                    return 0;
                  }
                  return Forge1201TerritoryClaimSessions.confirm(
                      player, StringArgumentType.getString(context, "name"));
                })));
  }
}
