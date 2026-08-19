package com.mo.economy_system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Keeps the Forge and NeoForge public entry facades source-compatible for third-party mods. */
class PublicApiFacadeParitySourceTest {
  @Test
  void targetFacadesExposeTheSamePublicStaticSurface() throws IOException {
    Path root = repositoryRoot();
    String neo = Files.readString(root.resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/api/EconomySystemApi.java"),
        StandardCharsets.UTF_8);
    String forge = Files.readString(root.resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/api/EconomySystemApi.java"),
        StandardCharsets.UTF_8);

    assertTrue(neo.contains("package com.mo.economy_system.api;"));
    assertTrue(forge.contains("package com.mo.economy_system.api;"));
    assertEquals(publicStaticLines(neo), publicStaticLines(forge));
  }

  private static List<String> publicStaticLines(String source) {
    return source.lines()
        .map(String::trim)
        .filter(line -> line.startsWith("public static "))
        .toList();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}
