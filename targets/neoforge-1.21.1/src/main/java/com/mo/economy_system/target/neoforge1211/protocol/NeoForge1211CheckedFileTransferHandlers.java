package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileTransferRoutingService;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211CheckedFileTransferClient;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class NeoForge1211CheckedFileTransferHandlers {
  private NeoForge1211CheckedFileTransferHandlers() {}

  static void controlRequest(CheckedFileTransferControlRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer target) || target.getServer() == null) return;
      var server = target.getServer();
      CheckedFileTransferRoutingService.control(message, target.getUUID(), server.getTickCount(),
          NeoForge1211ClientFileCheckRuntime.transfers(server).transfers(),
          id -> server.getPlayerList().getPlayer(id),
          (player, response) -> com.mo.economy_system.network.EconomySystem_NetworkManager
              .sendToClient((ServerPlayer) player, (EconomyNetworkMessage) response));
    });
  }

  static void chunkRequest(CheckedFileTransferChunkRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer target) || target.getServer() == null) return;
      var server = target.getServer();
      CheckedFileTransferRoutingService.chunk(message, target.getUUID(), server.getTickCount(),
          NeoForge1211ClientFileCheckRuntime.transfers(server).transfers(),
          id -> server.getPlayerList().getPlayer(id),
          (player, response) -> com.mo.economy_system.network.EconomySystem_NetworkManager
              .sendToClient((ServerPlayer) player, (EconomyNetworkMessage) response));
    });
  }

  static void request(CheckedFileTransferRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211CheckedFileTransferClient.handle(message));
  }
  static void controlResponse(CheckedFileTransferControlResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211CheckedFileTransferClient.control(message));
  }
  static void chunkResponse(CheckedFileTransferChunkResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> NeoForge1211CheckedFileTransferClient.chunk(message));
  }
}
