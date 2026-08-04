package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.common.territory.*;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201TerritoryRemovalHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TerritoryRemovalRateLimiterRegistry<MinecraftServer> LIMITERS =
      new TerritoryRemovalRateLimiterRegistry<>();

  static void handle(RemoveTerritoryMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer sender = context.getSender();
    if (sender == null) {
      context.setPacketHandled(true);
      return;
    }
    context.enqueueWork(() -> remove(sender, message));
    context.setPacketHandled(true);
  }

  private static void remove(ServerPlayer sender, RemoveTerritoryMessage message) {
    MinecraftServer server = sender.getServer();
    if (server.overworld() == null) {
      LOGGER.warn(
          "territory removal has no overworld player={} territory={}",
          sender.getUUID(),
          message.territoryId());
      sender.sendSystemMessage(Component.translatable("message.territory.remove.state_unknown"));
      return;
    }
    TerritoryRemovalService service =
        new TerritoryRemovalService(
            (id, owner) ->
                Forge1201TerritorySnapshotStore.get(sender.serverLevel()).remove(id, owner),
            LIMITERS.get(server),
            (removed, tick) ->
                Forge1201TerritoryInviteRuntime.store(server)
                    .discardPendingForTerritory(removed.territoryId(), tick),
            (removed, tick) -> {
              /* Forge has no legacy resize-session state. */
            },
            (stage, player, id, error) ->
                LOGGER.warn(
                    "territory removal stage={} player={} territory={}", stage, player, id, error));
    var outcome =
        service.remove(sender.getUUID(), message.territoryId(), server.getTickCount());
    String key =
        switch (outcome.result()) {
          case SUCCESS -> "message.territory.remove.success";
          case TERRITORY_NOT_FOUND -> "message.territory.remove.not_found";
          case NO_PERMISSION -> "message.territory.remove.no_permission";
          case RATE_LIMITED -> "message.territory.remove.rate_limited";
          case PERSIST_FAILED -> "message.territory.remove.persist_failed";
          case STATE_UNKNOWN -> "message.territory.remove.state_unknown";
        };
    try {
      sender.sendSystemMessage(
          outcome.result() == TerritoryRemovalService.Result.SUCCESS
              ? Component.translatable(key, outcome.removedTerritory().territoryName())
              : Component.translatable(key));
    } catch (RuntimeException e) {
      LOGGER.warn("territory removal notification failed", e);
    }
  }
}
