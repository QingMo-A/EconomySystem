package com.mo.economy_system.common.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RewardConfigFileTest {
  @TempDir Path directory;

  @Test
  void missingConfigCreatesThe1211DefaultShape() throws Exception {
    RewardConfigFile file = new RewardConfigFile(directory.resolve("economy_rewards.json"));

    RewardConfigFile.LoadResult result = file.loadOrCreate(RewardDefaults.entries());

    assertTrue(result.usable());
    assertEquals(27, result.entries().size());
    String written = Files.readString(file.path(), StandardCharsets.UTF_8);
    assertTrue(written.contains("\"dropChance\""));
    assertTrue(written.contains("\"minecraft:ender_dragon\""));
  }

  @Test
  void invalidRowsAreSkippedAndLegacyProbabilityIsSafelyClamped() {
    String json = """
        [
          {"type":"minecraft:zombie","dropChance":1.5,"dropMin":1,"dropMax":5},
          {"type":"minecraft:skeleton","dropChance":0.5,"dropMin":8,"dropMax":2},
          {"type":"minecraft:zombie","dropChance":0.1,"dropMin":1,"dropMax":1}
        ]
        """;

    RewardConfigFile.LoadResult result = RewardConfigFile.decode(new StringReader(json));

    assertTrue(result.usable());
    assertEquals(List.of(new RewardEntry("minecraft:zombie", 1.0D, 1, 5)), result.entries());
    assertEquals(3, result.issues().size());
  }

  @Test
  void entirelyInvalidNonEmptyFileIsNotInstalled() {
    RewardConfigFile.LoadResult result =
        RewardConfigFile.decode(
            new StringReader("[{\"type\":\"bad id\",\"dropChance\":1,\"dropMin\":1,\"dropMax\":2}]"));

    assertFalse(result.usable());
    assertTrue(result.entries().isEmpty());
  }

  @Test
  void malformedReloadKeepsLastKnownGoodCatalog() throws Exception {
    Path path = directory.resolve("economy_rewards.json");
    Files.writeString(
        path,
        "[{\"type\":\"minecraft:zombie\",\"dropChance\":1,\"dropMin\":2,\"dropMax\":2}]",
        StandardCharsets.UTF_8);
    RewardConfiguration configuration = new RewardConfiguration(path);
    configuration.start();
    Files.writeString(path, "not json", StandardCharsets.UTF_8);

    configuration.reload();

    assertEquals(2, configuration.catalog().find("minecraft:zombie").orElseThrow().dropMin());
    configuration.close();
  }
}
