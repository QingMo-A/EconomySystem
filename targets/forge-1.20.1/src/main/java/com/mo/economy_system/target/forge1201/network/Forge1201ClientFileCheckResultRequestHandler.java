package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckResultJsonCodec;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
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
    ServerPlayer sender = context.getSender();
    if (sender != null) process(sender, message);
    context.setPacketHandled(true);
  }

  private static void process(ServerPlayer sender, ClientFileCheckResultRequestMessage message) {
    MinecraftServer server = sender.getServer();
    if (server == null
        || server.overworld() == null
        || !sender.getUUID().equals(message.targetPlayerId())) return;
    long tick = server.overworld().getGameTime();
    ClientFileCheckRequestStore store = Forge1201ClientFileCheckRuntime.store(server);
    var key =
        new ClientFileCheckRequestStore.Key(
            sender.getUUID(), message.requesterPlayerId(), message.checkType());
    var result = store.claim(key, tick);
    if (result.status() != ClientFileCheckRequestStore.ClaimStatus.CLAIMED) return;
    var claim = result.claim();
    var pending = claim.pending();
    if (!metadataMatches(message, pending)) {
      store.complete(claim);
      return;
    }
    String json;
    try {
      ClientFileCheckResult parsed = ClientFileCheckResultJsonCodec.decode(message.resultJson());
      if (parsed.checkType() != pending.checkType())
        throw new IllegalArgumentException("check type");
      json = ClientFileCheckResultJsonCodec.encode(parsed);
    } catch (RuntimeException invalid) {
      json =
          ClientFileCheckResultJsonCodec.encode(
              ClientFileCheckResult.failed(pending.checkType(), "INVALID_RESULT"));
    }
    ServerPlayer requester = server.getPlayerList().getPlayer(pending.requesterPlayerId());
    if (requester == null) {
      store.complete(claim);
      LOGGER.warn("File-check requester {} went offline", pending.requesterPlayerId());
      return;
    }
    try {
      Forge1201NetworkChannel.sendToPlayer(
          requester,
          new ClientFileCheckResultResponseMessage(
              pending.targetPlayerName(),
              pending.targetPlayerId(),
              pending.requesterPlayerName(),
              pending.requesterPlayerId(),
              pending.checkType(),
              json));
      store.complete(claim);
    } catch (RuntimeException failure) {
      store.release(claim);
      LOGGER.error("Failed to send file-check result", failure);
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
