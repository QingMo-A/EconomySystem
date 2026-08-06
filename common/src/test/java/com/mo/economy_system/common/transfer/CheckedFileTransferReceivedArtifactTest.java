package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferReceivedArtifactTest {
  @TempDir Path temporaryDirectory;

  @Test
  void discardDeletesPartAndReleasesReservation() throws Exception {
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(temporaryDirectory);
    CheckedFileTransferTempDirectory.OwnedFile part =
        CheckedFileTransferTestSupport.part(temp, new byte[] {1, 2, 3});
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferTempBudget.Reservation reservation = budget.reserve(3);
    CheckedFileTransferReceivedArtifact artifact =
        new CheckedFileTransferReceivedArtifact(part, reservation, metadata(3));

    assertTrue(artifact.discard());
    assertEquals(CheckedFileTransferReceivedArtifact.State.DISCARDED, artifact.state());
    assertFalse(Files.exists(part.path()));
    assertEquals(0, budget.reservedFiles());
    assertFalse(artifact.discard());
    temp.close();
  }

  @Test
  void saveTransitionIsTerminalAndMetadataIsRetained() throws Exception {
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(temporaryDirectory);
    CheckedFileTransferTempDirectory.OwnedFile part =
        CheckedFileTransferTestSupport.part(temp, new byte[] {1, 2, 3});
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferTempBudget.Reservation reservation = budget.reserve(3);
    CheckedFileTransferReceivedArtifact artifact =
        new CheckedFileTransferReceivedArtifact(part, reservation, metadata(3));

    CheckedFileTransferTempDirectory.DirectoryHandle target =
        temp.openTargetDirectory(UUID.randomUUID());
    Path destination = target.absolutePath().resolve("saved.jar");
    assertEquals(
        CheckedFileTransferReceivedArtifact.MoveResult.MOVED,
        artifact.moveTo(target, Path.of("saved.jar"), destination));
    target.close();
    assertEquals(CheckedFileTransferReceivedArtifact.State.SAVED, artifact.state());
    assertEquals(destination.toAbsolutePath().normalize(), artifact.path());
    assertEquals("mod.jar", artifact.metadata().fileName());
    assertEquals(0, budget.reservedFiles());
    assertFalse(artifact.discard());
    temp.close();
  }

  @Test
  void mismatchedReservationIsRejectedAndReleased() throws Exception {
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(temporaryDirectory);
    CheckedFileTransferTempDirectory.OwnedFile part =
        CheckedFileTransferTestSupport.part(temp, new byte[] {1, 2, 3});
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, 10);
    CheckedFileTransferTempBudget.Reservation reservation = budget.reserve(2);
    assertThrows(
        IllegalArgumentException.class,
        () -> new CheckedFileTransferReceivedArtifact(part, reservation, metadata(3)));
    assertEquals(0, budget.reservedFiles());
    temp.close();
  }

  private static CheckedFileTransferReceivedArtifact.Metadata metadata(long size) {
    return new CheckedFileTransferReceivedArtifact.Metadata(
        "Target",
        UUID.randomUUID(),
        "Requester",
        UUID.randomUUID(),
        ClientFileCheckType.MODS,
        "mod.jar",
        UUID.randomUUID(),
        size,
        "0000000000000000000000000000000000000000000000000000000000000000",
        CheckedFileTransferValidation.totalChunks(
            size, com.mo.economy_system.common.network.EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES));
  }
}
