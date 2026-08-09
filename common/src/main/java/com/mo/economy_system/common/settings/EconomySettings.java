package com.mo.economy_system.common.settings;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.platform.EconomyServices;
import java.util.Map;

/** Process-local facade for the one common settings store used by both target builds. */
public final class EconomySettings {
  private static CommonSettingsStore store;

  private EconomySettings() {}

  public static synchronized void initialize() {
    if (store != null) return;
    store = new CommonSettingsStore(
        EconomyServices.platform()
            .configDirectory()
            .resolve(EconomyConstants.MOD_ID)
            .resolve("game_settings.json"));
    store.load();
  }

  public static synchronized Map<String, String> all() {
    return store().snapshot();
  }

  public static synchronized String get(String key) {
    return store().get(key);
  }

  public static synchronized String description(String key) {
    return store().description(key);
  }

  public static synchronized boolean set(String key, String value) {
    try {
      return store().set(key, value);
    } catch (java.io.IOException error) {
      throw new IllegalStateException("could not persist setting " + key, error);
    }
  }

  public static synchronized void reload() {
    store().load();
  }

  private static CommonSettingsStore store() {
    initialize();
    return store;
  }
}
