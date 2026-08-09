package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientTerritoryState;
import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.EconomySystem;

/** Client-only response application with no dependency on a particular Screen instance. */
final class NeoForge1211TerritoryDataClientHandler {
  private NeoForge1211TerritoryDataClientHandler() {}

  static void handle(TerritoryDataResponseMessage message) {
    ClientTerritoryState.apply(message);
  }

  /** Compatibility adapter for callers that still own a target-local model. */
  static boolean apply(TerritoryDataResponseMessage message,
      TerritoryDataClientApplier.TerritoryScreenTarget<Territory, Territory> target) {
    return TerritoryDataClientApplier.apply(message, target,
        TerritoryNetworkSnapshots::restoreOwned, TerritoryNetworkSnapshots::restoreSummary,
        (requestId, owned, authorized, error) -> EconomySystem.LOGGER.error(
            "Territory sync failed requestId={} stage=client-restore owned={} authorized={}",
            requestId, owned, authorized, error));
  }
}
