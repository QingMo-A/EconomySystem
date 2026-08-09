package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.territory.TerritoryBackpointService;
import com.mo.economy_system.common.territory.TerritoryManagementResult;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryInviteHandler;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritorySnapshotStore;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command adapters for baseline territory utility commands. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201TerritoryCommands {
  private Forge1201TerritoryCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(
        Commands.literal("setbackpoint")
            .executes(context -> setBackpoint(context.getSource().getPlayerOrException())));
    event.getDispatcher().register(
        Commands.literal("invite")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> invite(
                    context.getSource().getPlayerOrException(),
                    EntityArgument.getPlayer(context, "player")))));
  }

  private static int setBackpoint(ServerPlayer player) {
    int x = (int) Math.floor(player.getX());
    int y = (int) Math.floor(player.getY());
    int z = (int) Math.floor(player.getZ());
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
    String dimension = player.serverLevel().dimension().location().toString();
    TerritoryManagementResult result = TerritoryBackpointService.execute(
        player.getUUID(),
        dimension,
        new Position(x, y, z),
        store,
        TerritoryBackpointService.Diagnostics.noop());
    if (result != TerritoryManagementResult.SUCCESS) {
      String key = switch (result) {
        case NOT_FOUND, NOT_OWNER, INVALID_TARGET ->
            "message.territory.setbackpoint.no_permission";
        case PERSIST_FAILED -> "message.claim.resize.persist_failed";
        default -> "message.claim.resize.state_unknown";
      };
      player.sendSystemMessage(Component.translatable(key));
      return 0;
    }
    player.sendSystemMessage(Component.translatable(
        "message.territory.setbackpoint.success", x, y, z));
    return 1;
  }

  private static int invite(ServerPlayer sender, ServerPlayer target) {
    int x = (int) Math.floor(sender.getX());
    int z = (int) Math.floor(sender.getZ());
    String dimension = sender.serverLevel().dimension().location().toString();
    var territory = Forge1201TerritorySnapshotStore.get(sender.serverLevel())
        .at(dimension, x, z).orElse(null);
    if (territory == null || !territory.summary().ownerId().equals(sender.getUUID())) {
      sender.sendSystemMessage(Component.translatable("message.invite.not_in_territory"));
      return 0;
    }
    Forge1201TerritoryInviteHandler.request(
        sender, territory.summary().territoryId(), target.getUUID());
    return 1;
  }
}
