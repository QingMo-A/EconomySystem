package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211ClientFileCheckResultRequestHandler {
  private NeoForge1211ClientFileCheckResultRequestHandler() {}

  public static void handle(ClientFileCheckResultRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> process(message, context));
  }

  private static void process(
      ClientFileCheckResultRequestMessage message, IPayloadContext context) {
    if (!(context.player() instanceof ServerPlayer sender)) return;
    MinecraftServer server = sender.getServer();
    if (server == null
        || server.overworld() == null
        || !sender.getUUID().equals(message.targetPlayerId())) return;
    long tick = server.overworld().getGameTime();
    ClientFileCheckRequestStore store = NeoForge1211ClientFileCheckRuntime.store(server);
    ClientFileCheckRequestStore.Key key =
        new ClientFileCheckRequestStore.Key(
            sender.getUUID(), message.requesterPlayerId(), message.checkType());
    ClientFileCheckRequestStore.ClaimResult claimed = store.claim(key, tick);
    if (claimed.status() != ClientFileCheckRequestStore.ClaimStatus.CLAIMED) return;
    ClientFileCheckRequestStore.Claim claim = claimed.claim();
    ClientFileCheckRequestStore.Pending pending = claim.pending();
    if (!metadataMatches(message, pending)) {
      store.complete(claim);
      return;
    }
    String authoritativeJson;
    try {
      ClientFileCheckResult result = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (result.checkType() != pending.checkType())
        throw new IllegalArgumentException("check type");
      authoritativeJson = ClientFileCheckResultJsonCodec.encode(result);
    } catch (RuntimeException invalid) {
      authoritativeJson =
          ClientFileCheckResultJsonCodec.encode(
              ClientFileCheckResult.failed(pending.checkType(), "INVALID_RESULT"));
    }
    ServerPlayer requester = server.getPlayerList().getPlayer(pending.requesterPlayerId());
    if (requester == null) {
      store.complete(claim);
      EconomySystem.LOGGER.warn(
          "Discarded file-check result because requester {} is offline",
          pending.requesterPlayerId());
      return;
    }
    ClientFileCheckResultResponseMessage response =
        new ClientFileCheckResultResponseMessage(
            pending.targetPlayerName(),
            pending.targetPlayerId(),
            pending.requesterPlayerName(),
            pending.requesterPlayerId(),
            pending.checkType(),
            authoritativeJson);
    try {
      EconomySystem_NetworkManager.sendToClient(requester, response);
      store.complete(claim);
    } catch (RuntimeException failure) {
      store.release(claim);
      EconomySystem.LOGGER.error("Failed to send file-check result", failure);
    }
  }

  private static boolean metadataMatches(
      ClientFileCheckResultRequestMessage message, ClientFileCheckRequestStore.Pending pending) {
    return message.targetPlayerId().equals(pending.targetPlayerId())
        && message.targetPlayerName().equals(pending.targetPlayerName())
        && message.requesterPlayerId().equals(pending.requesterPlayerId())
        && message.requesterPlayerName().equals(pending.requesterPlayerName())
        && message.checkType() == pending.checkType();
  }
}
