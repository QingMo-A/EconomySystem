package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.territory.TerritoryInvite;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryInviteResult;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

/** Server-side Forge protocol-20 invitation request handler. */
public final class Forge1201TerritoryInviteHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201TerritoryInviteHandler() {}

  /** Command-side entry point; it uses the same common service as protocol 20. */
  public static void request(ServerPlayer sender, UUID territoryId, UUID targetPlayerId) {
    execute(sender, new InvitePlayerMessage(territoryId, targetPlayerId));
  }

  static void handle(InvitePlayerMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer sender = context.getSender();
    if (sender != null) context.enqueueWork(() -> execute(sender, message));
    context.setPacketHandled(true);
  }

  private static void execute(ServerPlayer sender, InvitePlayerMessage message) {
    MinecraftServer server = sender.serverLevel().getServer();
    long tick = Forge1201TerritoryInviteRuntime.tick(server);
    try {
      Forge1201TerritorySnapshotStore store =
          Forge1201TerritorySnapshotStore.get(sender.serverLevel());
      TerritoryInviteRequestService service = new TerritoryInviteRequestService(
          store::inviteTerritory,
          targetId -> Optional.ofNullable(server.getPlayerList().getPlayer(targetId))
              .map(target -> new TerritoryInviteRequestService.Player(
                  target.getUUID(), target.getGameProfile().getName())),
          Forge1201TerritoryInviteRuntime.store(server),
          Forge1201TerritoryInviteRuntime.limiter(server),
          Forge1201TerritoryInviteRuntime::nextInviteId,
          (stage, inviter, territory, target, error) -> LOGGER.warn(
              "invite stage={} inviter={} territory={} target={}",
              stage, inviter, territory, target, error), 1200);
      TerritoryInviteRequestService.Outcome outcome = service.create(
          sender.getUUID(), sender.getGameProfile().getName(), message.territoryId(),
          message.targetPlayerId(), tick);
      try { sendOutcome(sender, server, outcome); }
      catch (RuntimeException notificationError) { LOGGER.warn("invite notification failed", notificationError); }
    } catch (Exception error) {
      LOGGER.error("Territory invite request failed player={} territory={} target={}",
          sender.getUUID(), message.territoryId(), message.targetPlayerId(), error);
      sender.sendSystemMessage(Component.translatable("message.invite.create_failed"));
    }
  }

  private static void sendOutcome(
      ServerPlayer sender, MinecraftServer server, TerritoryInviteRequestService.Outcome outcome) {
    TerritoryInvite invite = outcome.invite();
    if (outcome.result() != TerritoryInviteResult.SUCCESS || invite == null) {
      sender.sendSystemMessage(message(outcome.result()));
      return;
    }

    sender.sendSystemMessage(Component.translatable(
        "message.invite.sent", invite.targetPlayerName(), invite.territoryName()));
    ServerPlayer target = server.getPlayerList().getPlayer(invite.targetPlayerId());
    if (target == null) return;
    Component accept = Component.translatable("button.invite.accept")
        .withStyle(style -> style
            .withColor(0x55FF55)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/accept_invite " + invite.inviteId()))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.translatable("button.invite.accept"))));
    Component decline = Component.translatable("button.invite.decline")
        .withStyle(style -> style
            .withColor(0xFF5555)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/decline_invite " + invite.inviteId()))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                Component.translatable("button.invite.decline"))));
    target.sendSystemMessage(Component.translatable(
            "message.invite.received", invite.inviterName(), invite.territoryName())
        .append(" ").append(accept).append(" ").append(decline));
  }

  static Component message(TerritoryInviteResult result) {
    return switch (result) {
      case SUCCESS -> Component.translatable("message.invite.sent");
      case TERRITORY_NOT_FOUND -> Component.translatable("message.invite.territory_not_found");
      case NO_PERMISSION -> Component.translatable("message.invite.no_permission");
      case TARGET_OFFLINE -> Component.translatable("message.invite.target_offline");
      case CANNOT_INVITE_OWNER -> Component.translatable("message.invite.cannot_invite_owner");
      case CANNOT_INVITE_SELF -> Component.translatable("message.invite.cannot_invite_self");
      case ALREADY_MEMBER -> Component.translatable("message.invite.already_member");
      case ALREADY_PENDING -> Component.translatable("message.invite.already_pending");
      case RATE_LIMITED -> Component.translatable("message.invite.rate_limited");
      case STORE_FULL -> Component.translatable("message.invite.store_full");
      case CREATE_FAILED -> Component.translatable("message.invite.create_failed");
    };
  }
}
