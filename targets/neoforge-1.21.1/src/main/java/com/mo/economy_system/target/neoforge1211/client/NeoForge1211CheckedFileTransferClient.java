package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileTransferControl;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlStatus;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.client.Minecraft;

public final class NeoForge1211CheckedFileTransferClient {
  private NeoForge1211CheckedFileTransferClient() {}

  public static void handle(CheckedFileTransferRequestMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null
        || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    var session = NeoForge1211ClientFileCheckClientRuntime.currentOrBegin(
        minecraft.getConnection(), minecraft.player.getUUID());
    var entry = NeoForge1211ClientFileCheckClientRuntime.manifest().find(
        new CheckedFileTransferManifestCache.Key(
            message.requesterPlayerId(), message.checkType(), message.fileName()), System.nanoTime());
    if (entry.isEmpty()) {
      send(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "STALE_CHECK")));
      return;
    }
    var result = NeoForge1211ClientFileCheckClientRuntime.transfers().outgoing().receive(message, session);
    if (result == CheckedFileTransferOutgoing.BeginResult.OPEN) {
      minecraft.setScreen(new Screen_CheckedFileTransferConsent(message, entry.get(), session));
    } else if (result == CheckedFileTransferOutgoing.BeginResult.CONSENT_BUSY) {
      send(session, CheckedFileTransferOutgoing.control(message,
          CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "CONSENT_BUSY")));
    }
  }

  private static void send(com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator.Session session,
                           Object message) {
    var active = NeoForge1211ClientFileCheckClientRuntime.transfers().currentSession();
    if (active != null && active.generation() == session.generation()
        && active.connectionIdentity() == session.connectionIdentity()
        && active.localPlayerId().equals(session.localPlayerId())) {
      EconomySystem_NetworkManager.sendToServer((EconomyNetworkMessage) message);
    }
  }
  public static void control(CheckedFileTransferControlResponseMessage message) {
    CheckedFileTransferIncomingRuntime.control(message);
  }
  public static void chunk(CheckedFileTransferChunkResponseMessage message) {
    CheckedFileTransferIncomingRuntime.chunk(message);
  }
}
