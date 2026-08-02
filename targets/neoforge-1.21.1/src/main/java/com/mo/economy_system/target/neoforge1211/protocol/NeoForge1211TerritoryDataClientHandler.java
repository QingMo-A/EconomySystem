package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Client-only response application, kept out of the server handler's linkage surface. */
final class NeoForge1211TerritoryDataClientHandler {
  private NeoForge1211TerritoryDataClientHandler() {}
  static void handle(TerritoryDataResponseMessage message) {
    if (!(Minecraft.getInstance().screen instanceof Screen_Territory screen)
        || !screen.acceptsRequest(message.requestId())) return;
    try {
      List<Territory> owned = message.owned().stream().map(TerritoryNetworkSnapshots::restoreOwned).toList();
      List<Territory> authorized = message.authorized().stream().map(TerritoryNetworkSnapshots::restoreSummary).toList();
      screen.commitTerritoryData(message.requestId(), owned, authorized);
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error("Territory sync failed requestId={} stage=client-restore owned={} authorized={}",
          message.requestId(), message.owned().size(), message.authorized().size(), error);
      screen.territorySyncFailed(message.requestId());
    }
  }
}
