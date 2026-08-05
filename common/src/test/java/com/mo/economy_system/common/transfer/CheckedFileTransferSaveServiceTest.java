package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mo.economy_system.common.check.ClientFileCheckType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferSaveServiceTest {
  @TempDir Path gameDirectory;

  @Test
  void savesOriginalNameAndUsesNoReplaceCollisionNaming() throws Exception {
    Path firstPart = gameDirectory.resolve("first.part");
    Files.write(firstPart, new byte[] {1});
    UUID targetId = UUID.randomUUID();
    CheckedFileTransferSaveService service = new CheckedFileTransferSaveService(gameDirectory);
    CheckedFileTransferReceivedArtifact first = artifact(firstPart, targetId, 1);
    CheckedFileTransferSaveService.Result firstResult = service.save(first);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVED, firstResult.code());
    assertTrue(firstResult.savedPath().getFileName().toString().equals("mod.jar"));

    Path secondPart = gameDirectory.resolve("second.part");
    Files.write(secondPart, new byte[] {2});
    CheckedFileTransferReceivedArtifact second = artifact(secondPart, targetId, 1);
    CheckedFileTransferSaveService.Result secondResult = service.save(second);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVED, secondResult.code());
    assertEquals("mod-1.jar", secondResult.savedPath().getFileName().toString());
    assertArrayEquals(new byte[] {1}, Files.readAllBytes(firstResult.savedPath()));
  }

  @Test
  void boundedExhaustionLeavesArtifactPending() throws Exception {
    UUID targetId = UUID.randomUUID();
    CheckedFileTransferSaveService service =
        new CheckedFileTransferSaveService(gameDirectory, 2);
    Path firstPart = gameDirectory.resolve("first.part");
    Files.write(firstPart, new byte[] {1});
    CheckedFileTransferReceivedArtifact first = artifact(firstPart, targetId, 1);
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVED, service.save(first).code());

    Path secondPart = gameDirectory.resolve("second.part");
    Files.write(secondPart, new byte[] {2});
    CheckedFileTransferReceivedArtifact second = artifact(secondPart, targetId, 1);
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVED, service.save(second).code());

    Path thirdPart = gameDirectory.resolve("third.part");
    Files.write(thirdPart, new byte[] {3});
    CheckedFileTransferReceivedArtifact third = artifact(thirdPart, targetId, 1);
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVE_NAME_EXHAUSTED,
        service.save(third).code());
    assertEquals(CheckedFileTransferReceivedArtifact.State.PENDING_DECISION, third.state());
    assertTrue(Files.exists(third.path()));
  }

  @Test
  void rejectsSymlinkParentWithoutMovingArtifact() throws Exception {
    Path outside = Files.createDirectory(gameDirectory.resolve("outside"));
    Path link = gameDirectory.resolve("economy_system");
    try {
      Files.createSymbolicLink(link, outside);
    } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
      assumeTrue(false, "symbolic links unavailable");
      return;
    }
    Path part = gameDirectory.resolve("symlink.part");
    Files.write(part, new byte[] {1});
    CheckedFileTransferReceivedArtifact artifact = artifact(part, UUID.randomUUID(), 1);
    CheckedFileTransferSaveService.Result result =
        new CheckedFileTransferSaveService(gameDirectory).save(artifact);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVE_PARENT_UNSAFE, result.code());
    assertEquals(CheckedFileTransferReceivedArtifact.State.PENDING_DECISION, artifact.state());
    assertTrue(Files.exists(part));
  }

  private CheckedFileTransferReceivedArtifact artifact(Path part, UUID targetId, long size) {
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(2, 10);
    CheckedFileTransferTempBudget.Reservation reservation = budget.reserve(size);
    return new CheckedFileTransferReceivedArtifact(
        part,
        reservation,
        new CheckedFileTransferReceivedArtifact.Metadata(
            "Target",
            targetId,
            "Requester",
            UUID.randomUUID(),
            ClientFileCheckType.MODS,
            "mod.jar",
            UUID.randomUUID(),
            size,
            "0000000000000000000000000000000000000000000000000000000000000000",
            CheckedFileTransferValidation.totalChunks(
                size,
                com.mo.economy_system.common.network.EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES)));
  }
}
