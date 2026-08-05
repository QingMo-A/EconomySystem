package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFileCheckConsentCoordinatorTest {
  @Test
  void retainsIdentityThroughConsentScanAndSend() {
    var coordinator = new ClientFileCheckConsentCoordinator();
    var session = session();
    var first = identity();
    var other = identity();
    assertEquals(
        ClientFileCheckConsentCoordinator.Decision.OPEN, coordinator.receive(first, session));
    assertEquals(ClientFileCheckConsentCoordinator.State.CONSENT, coordinator.state());
    assertEquals(
        ClientFileCheckConsentCoordinator.Decision.DUPLICATE, coordinator.receive(first, session));
    assertEquals(
        ClientFileCheckConsentCoordinator.Decision.BUSY, coordinator.receive(other, session));
    assertTrue(
        coordinator.transition(
            first,
            session,
            ClientFileCheckConsentCoordinator.State.CONSENT,
            ClientFileCheckConsentCoordinator.State.SCANNING));
    assertEquals(
        ClientFileCheckConsentCoordinator.Decision.DUPLICATE, coordinator.receive(first, session));
    assertTrue(coordinator.beginSending(first, session));
    assertEquals(ClientFileCheckConsentCoordinator.State.SENDING, coordinator.state());
    assertTrue(coordinator.finish(first, session));
    assertEquals(ClientFileCheckConsentCoordinator.State.IDLE, coordinator.state());
  }

  @Test
  void sessionIdentityIsPartOfActiveStateAndInvalidationIsSilent() {
    var coordinator = new ClientFileCheckConsentCoordinator();
    var request = identity();
    var first = session();
    var other =
        new ClientFileCheckTaskCoordinator.Session(
            first.generation() + 1, new Object(), first.localPlayerId());
    coordinator.receive(request, first);
    assertFalse(coordinator.held(request, other));
    coordinator.invalidate();
    assertNull(coordinator.active());
  }

  private static ClientFileCheckTaskCoordinator.Session session() {
    return new ClientFileCheckTaskCoordinator.Session(1, new Object(), UUID.randomUUID());
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity() {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS);
  }
}
