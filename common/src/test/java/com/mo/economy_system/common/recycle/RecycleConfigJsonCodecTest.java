package com.mo.economy_system.common.recycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecycleConfigJsonCodecTest {
  @Test
  void roundTripsDefaultsAndCreatesConfigFile() throws Exception {
    Path directory = Files.createTempDirectory("recycle-config");
    try {
      Path path = directory.resolve("config/economysystem/recycle.json");
      RecycleConfig first = RecycleConfigLoader.loadOrCreate(path);
      RecycleConfig second = RecycleConfigJsonCodec.decode(RecycleConfigJsonCodec.encode(first));
      assertEquals(first, second);
      assertEquals(first, RecycleConfigLoader.loadOrCreate(path));
    } finally {
      deleteTree(directory);
    }
  }

  @Test
  void unsupportedSchemaFailsFast() {
    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> RecycleConfigJsonCodec.decode("{\"schema\":2,\"cycleMillis\":3600000,\"offers\":[]}"));
    assertEquals(true, failure.getMessage().contains("unsupported recycle config schema"));
  }

  private static void deleteTree(Path root) throws Exception {
    try (var paths = Files.walk(root)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
        try { Files.deleteIfExists(path); } catch (Exception failure) { throw new RuntimeException(failure); }
      });
    }
  }
}
