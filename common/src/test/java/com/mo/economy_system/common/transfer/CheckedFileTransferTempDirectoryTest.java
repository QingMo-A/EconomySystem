package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileTransferTempDirectoryTest {
  @TempDir Path game;

  @Test
  void createsUuidPartAndDeletesThroughRelativeOwnership() throws Exception {
    CheckedFileTransferTempDirectory directory = CheckedFileTransferTestSupport.open(game);
    CheckedFileTransferTempDirectory.OwnedFile file = directory.createPart();

    assertEquals(
        CheckedFileTransferTempDirectory.expectedPath(game), directory.path());
    assertFalse(file.relativeName().isAbsolute());
    assertEquals(1, file.relativeName().getNameCount());
    assertTrue(file.relativeName().toString().matches("[0-9a-f-]{36}\\.part"));
    assertTrue(Files.exists(file.path()));

    directory.close();
    assertTrue(file.delete(), "an outstanding lease keeps the secure handle usable");
    assertFalse(Files.exists(file.path()));
  }

  @Test
  void rejectsNonFixedLegacyRoot() {
    IOException failure =
        assertThrows(
            IOException.class,
            () -> CheckedFileTransferTempDirectory.openFixedRoot(game.resolve("other")));
    assertEquals(CheckedFileTransferTempDirectory.PROVIDER_UNSAFE, failure.getMessage());
  }

  @Test
  void unsupportedProviderFailsClosedWithStableCode() {
    CheckedFileTransferTempDirectory.ProviderUnsafeException failure =
        assertThrows(
            CheckedFileTransferTempDirectory.ProviderUnsafeException.class,
            () ->
                CheckedFileTransferTempDirectory.open(
                    game,
                    (ignoredGame, ignoredChildren) -> {
                      throw new CheckedFileTransferTempDirectory.ProviderUnsafeException();
                    }));
    assertEquals("TEMP_DIRECTORY_PROVIDER_UNSAFE", failure.errorCode());
  }

  @Test
  void symlinkManagedParentFailsClosed() throws Exception {
    Path outside = Files.createDirectory(game.resolve("outside"));
    Path economy = game.resolve("economy_system");
    try {
      Files.createSymbolicLink(economy, outside);
    } catch (UnsupportedOperationException | IOException | SecurityException unsupported) {
      return;
    }

    CheckedFileTransferTempDirectory.ProviderUnsafeException failure =
        assertThrows(
            CheckedFileTransferTempDirectory.ProviderUnsafeException.class,
            () -> CheckedFileTransferTempDirectory.open(game));
    assertEquals("TEMP_DIRECTORY_PROVIDER_UNSAFE", failure.getMessage());
  }
}
