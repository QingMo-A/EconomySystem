package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckResultRoutingService;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mojang.logging.LogUtils;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201ClientFileCheckResultRequestHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201ClientFileCheckResultRequestHandler() {}

  static void handle(
      ClientFileCheckResultRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer authenticatedTarget = context.getSender();
    if (authenticatedTarget != null) {
      MinecraftServer server = authenticatedTarget.getServer();
      if (server != null && server.overworld() != null) {
        long tick = server.overworld().getGameTime();
        ClientFileCheckResultRoutingService.route(
            message,
            authenticatedTarget.getUUID(),
            tick,
            Forge1201ClientFileCheckRuntime.store(server),
            Forge1201ClientFileCheckRuntime.transfers(server).authorizations(),
            requesterId -> server.getPlayerList().getPlayer(requesterId),
            (requester, response) ->
                Forge1201NetworkChannel.sendToPlayer((ServerPlayer) requester, response),
            (stage, targetId, requesterId, failure) -> {
              if (failure == null)
                LOGGER.warn(
                    "Client file check {} target={} requester={}", stage, targetId, requesterId);
              else
                LOGGER.error(
                    "Client file check {} target={} requester={}",
                    stage,
                    targetId,
                    requesterId,
                    failure);
            });
      }
    }
    context.setPacketHandled(true);
  }
}
