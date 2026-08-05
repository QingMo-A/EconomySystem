package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.check.ClientFileCheckResultRoutingService;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211ClientFileCheckResultRequestHandler {
  private NeoForge1211ClientFileCheckResultRequestHandler() {}

  public static void handle(ClientFileCheckResultRequestMessage message, IPayloadContext context) {
    context.enqueueWork(
        () -> {
          if (!(context.player() instanceof ServerPlayer authenticatedTarget)) return;
          MinecraftServer server = authenticatedTarget.getServer();
          if (server == null || server.overworld() == null) return;
          long tick = server.overworld().getGameTime();
          ClientFileCheckResultRoutingService.route(
              message,
              authenticatedTarget.getUUID(),
              tick,
              NeoForge1211ClientFileCheckRuntime.store(server),
              requesterId -> server.getPlayerList().getPlayer(requesterId),
              (requester, response) ->
                  EconomySystem_NetworkManager.sendToClient((ServerPlayer) requester, response),
              (stage, targetId, requesterId, failure) -> {
                if (failure == null)
                  EconomySystem.LOGGER.warn(
                      "Client file check {} target={} requester={}", stage, targetId, requesterId);
                else
                  EconomySystem.LOGGER.error(
                      "Client file check {} target={} requester={}",
                      stage,
                      targetId,
                      requesterId,
                      failure);
              });
        });
  }
}
