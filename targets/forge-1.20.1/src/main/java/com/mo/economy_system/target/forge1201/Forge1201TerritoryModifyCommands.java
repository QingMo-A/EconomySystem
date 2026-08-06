package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryModifySessions;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command endpoint that commits the current server-authoritative resize session. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201TerritoryModifyCommands {
  private Forge1201TerritoryModifyCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(
        Commands.literal("confirm_modify")
            .executes(
                context -> {
                  final ServerPlayer player;
                  try {
                    player = context.getSource().getPlayerOrException();
                  } catch (Exception failure) {
                    context.getSource().sendFailure(
                        Component.translatable("message.command.player_only"));
                    return 0;
                  }
                  return Forge1201TerritoryModifySessions.confirm(player);
                }));
  }
}
