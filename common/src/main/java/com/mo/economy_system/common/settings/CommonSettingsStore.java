package com.mo.economy_system.common.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Atomic, validated settings state shared by all target adapters. */
public final class CommonSettingsStore {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  public static final String SHOP_PRICING_MODE = "shop.pricing.mode";
  public static final String TERRITORY_PRICE_PER_CELL = "territory.price_per_cell";

  private final Path path;
  private final Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
  private final Map<String, String> values = new LinkedHashMap<>();

  public CommonSettingsStore(Path path) {
    this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    register(new SettingDefinition(
        SHOP_PRICING_MODE,
        "shop pricing mode: demand or stock",
        "demand",
        java.util.Set.of("demand", "stock")));
    register(new SettingDefinition(
        TERRITORY_PRICE_PER_CELL,
        "territory claim price per X/Z cell (non-negative integer)",
        "20",
        java.util.Set.of()));
  }

  public synchronized void register(SettingDefinition definition) {
    Objects.requireNonNull(definition, "definition");
    definitions.put(definition.key(), definition);
    values.putIfAbsent(definition.key(), definition.defaultValue());
  }

  public synchronized Map<String, String> snapshot() {
    return Map.copyOf(new LinkedHashMap<>(values));
  }

  public synchronized String get(String key) {
    SettingDefinition definition = definitions.get(key);
    return definition == null ? null : values.getOrDefault(key, definition.defaultValue());
  }

  public synchronized String description(String key) {
    SettingDefinition definition = definitions.get(key);
    return definition == null ? "" : definition.description();
  }

  public synchronized boolean set(String key, String rawValue) throws IOException {
    SettingDefinition definition = definitions.get(key);
    if (definition == null) return false;
    String parsed = definition.parse(rawValue);
    String previous = values.put(key, parsed);
    try {
      saveLocked();
      return true;
    } catch (IOException | RuntimeException error) {
      if (previous == null) values.remove(key);
      else values.put(key, previous);
      throw error;
    }
  }

  public synchronized void load() {
    Map<String, String> previous = new LinkedHashMap<>(values);
    ensureDefaultsLocked();
    if (!Files.isRegularFile(path)) {
      try {
        saveLocked();
      } catch (IOException ignored) {
        values.clear();
        values.putAll(previous);
      }
      return;
    }
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) throw new IllegalArgumentException("settings root must be object");
      JsonObject object = root.getAsJsonObject();
      for (Map.Entry<String, SettingDefinition> entry : definitions.entrySet()) {
        JsonElement raw = object.get(entry.getKey());
        if (raw == null || !raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) continue;
        try {
          values.put(entry.getKey(), entry.getValue().parse(raw.getAsString()));
        } catch (RuntimeException ignored) {
          // Keep the last/default value for one invalid setting.
        }
      }
      saveLocked();
    } catch (Exception error) {
      values.clear();
      values.putAll(previous);
    }
  }

  private void ensureDefaultsLocked() {
    for (SettingDefinition definition : definitions.values()) {
      values.putIfAbsent(definition.key(), definition.defaultValue());
    }
  }

  private void saveLocked() throws IOException {
    Path parent = Objects.requireNonNull(path.getParent(), "settings parent");
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
    try {
      JsonObject object = new JsonObject();
      for (Map.Entry<String, String> entry : values.entrySet()) object.addProperty(entry.getKey(), entry.getValue());
      try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        GSON.toJson(object, writer);
      }
      try {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException error) {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
