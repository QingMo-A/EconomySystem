package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.territory.TerritoryInvite;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryInviteRuntime;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command registration for accepting and declining pending invitations. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201TerritoryInviteCommands {
  private Forge1201TerritoryInviteCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("accept_invite")
        .executes(context -> decide(context, null, true))
        .then(Commands.argument("inviteId", StringArgumentType.word())
            .executes(context -> decide(context,
                StringArgumentType.getString(context, "inviteId"), true))));
    event.getDispatcher().register(Commands.literal("decline_invite")
        .executes(context -> decide(context, null, false))
        .then(Commands.argument("inviteId", StringArgumentType.word())
            .executes(context -> decide(context,
                StringArgumentType.getString(context, "inviteId"), false))));
  }

  private static int decide(CommandContext<CommandSourceStack> context, String rawId, boolean accept) {
    ServerPlayer player;
    try {
      player = context.getSource().getPlayerOrException();
    } catch (Exception error) {
      context.getSource().sendFailure(Component.translatable("message.command.player_only"));
      return 0;
    }
    MinecraftServer server = context.getSource().getServer();
    long tick = Forge1201TerritoryInviteRuntime.tick(server);
    TerritoryInviteDecisionService service = Forge1201TerritoryInviteRuntime.decisions(server);
    UUID inviteId = resolveInviteId(context.getSource(), service, player.getUUID(), rawId, tick);
    if (inviteId == null) return 0;
    TerritoryInvite invite = Forge1201TerritoryInviteRuntime.store(server).find(inviteId, tick).orElse(null);
    TerritoryInviteDecisionService.Result result = accept
        ? service.accept(inviteId, player.getUUID(), player.getGameProfile().getName(), tick)
        : service.decline(inviteId, player.getUUID(), tick);
    if ((accept && result == TerritoryInviteDecisionService.Result.ACCEPTED)
        || (!accept && result == TerritoryInviteDecisionService.Result.DECLINED)) {
      String key = accept ? "message.invite.accepted" : "message.invite.declined";
      String name = invite == null ? "" : invite.territoryName();
      context.getSource().sendSuccess(() -> Component.translatable(key, name), false);
      return 1;
    }
    context.getSource().sendFailure(message(result));
    return 0;
  }

  private static UUID resolveInviteId(CommandSourceStack source,
      TerritoryInviteDecisionService service, UUID playerId, String rawId, long tick) {
    if (rawId != null) {
      try {
        return UUID.fromString(rawId);
      } catch (IllegalArgumentException error) {
        source.sendFailure(Component.translatable("message.invite.not_found"));
        return null;
      }
    }
    int count = service.pending(playerId, tick);
    if (count == 0) {
      source.sendFailure(Component.translatable("message.invite.no_pending"));
      return null;
    }
    if (count > 1) {
      source.sendFailure(Component.translatable("message.invite.multiple_pending"));
      return null;
    }
    return service.sole(playerId, tick).orElse(null);
  }

  static Component message(TerritoryInviteDecisionService.Result result) {
    return switch (result) {
      case ACCEPTED -> Component.translatable("message.invite.accepted");
      case DECLINED -> Component.translatable("message.invite.declined");
      case NOT_FOUND -> Component.translatable("message.invite.no_pending");
      case NOT_TARGET -> Component.translatable("message.invite.not_target");
      case TERRITORY_NOT_FOUND -> Component.translatable("message.invite.target_not_found");
      case OWNER_CHANGED -> Component.translatable("message.invite.owner_changed");
      case ALREADY_MEMBER -> Component.translatable("message.invite.already_member");
      case PERSIST_FAILED -> Component.translatable("message.invite.persist_failed");
      case STATE_UNKNOWN -> Component.translatable("message.invite.state_unknown");
      case BUSY -> Component.translatable("message.invite.busy");
      case MULTIPLE_PENDING -> Component.translatable("message.invite.multiple_pending");
    };
  }
}
