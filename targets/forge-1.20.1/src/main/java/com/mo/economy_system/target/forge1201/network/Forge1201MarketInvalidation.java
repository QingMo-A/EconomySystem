package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.market.MarketInvalidationFactory;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Forge network adapter for publishing the current market revision. */
final class Forge1201MarketInvalidation {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201MarketInvalidation() {}

  static void broadcast(ServerPlayer source) {
    broadcast(source.server, source.serverLevel());
  }

  static void broadcast(MinecraftServer server, ServerLevel level) {
    var message = MarketInvalidationFactory.create(MarketSavedData.getInstance(level).getView());
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      try {
        Forge1201NetworkChannel.sendToPlayer(player, message);
      } catch (RuntimeException exception) {
        LOGGER.error("Failed to invalidate market for player={}", player.getUUID(), exception);
      }
    }
  }
}
