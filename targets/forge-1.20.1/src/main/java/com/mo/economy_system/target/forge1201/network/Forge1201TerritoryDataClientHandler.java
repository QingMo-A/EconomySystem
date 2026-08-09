package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientTerritoryState;
import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Client-only response application with no dependency on a particular Screen instance. */
final class Forge1201TerritoryDataClientHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private Forge1201TerritoryDataClientHandler() {}

  static void apply(TerritoryDataResponseMessage message) {
    ClientTerritoryState.apply(message);
  }

  /** Compatibility adapter for callers that still own a target-local model. */
  static boolean apply(TerritoryDataResponseMessage message,
      TerritoryDataClientApplier.TerritoryScreenTarget<Owned, Summary> target) {
    return TerritoryDataClientApplier.apply(message, target, value -> value, value -> value,
        (requestId, owned, authorized, error) -> LOGGER.error(
            "Territory sync failed requestId={} stage=client-restore owned={} authorized={}",
            requestId, owned, authorized, error));
  }
}
