package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileTransferRoutingService;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

final class Forge1201CheckedFileTransferHandlers {
  private Forge1201CheckedFileTransferHandlers() {}

  static void controlRequest(CheckedFileTransferControlRequestMessage message,
                             Supplier<NetworkEvent.Context> supplied) {
    NetworkEvent.Context context = supplied.get();
    ServerPlayer target = context.getSender();
    if (target != null && target.getServer() != null) {
      var server = target.getServer();
      CheckedFileTransferRoutingService.control(message, target.getUUID(), server.getTickCount(),
          Forge1201ClientFileCheckRuntime.transfers(server).transfers(),
          id -> server.getPlayerList().getPlayer(id),
          (player, response) -> {
            if (response instanceof CheckedFileTransferControlResponseMessage control) {
              Forge1201NetworkChannel.sendToPlayer((ServerPlayer) player, control);
            }
          });
    }
    context.setPacketHandled(true);
  }

  static void chunkRequest(CheckedFileTransferChunkRequestMessage message,
                           Supplier<NetworkEvent.Context> supplied) {
    NetworkEvent.Context context = supplied.get();
    ServerPlayer target = context.getSender();
    if (target != null && target.getServer() != null) {
      var server = target.getServer();
      CheckedFileTransferRoutingService.chunk(message, target.getUUID(), server.getTickCount(),
          Forge1201ClientFileCheckRuntime.transfers(server).transfers(),
          id -> server.getPlayerList().getPlayer(id),
          (player, response) -> {
            if (response instanceof CheckedFileTransferChunkResponseMessage chunk) {
              Forge1201NetworkChannel.sendToPlayer((ServerPlayer) player, chunk);
            } else if (response instanceof CheckedFileTransferControlResponseMessage control) {
              Forge1201NetworkChannel.sendToPlayer((ServerPlayer) player, control);
            }
          });
    }
    context.setPacketHandled(true);
  }

  static void request(CheckedFileTransferRequestMessage message,
                      Supplier<NetworkEvent.Context> supplied) {
    NetworkEvent.Context context = supplied.get();
    context.enqueueWork(() -> Forge1201CheckedFileTransferClient.request(message));
    context.setPacketHandled(true);
  }
  static void controlResponse(CheckedFileTransferControlResponseMessage message,
                              Supplier<NetworkEvent.Context> supplied) {
    NetworkEvent.Context context = supplied.get();
    context.enqueueWork(() -> Forge1201CheckedFileTransferClient.control(message));
    context.setPacketHandled(true);
  }
  static void chunkResponse(CheckedFileTransferChunkResponseMessage message,
                            Supplier<NetworkEvent.Context> supplied) {
    NetworkEvent.Context context = supplied.get();
    context.enqueueWork(() -> Forge1201CheckedFileTransferClient.chunk(message));
    context.setPacketHandled(true);
  }
}
