package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import java.util.UUID;
import com.mo.economy_system.common.transfer.CheckedFileTransferManifestCache;
import com.mo.economy_system.common.transfer.CheckedFileTransferClientCoordinator;

public final class NeoForge1211ClientFileCheckClientRuntime {
  private static ClientFileCheckTaskCoordinator tasks = new ClientFileCheckTaskCoordinator();
  private static final ClientFileCheckConsentCoordinator CONSENT =
      new ClientFileCheckConsentCoordinator();
  private static final CheckedFileTransferManifestCache MANIFEST = new CheckedFileTransferManifestCache();
  private static CheckedFileTransferClientCoordinator transfers = new CheckedFileTransferClientCoordinator();

  private NeoForge1211ClientFileCheckClientRuntime() {}

  public static synchronized ClientFileCheckTaskCoordinator.Session begin(
      Object connection, UUID playerId) {
    CONSENT.invalidate();
    MANIFEST.clear();
    transfers.beginSession(connection, playerId);
    return tasks.beginSession(connection, playerId);
  }

  public static synchronized ClientFileCheckTaskCoordinator.Session currentOrBegin(
      Object connection, UUID playerId) {
    ClientFileCheckTaskCoordinator.Session current = tasks.currentSession();
    if (current != null
        && current.connectionIdentity() == connection
        && current.localPlayerId().equals(playerId)) return current;
    return begin(connection, playerId);
  }

  public static synchronized void invalidate() {
    CONSENT.invalidate();
    MANIFEST.clear();
    transfers.invalidateSession();
    tasks.invalidateSession();
  }

  public static synchronized void stop() {
    CONSENT.invalidate();
    MANIFEST.clear();
    transfers.close();
    transfers = new CheckedFileTransferClientCoordinator();
    tasks.close();
    tasks = new ClientFileCheckTaskCoordinator();
  }

  public static ClientFileCheckTaskCoordinator tasks() {
    return tasks;
  }

  public static ClientFileCheckConsentCoordinator consent() {
    return CONSENT;
  }
  public static CheckedFileTransferManifestCache manifest(){return MANIFEST;}
  public static CheckedFileTransferClientCoordinator transfers(){return transfers;}
}
