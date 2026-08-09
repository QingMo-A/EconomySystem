package com.mo.economy_system.target.neoforge1211.protocol;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.market.MarketDataQueryService;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

/** NeoForge adapter for common market queries and client snapshot publication. */
public final class NeoForge1211MarketDataHandlers {
  private static final Logger LOGGER = LogUtils.getLogger();
  private NeoForge1211MarketDataHandlers() {}

  public static void request(MarketDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      try {
        var data = MarketSavedData.getInstance(player.serverLevel());
        EconomySystem_NetworkManager.sendToClient(player,
            MarketDataQueryService.query(data.getView(), player.getUUID(), message));
      } catch (RuntimeException exception) {
        LOGGER.error("Failed to serve market data request player={} request={}",
            player.getUUID(), message.requestId(), exception);
      }
    });
  }

  public static void response(MarketDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!ClientMarketState.apply(message)) {
        LOGGER.debug("Ignored stale market response requestId={} revision={}",
            message.requestId(), message.marketRevision());
      }
    });
  }
}
