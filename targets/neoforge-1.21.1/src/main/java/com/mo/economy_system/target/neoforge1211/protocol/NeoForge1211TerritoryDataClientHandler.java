package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import net.minecraft.client.Minecraft;

/** Client-only response application, kept out of the server handler's linkage surface. */
final class NeoForge1211TerritoryDataClientHandler {
  private NeoForge1211TerritoryDataClientHandler() {}
  static void handle(TerritoryDataResponseMessage message) {
    if (!(Minecraft.getInstance().screen instanceof Screen_Territory screen)) return;
    apply(message, screen);
  }

  static boolean apply(TerritoryDataResponseMessage message,
      TerritoryDataClientApplier.TerritoryScreenTarget<Territory, Territory> screen) {
    return TerritoryDataClientApplier.apply(message, screen,
        TerritoryNetworkSnapshots::restoreOwned, TerritoryNetworkSnapshots::restoreSummary,
        (requestId, owned, authorized, error) -> EconomySystem.LOGGER.error(
            "Territory sync failed requestId={} stage=client-restore owned={} authorized={}",
            requestId, owned, authorized, error));
  }
}
