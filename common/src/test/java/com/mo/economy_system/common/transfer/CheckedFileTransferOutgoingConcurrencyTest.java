package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferOutgoingConcurrencyTest {
  @TempDir Path game;

  @Test
  void senderRunsOutsideMonitorSoTickCanComplete() throws Exception {
    CheckedFileTransferOutgoing outgoing = new CheckedFileTransferOutgoing();
    UUID target = UUID.randomUUID();
    var session = new ClientFileCheckTaskCoordinator.Session(1, new Object(), target);
    var request = new CheckedFileTransferRequestMessage("Target", target, "Requester",
        UUID.randomUUID(), ClientFileCheckType.MODS, "mod.jar");
    outgoing.beginSession(session);
    long now = System.nanoTime();
    outgoing.receive(request, session, now);
    CountDownLatch senderEntered = new CountDownLatch(1);
    CountDownLatch tickFinished = new CountDownLatch(1);
    CountDownLatch releaseSender = new CountDownLatch(1);
    assertTrue(outgoing.allow(request, session,
        deadline -> CheckedFileTransferTestSupport.snapshot(game, "payload".getBytes(),
            outgoing.tempBudget()),
        (activeSession, token, message) -> {
          senderEntered.countDown();
          if (!releaseSender.await(5, TimeUnit.SECONDS)) throw new AssertionError("release");
        }, now));
    assertTrue(senderEntered.await(5, TimeUnit.SECONDS));
    Thread tick = new Thread(() -> { outgoing.tick(System.nanoTime()); tickFinished.countDown(); });
    tick.start();
    assertTrue(tickFinished.await(1, TimeUnit.SECONDS));
    releaseSender.countDown();
    tick.join(5000);
    outgoing.close();
  }
}
