package com.mo.economy_system.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.market.MarketInvalidationFactory;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** NeoForge network adapter for publishing the current market revision. */
public final class MarketInvalidationBroadcaster {
  private static final Logger LOGGER = LogUtils.getLogger();

  private MarketInvalidationBroadcaster() {}

  public static void broadcast(ServerPlayer source) {
    broadcast(source.server, source.serverLevel());
  }

  public static void broadcast(MinecraftServer server, ServerLevel level) {
    var message = MarketInvalidationFactory.create(MarketSavedData.getInstance(level).getView());
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      try {
        EconomySystem_NetworkManager.sendToClient(player, message);
      } catch (RuntimeException exception) {
        LOGGER.error("Failed to invalidate market for player={}", player.getUUID(), exception);
      }
    }
  }
}
