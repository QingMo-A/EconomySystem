package com.mo.economy_system.common.commission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommissionCatalogConfigLoaderTest {
  @Test
  void createsDefaultsThenReadsAdministratorFile() throws Exception {
    Path directory = Files.createTempDirectory("commission-catalog");
    try {
      Path path = directory.resolve("config/economysystem/commissions/catalog.json");
      CommissionCatalog created = CommissionCatalogConfigLoader.loadOrCreate(path);
      assertEquals(CommissionCatalogDefaults.create().templates(), created.templates());
      assertEquals(created.templates(), CommissionCatalogConfigLoader.loadOrCreate(path).templates());
      String custom = CommissionCatalogJsonCodec.encode(created);
      Files.writeString(path, custom);
      assertEquals(created.targetPools().keySet(), CommissionCatalogConfigLoader.loadOrCreate(path).targetPools().keySet());
    } finally {
      deleteTree(directory);
    }
  }

  @Test
  void malformedExistingFileFailsFast() throws Exception {
    Path directory = Files.createTempDirectory("commission-catalog-invalid");
    try {
      Path path = directory.resolve("catalog.json");
      Files.writeString(path, "{\"schema\":99}");
      IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
          () -> CommissionCatalogConfigLoader.loadOrCreate(path));
      assertEquals(true, failure.getMessage().contains("invalid commission catalog"));
    } finally {
      deleteTree(directory);
    }
  }

  private static void deleteTree(Path root) throws Exception {
    try (var paths = Files.walk(root)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
        try { Files.deleteIfExists(path); } catch (Exception failure) { throw new RuntimeException(failure); }
      });
    }
  }
}
