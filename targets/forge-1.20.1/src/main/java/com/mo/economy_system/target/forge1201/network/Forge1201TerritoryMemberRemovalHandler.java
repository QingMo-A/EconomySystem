package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalRateLimiterRegistry;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201TerritoryMemberRemovalHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TerritoryMemberRemovalRateLimiterRegistry<MinecraftServer> LIMITERS =
      new TerritoryMemberRemovalRateLimiterRegistry<>();

  static void handle(
      RemoveTerritoryMemberMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer sender = context.getSender();
    if (sender != null) context.enqueueWork(() -> remove(sender, message));
    context.setPacketHandled(true);
  }

  private static void remove(ServerPlayer sender, RemoveTerritoryMemberMessage message) {
    MinecraftServer server = sender.getServer();
    if (server == null) {
      notifyUnknown(sender);
      return;
    }
    var overworld = server.overworld();
    if (overworld == null) {
      notifyUnknown(sender);
      return;
    }
    long tick = overworld.getGameTime();
    TerritoryMemberRemovalService service =
        new TerritoryMemberRemovalService(
            (territory, owner, target) ->
                Forge1201TerritorySnapshotStore.get(sender.serverLevel())
                    .removeMember(territory, owner, target),
            LIMITERS.get(server),
            (target, territory, t) ->
                Forge1201TerritoryInviteRuntime.store(server).discardPending(target, territory, t),
            (stage, player, territory, error) ->
                LOGGER.warn(
                    "member removal stage={} player={} territory={}",
                    stage,
                    player,
                    territory,
                    error));
    var outcome =
        service.remove(sender.getUUID(), message.territoryId(), message.targetPlayerId(), tick);
    String key =
        switch (outcome.result()) {
          case SUCCESS -> "message.territory.member_remove.success";
          case TERRITORY_NOT_FOUND -> "message.territory.member_remove.territory_not_found";
          case NO_PERMISSION -> "message.territory.member_remove.no_permission";
          case CANNOT_REMOVE_OWNER -> "message.territory.member_remove.cannot_remove_owner";
          case TARGET_NOT_MEMBER -> "message.territory.member_remove.target_not_member";
          case RATE_LIMITED -> "message.territory.member_remove.rate_limited";
          case PERSIST_FAILED -> "message.territory.member_remove.persist_failed";
          case STATE_UNKNOWN -> "message.territory.member_remove.state_unknown";
        };
    try {
      sender.sendSystemMessage(
          outcome.result() == TerritoryMemberRemovalService.Result.SUCCESS
              ? Component.translatable(
                  key,
                  outcome.removedMember().targetPlayerName(),
                  outcome.removedMember().territoryName())
              : Component.translatable(key));
    } catch (RuntimeException e) {
      LOGGER.warn("member removal owner notification failed", e);
    }
    if (outcome.result() == TerritoryMemberRemovalService.Result.SUCCESS) {
      ServerPlayer target =
          server.getPlayerList().getPlayer(outcome.removedMember().targetPlayerId());
      if (target != null)
        try {
          target.sendSystemMessage(
              Component.translatable(
                  "message.territory.member_remove.target_notice",
                  outcome.removedMember().territoryName()));
        } catch (RuntimeException e) {
          LOGGER.warn("member removal target notification failed", e);
        }
    }
  }

  private static void notifyUnknown(ServerPlayer sender) {
    try {
      sender.sendSystemMessage(
          Component.translatable("message.territory.member_remove.state_unknown"));
    } catch (RuntimeException failure) {
      LOGGER.warn("member removal unavailable-state notification failed", failure);
    }
  }
}
