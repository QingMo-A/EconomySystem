package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryDataQueryService;
import com.mo.economy_system.common.territory.TerritoryDataServerService;
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
    if (player != null) context.enqueueWork(() -> {
      boolean success = TerritoryDataServerService.serve(message, player.getUUID(),
          new TerritoryDataQueryService.Repository() {
            public java.util.List<com.mo.economy_system.common.territory.TerritorySnapshots.Owned> owned(java.util.UUID id) {
              return Forge1201TerritorySnapshotStore.get(player.serverLevel()).owned(id);
            }
            public java.util.List<com.mo.economy_system.common.territory.TerritorySnapshots.Summary> authorized(java.util.UUID id) {
              return Forge1201TerritorySnapshotStore.get(player.serverLevel()).authorized(id);
            }
          }, response -> Forge1201NetworkChannel.sendToPlayer(player, response),
          (playerId, requestId, stage, owned, authorized, error) -> LOGGER.error(
              "Territory sync failed player={} requestId={} stage={} owned={} authorized={}",
              playerId, requestId, stage, owned, authorized, error));
      if (!success) {
        player.sendSystemMessage(Component.translatable("message.territory.sync_failed"));
      }
    });
    context.setPacketHandled(true);
  }

}
