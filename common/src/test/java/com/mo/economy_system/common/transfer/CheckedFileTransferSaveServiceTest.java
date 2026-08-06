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
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(gameDirectory);
    UUID targetId = UUID.randomUUID();
    CheckedFileTransferSaveService service = new CheckedFileTransferSaveService(gameDirectory);
    CheckedFileTransferReceivedArtifact first = artifact(temp, targetId, 1, new byte[] {1});
    CheckedFileTransferSaveService.Result firstResult = service.save(first);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVED, firstResult.code());
    assertTrue(firstResult.savedPath().getFileName().toString().equals("mod.jar"));

    CheckedFileTransferReceivedArtifact second = artifact(temp, targetId, 1, new byte[] {2});
    CheckedFileTransferSaveService.Result secondResult = service.save(second);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVED, secondResult.code());
    assertEquals("mod-1.jar", secondResult.savedPath().getFileName().toString());
    assertArrayEquals(new byte[] {1}, Files.readAllBytes(firstResult.savedPath()));
    temp.close();
  }

  @Test
  void boundedExhaustionLeavesArtifactPending() throws Exception {
    UUID targetId = UUID.randomUUID();
    CheckedFileTransferSaveService service =
        new CheckedFileTransferSaveService(gameDirectory, 2);
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(gameDirectory);
    CheckedFileTransferReceivedArtifact first = artifact(temp, targetId, 1, new byte[] {1});
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVED, service.save(first).code());

    CheckedFileTransferReceivedArtifact second = artifact(temp, targetId, 1, new byte[] {2});
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVED, service.save(second).code());

    CheckedFileTransferReceivedArtifact third = artifact(temp, targetId, 1, new byte[] {3});
    assertEquals(
        CheckedFileTransferSaveService.ResultCode.SAVE_NAME_EXHAUSTED,
        service.save(third).code());
    assertEquals(CheckedFileTransferReceivedArtifact.State.PENDING_DECISION, third.state());
    assertTrue(Files.exists(third.path()));
    third.discard();
    temp.close();
  }

  @Test
  void rejectsSymlinkParentWithoutMovingArtifact() throws Exception {
    CheckedFileTransferTempDirectory temp = CheckedFileTransferTestSupport.open(gameDirectory);
    CheckedFileTransferTempDirectory.OwnedFile part =
        CheckedFileTransferTestSupport.part(temp, new byte[] {1});
    Path outside = Files.createDirectory(gameDirectory.resolve("outside"));
    Path link = gameDirectory.resolve("economy_system").resolve("received-check-files");
    try {
      Files.createSymbolicLink(link, outside);
    } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
      part.delete();
      temp.close();
      assumeTrue(false, "symbolic links unavailable");
      return;
    }
    CheckedFileTransferReceivedArtifact artifact = artifact(UUID.randomUUID(), 1, part);
    CheckedFileTransferSaveService.Result result =
        new CheckedFileTransferSaveService(gameDirectory).save(artifact);
    assertEquals(CheckedFileTransferSaveService.ResultCode.SAVE_PARENT_UNSAFE, result.code());
    assertEquals(CheckedFileTransferReceivedArtifact.State.PENDING_DECISION, artifact.state());
    assertTrue(Files.exists(part.path()));
    artifact.discard();
    temp.close();
  }

  private CheckedFileTransferReceivedArtifact artifact(
      CheckedFileTransferTempDirectory temp, UUID targetId, long size, byte[] bytes)
      throws IOException {
    return artifact(targetId, size, CheckedFileTransferTestSupport.part(temp, bytes));
  }

  private CheckedFileTransferReceivedArtifact artifact(
      UUID targetId,
      long size,
      CheckedFileTransferTempDirectory.OwnedFile part) {
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
