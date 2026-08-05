package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientFileCheckScannerTest {
  @TempDir Path game;

  @Test
  void scansOnlyDirectRegularFilesInStableOrder() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("b.jar"), "b");
    Files.writeString(mods.resolve("a.jar"), "abc");
    Files.createDirectories(mods.resolve("nested"));
    Files.writeString(mods.resolve("nested/x.jar"), "x");
    ClientFileCheckResult result =
        new ClientFileCheckScanner().scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.SUCCESS, result.status());
    assertEquals(
        List.of("a.jar", "b.jar"),
        result.files().stream().map(ClientFileCheckEntry::fileName).toList());
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        result.files().get(0).sha256());
    assertTrue(result.skipped().stream().anyMatch(value -> value.fileName().equals("nested")));
    assertFalse(ClientFileCheckResultJsonCodec.encode(result).contains(game.toString()));
  }

  @Test
  void missingRootIsEmptyAndRootsAreFixed() {
    var scanner = new ClientFileCheckScanner();
    assertTrue(scanner.scan(game, ClientFileCheckType.RESOURCEPACKS).files().isEmpty());
    assertEquals(
        game.toAbsolutePath().normalize().resolve("mods"),
        scanner.directory(game, ClientFileCheckType.MODS));
    assertEquals(
        game.toAbsolutePath().normalize().resolve("shaderpacks"),
        scanner.directory(game, ClientFileCheckType.SHADERPACKS));
  }

  @Test
  void reportsFileAndTotalLimits() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("a"), "1234");
    Files.writeString(mods.resolve("b"), "12");
    var perFile =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 2, 10, 1_000_000_000), System::nanoTime);
    assertEquals(
        "FILE_TOO_LARGE", perFile.scan(game, ClientFileCheckType.MODS).skipped().get(0).reason());
    var total =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 4, 1_000_000_000), System::nanoTime);
    assertEquals(
        ClientFileCheckStatus.TRUNCATED, total.scan(game, ClientFileCheckType.MODS).status());
    var count =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(1, 10, 10, 10, 1_000_000_000), System::nanoTime);
    assertEquals("FILE_LIMIT", count.scan(game, ClientFileCheckType.MODS).errorCode());
  }

  @Test
  void reportsTimeLimit() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("a"), "a");
    long[] now = {0};
    var scanner =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 10, 1), () -> now[0] += 2);
    ClientFileCheckResult result = scanner.scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.TRUNCATED, result.status());
    assertEquals("TIME_LIMIT", result.errorCode());
  }
}
