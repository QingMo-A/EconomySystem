package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ClientFileCheckExecutorTest {
  @Test
  void rejectsConcurrentWorkAndCancelsActiveScan() throws Exception {
    try (var executor = new ClientFileCheckExecutor()) {
      CountDownLatch started = new CountDownLatch(1);
      CountDownLatch interrupted = new CountDownLatch(1);
      assertTrue(
          executor.submit(
              () -> {
                started.countDown();
                try {
                  Thread.sleep(30_000);
                } catch (InterruptedException expected) {
                  interrupted.countDown();
                  Thread.currentThread().interrupt();
                }
              }));
      assertTrue(started.await(2, TimeUnit.SECONDS));
      assertFalse(executor.submit(() -> fail("must not queue a second scan")));
      executor.cancelPending();
      assertTrue(interrupted.await(2, TimeUnit.SECONDS));
    }
  }
}
