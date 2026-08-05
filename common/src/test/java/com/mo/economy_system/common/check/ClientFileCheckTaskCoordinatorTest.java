package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ClientFileCheckTaskCoordinatorTest {
  @Test
  void queuedCompletionIsRejectedAfterSessionInvalidation() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      Object connection = new Object();
      var session = coordinator.beginSession(connection, UUID.randomUUID());
      var queued = new ArrayDeque<Runnable>();
      var applied = new AtomicInteger();
      var produced = new CountDownLatch(1);
      var token =
          coordinator.submit(
              session,
              identity(),
              1,
              () -> 7,
              runnable -> {
                queued.add(runnable);
                produced.countDown();
              },
              ignored -> true,
              applied::set);
      assertNotNull(token);
      assertTrue(produced.await(2, TimeUnit.SECONDS));
      coordinator.invalidateSession();
      queued.remove().run();
      assertEquals(0, applied.get());
    }
  }

  @Test
  void newConnectionRejectsOldResultAndCanRunNewWork() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var old = coordinator.beginSession(new Object(), UUID.randomUUID());
      var queued = new ArrayDeque<Runnable>();
      var produced = new CountDownLatch(1);
      coordinator.submit(
          old,
          identity(),
          1,
          () -> 1,
          runnable -> {
            queued.add(runnable);
            produced.countDown();
          },
          ignored -> true,
          ignored -> fail());
      assertTrue(produced.await(2, TimeUnit.SECONDS));
      var current = coordinator.beginSession(new Object(), UUID.randomUUID());
      queued.remove().run();
      var done = new CountDownLatch(1);
      assertNotNull(
          coordinator.submit(
              current,
              identity(),
              2,
              () -> 2,
              Runnable::run,
              ignored -> true,
              ignored -> done.countDown()));
      assertTrue(done.await(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void cancelledTokenSuppressesCompletion() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var gate = new CountDownLatch(1);
      var completed = new AtomicInteger();
      var token =
          coordinator.submit(
              session,
              identity(),
              1,
              () -> {
                try {
                  gate.await();
                } catch (InterruptedException failure) {
                  Thread.currentThread().interrupt();
                }
                return 1;
              },
              Runnable::run,
              ignored -> true,
              completed::set);
      assertNotNull(token);
      token.cancel();
      gate.countDown();
      Thread.sleep(50);
      assertEquals(0, completed.get());
    }
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity() {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS);
  }
}
