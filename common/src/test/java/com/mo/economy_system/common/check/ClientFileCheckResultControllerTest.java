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
    assertFalse(controller.apply(generation, remote));
    assertTrue(controller.rows().isEmpty());
  }

  @Test
  void filtersWithoutChangingStoredNames() {
    var remote = success("Alpha.jar", "0".repeat(64));
    var controller = new ClientFileCheckResultController(remote);
    assertTrue(controller.apply(controller.generation(), remote));
    assertEquals("Alpha.jar", controller.filtered("alpha").get(0).fileName());
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
