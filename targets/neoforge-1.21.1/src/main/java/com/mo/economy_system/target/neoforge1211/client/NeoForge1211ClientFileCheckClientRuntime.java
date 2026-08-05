package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.check.ClientFileCheckConsentCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import java.util.UUID;

public final class NeoForge1211ClientFileCheckClientRuntime {
  private static ClientFileCheckTaskCoordinator tasks = new ClientFileCheckTaskCoordinator();
  private static final ClientFileCheckConsentCoordinator CONSENT =
      new ClientFileCheckConsentCoordinator();

  private NeoForge1211ClientFileCheckClientRuntime() {}

  public static synchronized ClientFileCheckTaskCoordinator.Session begin(
      Object connection, UUID playerId) {
    CONSENT.invalidate();
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
    tasks.invalidateSession();
  }

  public static synchronized void stop() {
    CONSENT.invalidate();
    tasks.close();
    tasks = new ClientFileCheckTaskCoordinator();
  }

  public static ClientFileCheckTaskCoordinator tasks() {
    return tasks;
  }

  public static ClientFileCheckConsentCoordinator consent() {
    return CONSENT;
  }
}
