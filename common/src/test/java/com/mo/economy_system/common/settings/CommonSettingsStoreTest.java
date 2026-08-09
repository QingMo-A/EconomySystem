package com.mo.economy_system.common.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommonSettingsStoreTest {
  @TempDir Path directory;

  @Test
  void defaultsAndStrictValuesAreShared() {
    CommonSettingsStore store = new CommonSettingsStore(directory.resolve("game_settings.json"));
    assertEquals("demand", store.get(CommonSettingsStore.SHOP_PRICING_MODE));
    assertThrows(IllegalArgumentException.class,
        () -> store.set(CommonSettingsStore.SHOP_PRICING_MODE, "unknown"));
  }

  @Test
  void failedSaveRestoresPreviousInMemoryValue() throws Exception {
    Path path = directory.resolve("game_settings.json");
    CommonSettingsStore store = new CommonSettingsStore(path);
    store.load();
    assertEquals("demand", store.get(CommonSettingsStore.SHOP_PRICING_MODE));
    Files.delete(path);
    Files.createDirectory(path);
    assertThrows(Exception.class,
        () -> store.set(CommonSettingsStore.SHOP_PRICING_MODE, "stock"));
    assertEquals("demand", store.get(CommonSettingsStore.SHOP_PRICING_MODE));
  }

  @Test
  void malformedFileKeepsLastKnownGoodValue() throws Exception {
    Path path = directory.resolve("game_settings.json");
    CommonSettingsStore store = new CommonSettingsStore(path);
    store.load();
    store.set(CommonSettingsStore.SHOP_PRICING_MODE, "stock");
    Files.writeString(path, "not json", StandardCharsets.UTF_8);
    store.load();
    assertEquals("stock", store.get(CommonSettingsStore.SHOP_PRICING_MODE));
  }
}
