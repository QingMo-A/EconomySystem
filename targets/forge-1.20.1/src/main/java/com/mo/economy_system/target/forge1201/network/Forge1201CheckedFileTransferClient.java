package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;
import com.mo.economy_system.common.transfer.CheckedFileTransferControl;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlStatus;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferOutgoing;
import com.mo.economy_system.target.forge1201.client.Forge1201ClientFileCheckClientRuntime;
import net.minecraft.client.Minecraft;

/** Forge network binding for the loader-neutral checked-file transfer lifecycle. */
public final class Forge1201CheckedFileTransferClient {
  private Forge1201CheckedFileTransferClient() {}

  static void request(
      CheckedFileTransferRequestMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null
        || !minecraft.player.getUUID().equals(message.targetPlayerId())) return;
    var runtime = Forge1201ClientFileCheckClientRuntime.transfers();
    var requestResult = runtime.receiveRequest(message, arrivalSession);
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.IGNORED_STALE_SESSION
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.INVALID
        || requestResult == CheckedFileTransferClientCoordinator.RequestResult.CLOSED) return;
    ClientFileCheckTaskCoordinator.Session session = arrivalSession;
    var entry = Forge1201ClientFileCheckClientRuntime.manifest().find(
        new CheckedFileTransferManifestCache.Key(
            message.requesterPlayerId(), message.checkType(), message.fileName()),
        System.nanoTime());
    if (entry.isEmpty()) {
      runtime.cancelRequest(message, session);
      sendIfCurrent(
          session,
          CheckedFileTransferOutgoing.control(
              message, CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "STALE_CHECK")));
      return;
    }
    if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.OPEN) {
      minecraft.setScreen(new Forge1201CheckedFileTransferConsentScreen(message, entry.get(), session));
    } else if (requestResult == CheckedFileTransferClientCoordinator.RequestResult.CONSENT_BUSY) {
      sendIfCurrent(
          session,
          CheckedFileTransferOutgoing.control(
              message, CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "CONSENT_BUSY")));
    }
  }

  static void control(
      CheckedFileTransferControlResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    var result = coordinator.control(
        message, arrivalSession, minecraft.gameDirectory.toPath(), System.nanoTime());
    if (result == CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE
        && coordinator.completedArtifact() != null) {
      minecraft.setScreen(new Forge1201CheckedFileTransferResultScreen(coordinator.completedArtifact()));
    }
    pollNotification();
  }

  static void chunk(
      CheckedFileTransferChunkResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Forge1201ClientFileCheckClientRuntime.transfers().chunk(message, arrivalSession);
    pollNotification();
  }

  public static void pollNotification() {
    Minecraft minecraft = Minecraft.getInstance();
    var coordinator = Forge1201ClientFileCheckClientRuntime.transfers();
    if (coordinator.completedArtifact() != null) return;
    var notification = coordinator.pollTerminalNotification();
    if (notification != null) minecraft.setScreen(new Forge1201CheckedFileTransferResultScreen(notification));
  }

  static boolean current(ClientFileCheckTaskCoordinator.Session session) {
    if (session == null) return false;
    var current = Forge1201ClientFileCheckClientRuntime.transfers().currentSession();
    return current != null
        && current.generation() == session.generation()
        && current.connectionIdentity() == session.connectionIdentity()
        && current.localPlayerId().equals(session.localPlayerId());
  }

  static void sendIfCurrent(ClientFileCheckTaskCoordinator.Session session, Object message) {
    if (!current(session)) return;
    if (message instanceof CheckedFileTransferControlRequestMessage control) {
      Forge1201NetworkChannel.sendToServer(control);
    } else if (message instanceof CheckedFileTransferChunkRequestMessage chunk) {
      Forge1201NetworkChannel.sendToServer(chunk);
    }
  }
}
