package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.client.ClientTerritoryState;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryDataQueryService;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201TerritoryDataHandlers {
  private static final Logger LOGGER = LogUtils.getLogger();
  private Forge1201TerritoryDataHandlers() {}

  static void handleRequest(TerritoryDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) {
      try {
        Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
        TerritoryDataResponseMessage response = TerritoryDataQueryService.query(
            message, player.getUUID(), new TerritoryDataQueryService.Repository() {
              public java.util.List<com.mo.economy_system.common.territory.TerritorySnapshots.Owned> owned(java.util.UUID id) {
                return store.owned(id);
              }
              public java.util.List<com.mo.economy_system.common.territory.TerritorySnapshots.Summary> authorized(java.util.UUID id) {
                return store.authorized(id);
              }
            });
        Forge1201NetworkChannel.sendToPlayer(player, response);
      } catch (RuntimeException error) {
        LOGGER.error("Territory sync failed player={} requestId={} stage=query owned=0 authorized=0",
            player.getUUID(), message.requestId(), error);
        player.sendSystemMessage(Component.translatable("message.territory.sync_failed"));
      }
    }
    context.setPacketHandled(true);
  }

  static void handleResponse(TerritoryDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    ClientTerritoryState.apply(message);
    supplier.get().setPacketHandled(true);
  }
}
