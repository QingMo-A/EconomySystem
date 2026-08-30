package com.mo.economy_system.common.settings;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.platform.EconomyServices;
import java.util.Map;

/** Process-local facade for the one common settings store used by both target builds. */
public final class EconomySettings {
  /** Price charged for each claimed X/Z cell, shared by all loader targets. */
  public static final String TERRITORY_PRICE_PER_CELL = "territory.price_per_cell";

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

  /** Updates the territory cell price with the same bounds used by the administrator command. */
  public static synchronized boolean setTerritoryPricePerCell(long value) {
    if (value < 0 || value > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("territory price per cell must be between 0 and "
          + Integer.MAX_VALUE);
    }
    return set(TERRITORY_PRICE_PER_CELL, Long.toString(value));
  }

  public static synchronized void reload() {
    store().load();
  }

  private static CommonSettingsStore store() {
    initialize();
    return store;
  }
}
