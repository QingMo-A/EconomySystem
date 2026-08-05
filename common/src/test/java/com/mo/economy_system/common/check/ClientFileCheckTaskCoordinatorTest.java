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
              (ignored, value) -> applied.set(value),
              (ignored, failure) -> fail(failure),
              (ignored, failure) -> {});
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
          (ignored, value) -> fail(),
          (ignored, failure) -> fail(failure),
          (ignored, failure) -> {});
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
              (ignored, value) -> done.countDown(),
              (ignored, failure) -> fail(failure),
              (ignored, failure) -> {}));
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
              (ignored, value) -> completed.set(value),
              (ignored, failure) -> fail(failure),
              (ignored, failure) -> {});
      assertNotNull(token);
      token.cancel();
      gate.countDown();
      Thread.sleep(50);
      assertEquals(0, completed.get());
    }
  }

  @Test
  void runtimeFailureUsesMainThreadCallbackOnlyWhileSessionIsCurrent() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var queued = new ArrayDeque<Runnable>();
      var scheduled = new CountDownLatch(1);
      var failures = new AtomicInteger();
      assertNotNull(
          coordinator.submit(
              session,
              identity(),
              1,
              () -> {
                throw new IllegalStateException("scan");
              },
              runnable -> {
                queued.add(runnable);
                scheduled.countDown();
              },
              ignored -> true,
              (ignored, value) -> fail(),
              (ignored, failure) -> failures.incrementAndGet(),
              (ignored, failure) -> {}));
      assertTrue(scheduled.await(2, TimeUnit.SECONDS));
      coordinator.invalidateSession();
      queued.remove().run();
      assertEquals(0, failures.get());
    }
  }

  @Test
  void errorDoesNotInvokeRuntimeFailureAndWorkerRecovers() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var failure = new AtomicInteger();
      assertNotNull(
          coordinator.submit(
              session,
              identity(),
              1,
              () -> {
                throw new AssertionError("fatal");
              },
              Runnable::run,
              ignored -> true,
              (ignored, value) -> fail(),
              (ignored, problem) -> failure.incrementAndGet(),
              (ignored, problem) -> {}));
      Thread.sleep(50);
      var done = new CountDownLatch(1);
      assertNotNull(
          coordinator.submit(
              session,
              identity(),
              2,
              () -> 1,
              Runnable::run,
              ignored -> true,
              (ignored, value) -> done.countDown(),
              (ignored, problem) -> fail(),
              (ignored, problem) -> {}));
      assertTrue(done.await(2, TimeUnit.SECONDS));
      assertEquals(0, failure.get());
    }
  }

  @Test
  void completionReceivesItsRealTokenAndCompletesHandle() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var done = new CountDownLatch(1);
      ClientFileCheckTaskCoordinator.TaskToken[] seen = new ClientFileCheckTaskCoordinator.TaskToken[1];
      var token = coordinator.submit(session, identity(), 7, () -> 9, Runnable::run,
          ignored -> true, (actual, value) -> { seen[0] = actual; done.countDown(); },
          (ignored, failure) -> fail(failure), (ignored, failure) -> fail(failure));
      assertTrue(done.await(2, TimeUnit.SECONDS));
      assertSame(token, seen[0]);
      assertEquals(ClientFileCheckTaskCoordinator.TaskState.COMPLETED, token.state());
    }
  }

  @Test
  void schedulingFailureAbandonsAndMarksDispatchFailedThenExecutorIsReusable() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var abandoned = new CountDownLatch(1);
      var token = coordinator.submit(session, identity(), 1, () -> 1,
          ignored -> { throw new IllegalStateException("dispatch"); }, ignored -> true,
          (ignored, value) -> fail(), (ignored, failure) -> fail(),
          (ignored, failure) -> abandoned.countDown());
      assertTrue(abandoned.await(2, TimeUnit.SECONDS));
      assertEquals(ClientFileCheckTaskCoordinator.TaskState.DISPATCH_FAILED, token.state());
      var done = new CountDownLatch(1);
      assertNotNull(coordinator.submit(session, identity(), 2, () -> 2, Runnable::run,
          ignored -> true, (ignored, value) -> done.countDown(),
          (ignored, failure) -> fail(), (ignored, failure) -> fail()));
      assertTrue(done.await(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void queuedCallbackRuntimeFailureAbandonsAndTerminates() throws Exception {
    try (var coordinator = new ClientFileCheckTaskCoordinator()) {
      var session = coordinator.beginSession(new Object(), UUID.randomUUID());
      var abandoned = new CountDownLatch(1);
      var token = coordinator.submit(session, identity(), 1, () -> 1, Runnable::run,
          ignored -> true, (ignored, value) -> { throw new IllegalStateException("callback"); },
          (ignored, failure) -> fail(), (ignored, failure) -> abandoned.countDown());
      assertTrue(abandoned.await(2, TimeUnit.SECONDS));
      assertEquals(ClientFileCheckTaskCoordinator.TaskState.FAILED, token.state());
    }
  }

  private static ClientFileCheckTaskCoordinator.RequestIdentity identity() {
    return new ClientFileCheckTaskCoordinator.RequestIdentity(
        UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS);
  }
}
