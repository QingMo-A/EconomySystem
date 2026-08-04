package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.common.territory.*;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public final class NeoForge1211TerritoryRemovalHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TerritoryRemovalRateLimiterRegistry<MinecraftServer> LIMITERS =
      new TerritoryRemovalRateLimiterRegistry<>();

  private NeoForge1211TerritoryRemovalHandler() {}

  public static void handle(RemoveTerritoryMessage message, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (context.player() instanceof ServerPlayer sender) remove(sender, message);
        });
  }

  static void remove(ServerPlayer sender, RemoveTerritoryMessage message) {
    MinecraftServer server = sender.getServer();
    TerritoryRemovalService service =
        new TerritoryRemovalService(
            TerritoryManager::removeTerritoryAuthoritatively,
            LIMITERS.get(server),
            (removed, tick) ->
                NeoForge1211TerritoryInviteHandler.store(server)
                    .discardPendingForTerritory(removed.territoryId(), tick),
            (removed, tick) -> {
              Item_ClaimWand.ResizeCleanupResult cleanup =
                  Item_ClaimWand.cancelResizingForTerritory(
                      server, removed.territoryId(), removed.territoryName());
              if (cleanup.notificationFailures() > 0)
                throw new IllegalStateException(
                    "resize cleanup notification failures: " + cleanup.notificationFailures());
            },
            (stage, player, id, error) ->
                LOGGER.warn(
                    "territory removal stage={} player={} territory={}", stage, player, id, error));
    var overworld = server.overworld();
    if (overworld == null) {
      LOGGER.warn(
          "territory removal has no overworld player={} territory={}",
          sender.getUUID(),
          message.territoryId());
      sender.sendSystemMessage(Component.translatable("message.territory.remove.state_unknown"));
      return;
    }
    long tick = overworld.getGameTime();
    TerritoryRemovalService.Outcome outcome =
        service.remove(sender.getUUID(), message.territoryId(), tick);
    try {
      String key =
          switch (outcome.result()) {
            case SUCCESS -> "message.territory.remove.success";
            case TERRITORY_NOT_FOUND -> "message.territory.remove.not_found";
            case NO_PERMISSION -> "message.territory.remove.no_permission";
            case RATE_LIMITED -> "message.territory.remove.rate_limited";
            case PERSIST_FAILED -> "message.territory.remove.persist_failed";
            case STATE_UNKNOWN -> "message.territory.remove.state_unknown";
          };
      sender.sendSystemMessage(
          outcome.result() == TerritoryRemovalService.Result.SUCCESS
              ? Component.translatable(key, outcome.removedTerritory().territoryName())
              : Component.translatable(key));
    } catch (RuntimeException error) {
      LOGGER.warn("territory removal notification failed", error);
    }
  }
}
