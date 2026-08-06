package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferOutgoingTest {
  @TempDir Path game;

  @Test
  void storesCurrentSessionIndependentlyAndRejectsSelfComparisonRegression() {
    CheckedFileTransferOutgoing outgoing = new CheckedFileTransferOutgoing();
    ClientFileCheckTaskCoordinator.Session session = session(1, new Object(), UUID.randomUUID());
    CheckedFileTransferRequestMessage request = request(session.localPlayerId(), UUID.randomUUID());

    assertEquals(CheckedFileTransferOutgoing.BeginResult.INVALID_SESSION,
        outgoing.receive(request, session));
    outgoing.beginSession(session);
    assertSame(session, outgoing.currentSession());
    assertEquals(CheckedFileTransferOutgoing.BeginResult.OPEN, outgoing.receive(request, session));
    outgoing.close();
  }

  @Test
  void duplicateConsentIsIdempotentAndDifferentConsentIsBusy() {
    UUID target = UUID.randomUUID();
    CheckedFileTransferOutgoing outgoing = new CheckedFileTransferOutgoing();
    ClientFileCheckTaskCoordinator.Session session = session(1, new Object(), target);
    outgoing.beginSession(session);
    CheckedFileTransferRequestMessage first = request(target, UUID.randomUUID());
    assertEquals(CheckedFileTransferOutgoing.BeginResult.OPEN, outgoing.receive(first, session));
    assertEquals(CheckedFileTransferOutgoing.BeginResult.DUPLICATE, outgoing.receive(first, session));
    assertEquals(CheckedFileTransferOutgoing.BeginResult.CONSENT_BUSY,
        outgoing.receive(request(target, UUID.randomUUID()), session));
    outgoing.close();
  }

  @Test
  void exactOldSessionInvalidationCannotClearNewSession() {
    UUID target = UUID.randomUUID();
    Object firstConnection = new Object();
    CheckedFileTransferOutgoing outgoing = new CheckedFileTransferOutgoing();
    ClientFileCheckTaskCoordinator.Session first = session(1, firstConnection, target);
    ClientFileCheckTaskCoordinator.Session second = session(2, new Object(), target);
    outgoing.beginSession(first);
    outgoing.receive(request(target, UUID.randomUUID()), first);
    outgoing.beginSession(second);
    outgoing.invalidateSession(first);
    assertSame(second, outgoing.currentSession());
    assertEquals(CheckedFileTransferOutgoing.BeginResult.OPEN,
        outgoing.receive(request(target, UUID.randomUUID()), second));
    outgoing.close();
  }

  @Test
  void declineUsesSessionGateAndCleansActiveOperation() {
    Fixture fixture = new Fixture();
    List<Object> sent = new ArrayList<>();
    assertTrue(fixture.outgoing.decline(fixture.request, fixture.session,
        (session, token, message) -> sent.add(message)));
    assertEquals(1, sent.size());
    var control = (CheckedFileTransferControlRequestMessage) sent.get(0);
    assertEquals(CheckedFileTransferControlStatus.DECLINED,
        CheckedFileTransferControlJsonCodec.decode(control.controlPayload()).status());
    assertNull(fixture.outgoing.active());
    fixture.outgoing.close();
  }

  @Test
  void logoutBeforeDeclineCannotSend() {
    Fixture fixture = new Fixture();
    fixture.outgoing.invalidateSession(fixture.session);
    AtomicInteger sent = new AtomicInteger();
    assertFalse(fixture.outgoing.decline(fixture.request, fixture.session,
        (session, token, message) -> sent.incrementAndGet()));
    assertEquals(0, sent.get());
    fixture.outgoing.close();
  }

  @Test
  void snapshotFailureSendsBoundedFailedControl() throws Exception {
    Fixture fixture = new Fixture();
    CountDownLatch sent = new CountDownLatch(1);
    List<Object> messages = java.util.Collections.synchronizedList(new ArrayList<>());
    assertTrue(fixture.outgoing.allow(
        fixture.request,
        fixture.session,
        deadline -> new CheckedFileSnapshotter.Outcome(null, "SNAPSHOT_FAILED"),
        (session, token, message) -> {
          messages.add(message);
          sent.countDown();
        }));
    assertTrue(sent.await(5, TimeUnit.SECONDS));
    var message = (CheckedFileTransferControlRequestMessage) messages.get(0);
    CheckedFileTransferControl control =
        CheckedFileTransferControlJsonCodec.decode(message.controlPayload());
    assertEquals(CheckedFileTransferControlStatus.FAILED, control.status());
    assertEquals("SNAPSHOT_FAILED", control.errorCode());
    await(() -> fixture.outgoing.active() == null);
    fixture.outgoing.close();
  }

  @Test
  void reconnectWhileSnapshotWorkerRunsCannotSendOldSession() throws Exception {
    Fixture fixture = new Fixture();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger sent = new AtomicInteger();
    assertTrue(fixture.outgoing.allow(
        fixture.request,
        fixture.session,
        deadline -> {
          started.countDown();
          release.await();
          return new CheckedFileSnapshotter.Outcome(null, "SNAPSHOT_FAILED");
        },
        (session, token, message) -> sent.incrementAndGet()));
    assertTrue(started.await(5, TimeUnit.SECONDS));
    fixture.outgoing.beginSession(session(2, new Object(), fixture.target));
    release.countDown();
    Thread.sleep(25);
    assertEquals(0, sent.get());
    fixture.outgoing.close();
  }

  @Test
  void successfulSnapshotChecksSessionBeforeReadyAndEveryChunk() throws Exception {
    Fixture fixture = new Fixture();
    byte[] bytes = "payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    CountDownLatch sent = new CountDownLatch(2);
    List<Object> messages = java.util.Collections.synchronizedList(new ArrayList<>());
    assertTrue(fixture.outgoing.allow(
        fixture.request,
        fixture.session,
        deadline -> CheckedFileTransferTestSupport.snapshot(
            game, bytes, fixture.outgoing.tempBudget()),
        (session, token, message) -> {
          messages.add(message);
          sent.countDown();
        }));
    assertTrue(sent.await(5, TimeUnit.SECONDS));
    assertTrue(messages.get(0) instanceof CheckedFileTransferControlRequestMessage);
    assertTrue(messages.get(1) instanceof CheckedFileTransferChunkRequestMessage);
    await(() -> fixture.outgoing.active() == null);
    assertEquals(0, fixture.outgoing.tempBudget().reservedFiles());
    fixture.outgoing.close();
  }

  @Test
  void sendsExactChannelInsteadOfDisplayPath() throws Exception {
    Fixture fixture = new Fixture();
    byte[] authorized = "authorized".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] decoy = "decoy-data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    CountDownLatch sent = new CountDownLatch(2);
    List<Object> messages = java.util.Collections.synchronizedList(new ArrayList<>());
    assertTrue(fixture.outgoing.allow(fixture.request, fixture.session,
        deadline -> CheckedFileTransferTestSupport.snapshot(
            game, authorized, decoy, fixture.outgoing.tempBudget()),
        (session, token, message) -> { messages.add(message); sent.countDown(); }));
    assertTrue(sent.await(5, TimeUnit.SECONDS));
    var chunk = (CheckedFileTransferChunkRequestMessage) messages.get(1);
    assertArrayEquals(authorized, java.util.Base64.getDecoder().decode(chunk.chunkData()));
    fixture.outgoing.close();
  }

  @Test
  void senderRuntimeFailureConsumesOperation() {
    Fixture fixture = new Fixture();
    assertFalse(fixture.outgoing.decline(fixture.request, fixture.session,
        (session, token, message) -> { throw new IllegalStateException("send"); }));
    assertNull(fixture.outgoing.active());
    fixture.outgoing.close();
  }

  @Test
  void senderErrorEscapesAfterOperationCleanup() {
    Fixture fixture = new Fixture();
    assertThrows(AssertionError.class,
        () -> fixture.outgoing.decline(fixture.request, fixture.session,
            (session, token, message) -> { throw new AssertionError("fatal"); }));
    assertNull(fixture.outgoing.active());
    fixture.outgoing.close();
  }

  @Test
  void timeoutInterruptsActiveWorkButKeepsCurrentSession() {
    UUID target = UUID.randomUUID();
    CheckedFileTransferOutgoing outgoing =
        new CheckedFileTransferOutgoing(new CheckedFileTransferTempBudget(), 10);
    ClientFileCheckTaskCoordinator.Session session = session(1, new Object(), target);
    outgoing.beginSession(session);
    assertEquals(CheckedFileTransferOutgoing.BeginResult.OPEN,
        outgoing.receive(request(target, UUID.randomUUID()), session, 100));
    assertTrue(outgoing.tick(110));
    assertNull(outgoing.active());
    assertSame(session, outgoing.currentSession());
    outgoing.close();
  }

  private static ClientFileCheckTaskCoordinator.Session session(
      long generation, Object connection, UUID player) {
    return new ClientFileCheckTaskCoordinator.Session(generation, connection, player);
  }

  private static CheckedFileTransferRequestMessage request(UUID target, UUID requester) {
    return new CheckedFileTransferRequestMessage(
        "Target", target, "Requester", requester, ClientFileCheckType.MODS, "mod.jar");
  }

  private static void await(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
    assertTrue(condition.getAsBoolean());
  }

  private static final class Fixture {
    final UUID target = UUID.randomUUID();
    final ClientFileCheckTaskCoordinator.Session session = session(1, new Object(), target);
    final CheckedFileTransferRequestMessage request = request(target, UUID.randomUUID());
    final CheckedFileTransferOutgoing outgoing = new CheckedFileTransferOutgoing();

    Fixture() {
      outgoing.beginSession(session);
      assertEquals(CheckedFileTransferOutgoing.BeginResult.OPEN, outgoing.receive(request, session));
    }
  }
}
