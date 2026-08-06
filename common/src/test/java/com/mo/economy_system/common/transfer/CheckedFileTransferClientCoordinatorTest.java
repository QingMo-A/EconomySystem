package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferClientCoordinatorTest {
  private static final UUID TARGET =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  @TempDir Path game;

  @Test
  void beginSessionInstallsExactlyOneSessionAndArrivalAliases() {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    Object connection = new Object();
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(connection, UUID.randomUUID());
    assertSame(session, coordinator.currentSession());
    assertSame(session, coordinator.outgoing().currentSession());
    assertSame(session, coordinator.captureArrivalSession(connection));
    Object networkConnection = new Object();
    coordinator.bindArrivalConnection(networkConnection);
    assertSame(session, coordinator.captureArrivalSession(networkConnection));
    coordinator.close();
  }

  @Test
  void queuedOldArrivalIsIgnoredAfterReconnectWithoutOpeningState() {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session old = coordinator.beginSession(new Object(), UUID.randomUUID());
    CheckedFileTransferControlResponseMessage ready = ready(old, UUID.randomUUID(), bytes("one"));
    ClientFileCheckTaskCoordinator.Session current = coordinator.beginSession(new Object(), old.localPlayerId());
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.IGNORED_STALE_SESSION,
        coordinator.control(ready, old, game, 10));
    assertNull(coordinator.incoming());
    assertSame(current, coordinator.currentSession());
    coordinator.close();
  }

  @Test
  void staleChunkCannotAdvanceCurrentIncoming() throws Exception {
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(4, 100);
    CheckedFileTransferClientCoordinator coordinator = coordinator(budget, new AtomicBoolean());
    UUID requester = UUID.randomUUID();
    ClientFileCheckTaskCoordinator.Session old = coordinator.beginSession(new Object(), requester);
    ClientFileCheckTaskCoordinator.Session current = coordinator.beginSession(new Object(), requester);
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    CheckedFileTransferControl readyControl = CheckedFileTransferControl.ready(transfer, content.length, hash(content));
    CheckedFileTransferControlResponseMessage ready = response(current, CheckedFileTransferControlJsonCodec.encode(readyControl), "mod.jar");
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.OPEN,
        coordinator.control(ready, current, game, 1));
    var staleChunk = new CheckedFileTransferChunkResponseMessage(
        "Target", TARGET, "Requester", requester, ClientFileCheckType.MODS, "mod.jar",
        transfer, 0, 1, Base64.getEncoder().encodeToString(content));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.IGNORED_STALE_SESSION,
        coordinator.chunk(staleChunk, old));
    assertEquals(0, coordinator.incoming().nextChunkIndex());
    coordinator.close();
  }

  @Test
  void completesAndPendingArtifactRejectsDifferentReady() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    openReady(coordinator, session, transfer, content, 1);
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.ACCEPTED,
        coordinator.chunk(chunk(session, transfer, content), session));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE,
        coordinator.control(complete(session, transfer, content), session, game, 3));
    var artifact = coordinator.completedArtifact();
    assertTrue(artifact != null && artifact.isPendingDecision());
    CheckedFileTransferControl other = CheckedFileTransferControl.ready(UUID.randomUUID(), 0, hash(new byte[0]));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.ARTIFACT_PENDING,
        coordinator.control(response(session, CheckedFileTransferControlJsonCodec.encode(other), "other.jar"), session, game, 4));
    assertSame(artifact, coordinator.completedArtifact());
    coordinator.close();
  }

  @Test
  void duplicateReadyForManagedArtifactIsIdempotent() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    openReady(coordinator, session, transfer, content, 1);
    coordinator.chunk(chunk(session, transfer, content), session);
    coordinator.control(complete(session, transfer, content), session, game, 3);
    CheckedFileTransferControl readyControl = CheckedFileTransferControl.ready(transfer, content.length, hash(content));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.DUPLICATE,
        coordinator.control(response(session, CheckedFileTransferControlJsonCodec.encode(readyControl), "mod.jar"), session, game, 4));
    coordinator.close();
  }

  @Test
  void saveSuccessClearsArtifactAndUsesManagedDestination() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    complete(coordinator, session, transfer, content);
    var result = coordinator.saveCompleted(game);
    assertTrue(result.success());
    assertNull(coordinator.completedArtifact());
    Path destination = game.resolve("economy_system").resolve("received-check-files")
        .resolve(TARGET.toString()).resolve("mod.jar");
    assertArrayEquals(content, Files.readAllBytes(destination));
    coordinator.close();
  }

  @Test
  void discardSuccessClearsArtifact() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    byte[] content = bytes("payload");
    complete(coordinator, session, UUID.randomUUID(), content);
    assertTrue(coordinator.discardCompleted().success());
    assertNull(coordinator.completedArtifact());
    assertEquals(0, coordinator.tempBudget().reservedFiles());
    coordinator.close();
  }

  @Test
  void failedArtifactDeleteRetainsReferenceAndRetriesOnTick() throws Exception {
    AtomicBoolean failDelete = new AtomicBoolean(true);
    CheckedFileTransferClientCoordinator coordinator = coordinator(failDelete);
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    byte[] content = bytes("payload");
    complete(coordinator, session, UUID.randomUUID(), content);
    assertFalse(coordinator.discardCompleted().success());
    assertTrue(coordinator.completedArtifact() != null);
    failDelete.set(false);
    coordinator.tick(103);
    assertNull(coordinator.completedArtifact());
    coordinator.close();
  }

  @Test
  void artifactTtlRetriesRatherThanDroppingOwnership() throws Exception {
    AtomicBoolean failDelete = new AtomicBoolean(true);
    CheckedFileTransferClientCoordinator coordinator = coordinator(failDelete, 10);
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    complete(coordinator, session, UUID.randomUUID(), bytes("payload"));
    coordinator.tick(13);
    assertTrue(coordinator.completedArtifact() != null);
    failDelete.set(false);
    coordinator.tick(23);
    assertNull(coordinator.completedArtifact());
    coordinator.close();
  }

  @Test
  void terminalControlsAreBoundedAndDoNotClearUnrelatedIncoming() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean(), 10);
    UUID requester = UUID.randomUUID();
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), requester);
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    openReady(coordinator, session, transfer, content, 1);
    CheckedFileTransferControl declined = CheckedFileTransferControl.error(
        CheckedFileTransferControlStatus.DECLINED, "DECLINED");
    CheckedFileTransferControlResponseMessage unrelated = response(
        session, CheckedFileTransferControlJsonCodec.encode(declined), "other.jar");
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.TERMINAL,
        coordinator.control(unrelated, session, game, 1));
    assertTrue(coordinator.incoming() != null);
    assertEquals(CheckedFileTransferControlStatus.DECLINED, coordinator.terminalResult().status());
    assertEquals(CheckedFileTransferControlStatus.DECLINED,
        coordinator.pollTerminalNotification().status());
    assertNull(coordinator.pollTerminalNotification());
    coordinator.tick(11);
    assertEquals("TRANSFER_EXPIRED", coordinator.terminalResult().errorCode());
    coordinator.close();
  }

  @Test
  void malformedResponseClearsExactIncomingButStaleMalformedDoesNothing() throws Exception {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    UUID requester = UUID.randomUUID();
    ClientFileCheckTaskCoordinator.Session old = coordinator.beginSession(new Object(), requester);
    byte[] content = bytes("payload");
    UUID transfer = UUID.randomUUID();
    openReady(coordinator, old, transfer, content, 1);
    CheckedFileTransferControlResponseMessage malformed = response(old, "{", "mod.jar");
    ClientFileCheckTaskCoordinator.Session current = coordinator.beginSession(new Object(), requester);
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.IGNORED_STALE_SESSION,
        coordinator.control(malformed, old, game, 2));
    assertNull(coordinator.incoming());
    assertSame(current, coordinator.currentSession());
    coordinator.close();
  }

  @Test
  void stopDoesNotRecreateOrAcceptPackets() {
    CheckedFileTransferClientCoordinator coordinator = coordinator(new AtomicBoolean());
    ClientFileCheckTaskCoordinator.Session session = coordinator.beginSession(new Object(), UUID.randomUUID());
    coordinator.close();
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.IGNORED_STALE_SESSION,
        coordinator.chunk(new CheckedFileTransferChunkResponseMessage(
            "Target", TARGET, "Requester", session.localPlayerId(), ClientFileCheckType.MODS,
            "mod.jar", UUID.randomUUID(), 0, 1, "YQ=="), session));
  }

  private CheckedFileTransferClientCoordinator coordinator(AtomicBoolean failDelete) {
    return coordinator(new CheckedFileTransferTempBudget(8, 100), failDelete, 100);
  }

  private CheckedFileTransferClientCoordinator coordinator(
      CheckedFileTransferTempBudget budget, AtomicBoolean failDelete) {
    return coordinator(budget, failDelete, 100);
  }

  private CheckedFileTransferClientCoordinator coordinator(AtomicBoolean failDelete, long ttl) {
    return coordinator(new CheckedFileTransferTempBudget(8, 100), failDelete, ttl);
  }

  private CheckedFileTransferClientCoordinator coordinator(
      CheckedFileTransferTempBudget budget, AtomicBoolean failDelete, long ttl) {
    return new CheckedFileTransferClientCoordinator(
        budget, ttl, path -> CheckedFileTransferTestSupport.open(path, failDelete));
  }

  private void openReady(
      CheckedFileTransferClientCoordinator coordinator,
      ClientFileCheckTaskCoordinator.Session session,
      UUID transfer,
      byte[] content,
      long now) {
    CheckedFileTransferControl ready = CheckedFileTransferControl.ready(transfer, content.length, hash(content));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.OPEN,
        coordinator.control(response(session, CheckedFileTransferControlJsonCodec.encode(ready), "mod.jar"), session, game, now));
  }

  private void complete(
      CheckedFileTransferClientCoordinator coordinator,
      ClientFileCheckTaskCoordinator.Session session,
      UUID transfer,
      byte[] content) throws Exception {
    openReady(coordinator, session, transfer, content, 1);
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.ACCEPTED,
        coordinator.chunk(chunk(session, transfer, content), session));
    assertEquals(CheckedFileTransferClientCoordinator.IncomingResult.COMPLETE,
        coordinator.control(complete(session, transfer, content), session, game, 3));
  }

  private CheckedFileTransferControlResponseMessage complete(
      ClientFileCheckTaskCoordinator.Session session, UUID transfer, byte[] content) {
    CheckedFileTransferControl complete = CheckedFileTransferControl.complete(transfer, content.length, hash(content));
    return response(session, CheckedFileTransferControlJsonCodec.encode(complete), "mod.jar");
  }

  private static CheckedFileTransferChunkResponseMessage chunk(
      ClientFileCheckTaskCoordinator.Session session, UUID transfer, byte[] content) {
    return new CheckedFileTransferChunkResponseMessage(
        "Target", TARGET, "Requester", session.localPlayerId(), ClientFileCheckType.MODS,
        "mod.jar", transfer, 0, 1, Base64.getEncoder().encodeToString(content));
  }

  private static CheckedFileTransferControlResponseMessage response(
      ClientFileCheckTaskCoordinator.Session session, String payload, String fileName) {
    return new CheckedFileTransferControlResponseMessage(
        "Target", TARGET, "Requester", session.localPlayerId(), ClientFileCheckType.MODS,
        fileName, payload);
  }

  private static CheckedFileTransferControlResponseMessage ready(
      ClientFileCheckTaskCoordinator.Session session, UUID transfer, byte[] content) {
    return response(session, CheckedFileTransferControlJsonCodec.encode(
        CheckedFileTransferControl.ready(transfer, content.length, hash(content))), "mod.jar");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String hash(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void assertArrayEquals(byte[] expected, byte[] actual) {
    org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
  }
}
