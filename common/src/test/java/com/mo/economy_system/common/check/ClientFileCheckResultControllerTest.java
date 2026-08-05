package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientFileCheckResultControllerTest {
  @Test
  void declinedAndFailedNeverRequireLocalComparison() {
    assertFalse(
        new ClientFileCheckResultController(
                ClientFileCheckResult.declined(ClientFileCheckType.MODS))
            .needsComparison());
    assertFalse(
        new ClientFileCheckResultController(
                ClientFileCheckResult.failed(ClientFileCheckType.MODS, "SCAN_FAILED"))
            .needsComparison());
  }

  @Test
  void invalidatedGenerationRejectsLateCompletion() {
    var remote = success("a.jar", "0".repeat(64));
    var controller = new ClientFileCheckResultController(remote);
    long generation = controller.generation();
    controller.invalidate();
    assertEquals(
        ClientFileCheckResultController.LocalApplyOutcome.STALE,
        controller.acceptLocalResult(generation, remote));
    assertTrue(controller.rows().isEmpty());
  }

  @Test
  void filtersWithoutChangingStoredNames() {
    var remote = success("Alpha.jar", "0".repeat(64));
    var controller = new ClientFileCheckResultController(remote);
    assertEquals(
        ClientFileCheckResultController.LocalApplyOutcome.APPLIED,
        controller.acceptLocalResult(controller.generation(), remote));
    assertEquals("Alpha.jar", controller.filtered("alpha").get(0).fileName());
  }

  @Test
  void skippedRowsAreMappedAndSearchableWithoutChangingFilename() {
    var remote =
        new ClientFileCheckResult(
            1,
            ClientFileCheckStatus.TRUNCATED,
            ClientFileCheckType.MODS,
            List.of(),
            List.of(new ClientFileCheckSkippedEntry("Skipped.jar", "SYMLINK")),
            "FILE_LIMIT");
    var controller = new ClientFileCheckResultController(remote);
    var row = controller.filtered("skipped").get(0);
    assertEquals("Skipped.jar", row.fileName());
    assertEquals("symlink", row.reasonId());
    assertEquals(ClientFileCheckResultController.RowType.SKIPPED, row.type());
  }

  @Test
  void retryAdvancesGenerationAndRejectsOldCallback() {
    var remote = success("a.jar", "0".repeat(64));
    var controller = new ClientFileCheckResultController(remote);
    long old = controller.generation();
    controller.failed(old);
    long retry = controller.retry();
    assertTrue(retry > old);
    assertEquals(ClientFileCheckResultController.LocalState.LOADING, controller.localState());
    assertEquals(ClientFileCheckResultController.LocalApplyOutcome.STALE,
        controller.acceptLocalResult(old, remote));
    assertEquals(ClientFileCheckResultController.LocalApplyOutcome.APPLIED,
        controller.acceptLocalResult(retry, remote));
  }

  @Test
  void failedLocalHasNoComparisonAndPreservesError() {
    var controller = new ClientFileCheckResultController(success("remote.jar", "0".repeat(64)));
    var local = ClientFileCheckResult.failed(ClientFileCheckType.MODS, "DIRECTORY_PROVIDER_UNSAFE");
    assertEquals(ClientFileCheckResultController.LocalApplyOutcome.FAILED,
        controller.acceptLocalResult(controller.generation(), local));
    assertEquals(ClientFileCheckResultController.LocalState.FAILED, controller.localState());
    assertEquals("DIRECTORY_PROVIDER_UNSAFE", controller.localErrorCode());
    assertTrue(controller.rows().isEmpty());
  }

  @Test
  void truncatedLocalIsExplicitlyIncompleteAndRetryable() {
    var controller = new ClientFileCheckResultController(success("remote.jar", "0".repeat(64)));
    var local = new ClientFileCheckResult(1, ClientFileCheckStatus.TRUNCATED,
        ClientFileCheckType.MODS, List.of(),
        List.of(new ClientFileCheckSkippedEntry("local.jar", "FILE_TOO_LARGE")), "FILE_LIMIT");
    assertEquals(ClientFileCheckResultController.LocalApplyOutcome.INCOMPLETE,
        controller.acceptLocalResult(controller.generation(), local));
    assertEquals(ClientFileCheckResultController.LocalState.READY_INCOMPLETE, controller.localState());
    assertEquals("FILE_LIMIT", controller.localErrorCode());
    assertTrue(controller.rows().stream().anyMatch(row -> row.fileName().equals("local.jar")));
    assertTrue(controller.retry() > 0);
  }

  @Test
  void declinedAndTypeMismatchFailAsInvalidLocalResult() {
    var controller = new ClientFileCheckResultController(success("a.jar", "0".repeat(64)));
    controller.acceptLocalResult(controller.generation(), ClientFileCheckResult.declined(ClientFileCheckType.MODS));
    assertEquals("INVALID_LOCAL_RESULT", controller.localErrorCode());
    long retry = controller.retry();
    controller.acceptLocalResult(retry, ClientFileCheckResult.failed(ClientFileCheckType.SHADERPACKS, "X"));
    assertEquals("INVALID_LOCAL_RESULT", controller.localErrorCode());
  }

  private static ClientFileCheckResult success(String name, String hash) {
    return new ClientFileCheckResult(
        1,
        ClientFileCheckStatus.SUCCESS,
        ClientFileCheckType.MODS,
        List.of(new ClientFileCheckEntry(name, 1, hash)),
        List.of(),
        null);
  }
}
