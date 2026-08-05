package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFileCheckConsentCoordinatorTest {
  @Test
  void distinguishesDuplicateAndBusyAndCanInvalidate() {
    var coordinator = new ClientFileCheckConsentCoordinator();
    var first = identity(ClientFileCheckType.MODS);
    var other =
        new ClientFileCheckTaskCoordinator.RequestIdentity(
            first.targetPlayerId(), UUID.randomUUID(), ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckConsentCoordinator.Decision.OPEN, coordinator.receive(first));
    assertEquals(ClientFileCheckConsentCoordinator.Decision.DUPLICATE, coordinator.receive(first));
    assertEquals(ClientFileCheckConsentCoordinator.Decision.BUSY, coordinator.receive(other));
    assertFalse(coordinator.finish(other));
    assertTrue(coordinator.finish(first));
    assertEquals(ClientFileCheckConsentCoordinator.Decision.OPEN, coordinator.receive(other));
    coordinator.invalidate();
    assertNull(coordinator.active());
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity(ClientFileCheckType type) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        UUID.randomUUID(), UUID.randomUUID(), type);
  }
}
