package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

/** Client-only Forge protocol-18 response application. */
final class Forge1201TerritoryDataClientHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private Forge1201TerritoryDataClientHandler() {}

  static void apply(TerritoryDataResponseMessage message) {
    if (!(Minecraft.getInstance().screen instanceof Screen_Territory screen)) return;
    apply(message, screen);
  }

  static boolean apply(TerritoryDataResponseMessage message,
      TerritoryDataClientApplier.TerritoryScreenTarget<Owned, Summary> screen) {
    return TerritoryDataClientApplier.apply(message, screen, value -> value, value -> value,
        (requestId, owned, authorized, error) -> LOGGER.error(
            "Territory sync failed requestId={} stage=client-restore owned={} authorized={}",
            requestId, owned, authorized, error));
  }
}
