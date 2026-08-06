package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferControl;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlStatus;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.client.Minecraft;

public final class NeoForge1211CheckedFileTransferClient {
  private NeoForge1211CheckedFileTransferClient() {}

  public static void handle(
      CheckedFileTransferRequestMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null
        || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    var coordinator = NeoForge1211ClientFileCheckClientRuntime.transfers();
    var requestResult = coordinator.receiveRequest(message, arrivalSession);
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.IGNORED_STALE_SESSION
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.INVALID
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.CLOSED) return;
    var session = arrivalSession;
    var entry = NeoForge1211ClientFileCheckClientRuntime.manifest().find(
        new CheckedFileTransferManifestCache.Key(
            message.requesterPlayerId(), message.checkType(), message.fileName()), System.nanoTime());
    if (entry.isEmpty()) {
      coordinator.cancelRequest(message, session);
      send(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "STALE_CHECK")));
      return;
    }
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.OPEN) {
      minecraft.setScreen(new Screen_CheckedFileTransferConsent(message, entry.get(), session));
    } else if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.CONSENT_BUSY) {
      send(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "CONSENT_BUSY")));
    }
  }

  private static void send(com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator.Session session,
                           Object message) {
    if (session == null) return;
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    if (active != null && active.generation() == session.generation()
        && active.connectionIdentity() == session.connectionIdentity()
        && active.localPlayerId().equals(session.localPlayerId())) {
      EconomySystem_NetworkManager.sendToServer((EconomyNetworkMessage) message);
    }
  }
  public static void control(
      CheckedFileTransferControlResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    CheckedFileTransferIncomingRuntime.control(message, arrivalSession);
  }
  public static void chunk(
      CheckedFileTransferChunkResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    CheckedFileTransferIncomingRuntime.chunk(message, arrivalSession);
  }
}
