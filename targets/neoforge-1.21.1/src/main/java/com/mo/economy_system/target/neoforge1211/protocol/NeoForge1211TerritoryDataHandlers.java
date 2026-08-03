package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryDataQueryService;
import com.mo.economy_system.common.territory.TerritoryDataServerService;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211TerritoryDataHandlers {
  private NeoForge1211TerritoryDataHandlers() {}

  public static void handleRequest(TerritoryDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      boolean success = TerritoryDataServerService.serve(message, player.getUUID(), new Repository(),
          response -> EconomySystem_NetworkManager.sendToClient(player, response),
          (playerId, requestId, stage, owned, authorized, error) -> EconomySystem.LOGGER.error(
              "Territory sync failed player={} requestId={} stage={} owned={} authorized={}",
              playerId, requestId, stage, owned, authorized, error));
      if (!success) {
        player.sendSystemMessage(Component.translatable("message.territory.sync_failed"));
      }
    });
  }

  public static void handleResponse(TerritoryDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211TerritoryDataClientHandler.handle(message));
  }

  private static final class Repository implements TerritoryDataQueryService.Repository {
    public List<com.mo.economy_system.common.territory.TerritorySnapshots.Owned> owned(UUID requester) {
      return TerritoryManager.getTerritoriesByOwner(requester).stream()
          .map(TerritoryNetworkSnapshots::owned).toList();
    }
    public List<com.mo.economy_system.common.territory.TerritorySnapshots.Summary> authorized(UUID requester) {
      return TerritoryManager.getAuthorizedTerritories(requester).stream()
          .map(TerritoryNetworkSnapshots::summary).toList();
    }
  }
}
