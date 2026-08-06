package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferIncomingTest {
  @TempDir Path game;

  @Test
  void createsRelativePartAndCompletesIntoManagedArtifact() throws Exception {
    byte[] bytes = "incoming".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    UUID target = UUID.randomUUID();
    UUID requester = UUID.randomUUID();
    UUID transfer = UUID.randomUUID();
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(game);
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(2, 100);
    ClientFileCheckTaskCoordinator.Session session =
        new ClientFileCheckTaskCoordinator.Session(1, new Object(), requester);
    CheckedFileTransferIncoming.Metadata metadata =
        metadata(target, requester, transfer, bytes.length, sha256(bytes));
    CheckedFileTransferIncoming incoming =
        new CheckedFileTransferIncoming(metadata, session, temp, budget, System.nanoTime() + 1_000_000_000L);

    assertFalse(incoming.path().getFileName().toString().isEmpty());
    assertEquals(1, incoming.relativeName().getNameCount());
    String encoded = Base64.getEncoder().encodeToString(bytes);
    incoming.chunk(
        new CheckedFileTransferChunkResponseMessage(
            "Target", target, "Requester", requester, ClientFileCheckType.MODS, "mod.jar",
            transfer, 0, metadata.totalChunks(), encoded),
        session);
    CheckedFileTransferControl control =
        CheckedFileTransferControl.complete(transfer, bytes.length, sha256(bytes));
    CheckedFileTransferReceivedArtifact artifact =
        incoming.completeArtifact(
            new CheckedFileTransferControlResponseMessage(
                "Target", target, "Requester", requester, ClientFileCheckType.MODS, "mod.jar", ""),
            control,
            session);

    assertEquals(CheckedFileTransferIncoming.State.COMPLETED, incoming.state());
    assertEquals(CheckedFileTransferReceivedArtifact.State.PENDING_DECISION, artifact.state());
    assertArrayEquals(bytes, Files.readAllBytes(artifact.path()));
    assertTrue(artifact.discard());
    assertEquals(0, budget.reservedFiles());
    temp.close();
  }

  @Test
  void malformedChunkAbortsAndReleasesExactReservation() throws Exception {
    UUID transfer = UUID.randomUUID();
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(game);
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferIncoming incoming =
        new CheckedFileTransferIncoming(
            metadata(UUID.randomUUID(), UUID.randomUUID(), transfer, 1, sha256(new byte[] {1})),
            null,
            temp,
            budget,
            System.nanoTime() + 1_000_000_000L);
    assertThrows(
        IOException.class, () -> incoming.chunk(transfer, 0, 1, "%%%"));
    assertEquals(CheckedFileTransferIncoming.State.ABORTED, incoming.state());
    assertEquals(0, budget.reservedFiles());
    assertFalse(Files.exists(incoming.path()));
    temp.close();
  }

  @Test
  void staleSessionIsTerminalAndCannotAppend() throws Exception {
    UUID requester = UUID.randomUUID();
    UUID transfer = UUID.randomUUID();
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(game);
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    ClientFileCheckTaskCoordinator.Session first =
        new ClientFileCheckTaskCoordinator.Session(1, new Object(), requester);
    ClientFileCheckTaskCoordinator.Session second =
        new ClientFileCheckTaskCoordinator.Session(2, new Object(), requester);
    CheckedFileTransferIncoming.Metadata metadata =
        metadata(UUID.randomUUID(), requester, transfer, 1, sha256(new byte[] {1}));
    CheckedFileTransferIncoming incoming =
        new CheckedFileTransferIncoming(metadata, first, temp, budget, System.nanoTime() + 1_000_000_000L);
    CheckedFileTransferChunkResponseMessage message =
        new CheckedFileTransferChunkResponseMessage(
            "Target", metadata.targetPlayerId(), "Requester", requester, ClientFileCheckType.MODS,
            "mod.jar", transfer, 0, 1, Base64.getEncoder().encodeToString(new byte[] {1}));
    assertThrows(IOException.class, () -> incoming.chunk(message, second));
    assertEquals(CheckedFileTransferIncoming.State.ABORTED, incoming.state());
    assertEquals(0, budget.reservedFiles());
    temp.close();
  }

  @Test
  void timeoutDeletesPartAndReleasesBudget() throws Exception {
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(game);
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferIncoming incoming =
        new CheckedFileTransferIncoming(
            metadata(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, sha256(new byte[0])),
            null,
            temp,
            budget,
            10);
    Path path = incoming.path();
    assertTrue(incoming.expire(10));
    assertFalse(Files.exists(path));
    assertEquals(0, budget.reservedFiles());
    temp.close();
  }

  @Test
  void deleteFailureRetainsReservationForRetry() throws Exception {
    AtomicBoolean failDelete = new AtomicBoolean(true);
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(game, failDelete);
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferIncoming incoming =
        new CheckedFileTransferIncoming(
            metadata(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, sha256(new byte[0])),
            null,
            temp,
            budget,
            10);
    incoming.expire(10);
    assertEquals(1, budget.reservedFiles());
    failDelete.set(false);
    incoming.close();
    assertEquals(0, budget.reservedFiles());
    temp.close();
  }

  private static CheckedFileTransferIncoming.Metadata metadata(
      UUID target, UUID requester, UUID transfer, long size, String hash) {
    return new CheckedFileTransferIncoming.Metadata(
        "Target", target, "Requester", requester, ClientFileCheckType.MODS, "mod.jar", transfer,
        size, hash, CheckedFileTransferValidation.totalChunks(
            size, com.mo.economy_system.common.network.EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES));
  }

  private static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
