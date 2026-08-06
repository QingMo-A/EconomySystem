package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import java.util.UUID;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;

public final class Forge1201ClientFileCheckClientRuntime {
  private static ClientFileCheckTaskCoordinator tasks = new ClientFileCheckTaskCoordinator();
  private static final ClientFileCheckConsentCoordinator CONSENT =
      new ClientFileCheckConsentCoordinator();
  private static final CheckedFileTransferManifestCache MANIFEST = new CheckedFileTransferManifestCache();
  private static CheckedFileTransferClientCoordinator transfers = new CheckedFileTransferClientCoordinator();
  private static boolean stopped;

  private Forge1201ClientFileCheckClientRuntime() {}

  public static synchronized ClientFileCheckTaskCoordinator.Session begin(
      Object connection, UUID playerId) {
    if (stopped) throw new IllegalStateException("client runtime stopped");
    CONSENT.invalidate();
    MANIFEST.clear();
    ClientFileCheckTaskCoordinator.Session transferSession =
        transfers.beginSession(connection, playerId);
    transfers.bindArrivalConnection(connection);
    tasks.beginSession(connection, playerId);
    return transferSession;
  }

  public static synchronized ClientFileCheckTaskCoordinator.Session currentOrBegin(
      Object connection, UUID playerId) {
    if (stopped) throw new IllegalStateException("client runtime stopped");
    ClientFileCheckTaskCoordinator.Session current = tasks.currentSession();
    if (current != null
        && current.connectionIdentity() == connection
        && current.localPlayerId().equals(playerId)) return current;
    return begin(connection, playerId);
  }

  public static synchronized void invalidate() {
    if (stopped) return;
    CONSENT.invalidate();
    MANIFEST.clear();
    transfers.invalidateSession();
    tasks.invalidateSession();
  }

  public static synchronized void stop() {
    if (stopped) return;
    stopped = true;
    CONSENT.invalidate();
    MANIFEST.clear();
    transfers.close();
    tasks.close();
  }

  public static ClientFileCheckTaskCoordinator tasks() {
    return tasks;
  }

  public static ClientFileCheckConsentCoordinator consent() {
    return CONSENT;
  }
  public static CheckedFileTransferManifestCache manifest(){return MANIFEST;}
  public static CheckedFileTransferClientCoordinator transfers(){return transfers;}

  /** Captures the transfer session at network arrival, before a callback is queued. */
  public static synchronized ClientFileCheckTaskCoordinator.Session captureArrival(
      Object connectionIdentity) {
    return transfers.captureArrivalSession(connectionIdentity);
  }

  public static synchronized boolean isStopped() {
    return stopped;
  }
}
