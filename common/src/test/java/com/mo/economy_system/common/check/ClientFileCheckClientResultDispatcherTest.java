package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ClientFileCheckClientResultDispatcherTest {
  @Test
  void terminalSendRequiresCurrentSessionAndClearsAfterSuccess() {
    try (var tasks = new ClientFileCheckTaskCoordinator()) {
      Object connection = new Object();
      UUID player = UUID.randomUUID();
      var session = tasks.beginSession(connection, player);
      var request = identity(player);
      var consent = new ClientFileCheckConsentCoordinator();
      consent.receive(request, session);
      var sends = new AtomicInteger();
      assertTrue(
          ClientFileCheckClientResultDispatcher.terminal(
              tasks,
              consent,
              session,
              request,
              null,
              () -> connection,
              () -> player,
              ClientFileCheckResult.declined(ClientFileCheckType.MODS),
              ignored -> sends.incrementAndGet(),
              (stage, identity, failure) -> {}));
      assertEquals(1, sends.get());
      assertEquals(ClientFileCheckConsentCoordinator.State.IDLE, consent.state());
    }
  }

  @Test
  void staleConnectionNeverSendsAndCleansLocalState() {
    try (var tasks = new ClientFileCheckTaskCoordinator()) {
      UUID player = UUID.randomUUID();
      var session = tasks.beginSession(new Object(), player);
      var request = identity(player);
      var consent = new ClientFileCheckConsentCoordinator();
      consent.receive(request, session);
      assertFalse(
          ClientFileCheckClientResultDispatcher.terminal(
              tasks,
              consent,
              session,
              request,
              null,
              Object::new,
              () -> player,
              ClientFileCheckResult.declined(ClientFileCheckType.MODS),
              ignored -> fail(),
              (stage, identity, failure) -> {}));
      assertEquals(ClientFileCheckConsentCoordinator.State.IDLE, consent.state());
    }
  }

  @Test
  void runtimeFailureIsContainedAndErrorEscapesAfterCleanup() {
    try (var tasks = new ClientFileCheckTaskCoordinator()) {
      Object connection = new Object();
      UUID player = UUID.randomUUID();
      var session = tasks.beginSession(connection, player);
      var request = identity(player);
      var consent = new ClientFileCheckConsentCoordinator();
      consent.receive(request, session);
      assertFalse(
          ClientFileCheckClientResultDispatcher.terminal(
              tasks,
              consent,
              session,
              request,
              null,
              () -> connection,
              () -> player,
              ClientFileCheckResult.declined(ClientFileCheckType.MODS),
              ignored -> {
                throw new IllegalStateException("send");
              },
              (stage, identity, failure) -> {}));
      assertEquals(ClientFileCheckConsentCoordinator.State.IDLE, consent.state());
      consent.receive(request, session);
      assertThrows(
          AssertionError.class,
          () ->
              ClientFileCheckClientResultDispatcher.terminal(
                  tasks,
                  consent,
                  session,
                  request,
                  null,
                  () -> connection,
                  () -> player,
                  ClientFileCheckResult.declined(ClientFileCheckType.MODS),
                  ignored -> {
                    throw new AssertionError("fatal");
                  },
                  (stage, identity, failure) -> {}));
      assertEquals(ClientFileCheckConsentCoordinator.State.IDLE, consent.state());
    }
  }

  @Test
  void busyReplyDoesNotReplaceOrClearOriginalRequest() {
    try (var tasks = new ClientFileCheckTaskCoordinator()) {
      Object connection = new Object();
      UUID player = UUID.randomUUID();
      var session = tasks.beginSession(connection, player);
      var original = identity(player);
      var busy = identity(player);
      var consent = new ClientFileCheckConsentCoordinator();
      consent.receive(original, session);
      assertTrue(
          ClientFileCheckClientResultDispatcher.busy(
              tasks,
              consent,
              session,
              busy,
              () -> connection,
              () -> player,
              ClientFileCheckResult.failed(ClientFileCheckType.MODS, "CONSENT_BUSY"),
              ignored -> {},
              (stage, identity, failure) -> {}));
      assertTrue(consent.held(original, session));
    }
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity(UUID target) {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        target, UUID.randomUUID(), ClientFileCheckType.MODS);
  }
}
