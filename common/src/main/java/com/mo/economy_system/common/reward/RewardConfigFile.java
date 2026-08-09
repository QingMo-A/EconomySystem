package com.mo.economy_system.common.reward;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Backward-compatible JSON persistence for {@code economy_rewards.json}. */
public final class RewardConfigFile {
  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public record LoadResult(
      boolean usable, List<RewardEntry> entries, List<String> issues, Throwable error) {
    public LoadResult {
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
      issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
      if (usable && error != null && entries.isEmpty()) {
        throw new IllegalArgumentException("usable error result requires fallback entries");
      }
    }
  }

  private final Path path;

  public RewardConfigFile(Path path) {
    this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
  }

  public Path path() {
    return path;
  }

  public LoadResult loadOrCreate(List<RewardEntry> defaults) {
    Objects.requireNonNull(defaults, "defaults");
    if (!Files.exists(path)) {
      try {
        save(defaults);
        return new LoadResult(true, defaults, List.of(), null);
      } catch (IOException error) {
        return new LoadResult(
            true,
            defaults,
            List.of("could not create the default reward config"),
            error);
      }
    }
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return decode(reader);
    } catch (IOException | RuntimeException error) {
      return new LoadResult(false, List.of(), List.of("could not read reward config"), error);
    }
  }

  public void save(List<RewardEntry> entries) throws IOException {
    Objects.requireNonNull(entries, "entries");
    Path parent = Objects.requireNonNull(path.getParent(), "config parent");
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
    try {
      try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
        GSON.toJson(encode(entries), writer);
      }
      try {
        Files.move(
            temporary,
            path,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException error) {
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  static LoadResult decode(Reader reader) {
    JsonElement root = JsonParser.parseReader(reader);
    if (root == null || !root.isJsonArray()) {
      return new LoadResult(false, List.of(), List.of("root must be a JSON array"), null);
    }

    JsonArray array = root.getAsJsonArray();
    List<RewardEntry> entries = new ArrayList<>();
    List<String> issues = new ArrayList<>();
    Set<String> seenTypes = new HashSet<>();
    for (int index = 0; index < array.size(); index++) {
      try {
        JsonElement element = array.get(index);
        if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
        JsonObject object = element.getAsJsonObject();
        String type = stringValue(object, "type");
        double rawChance = doubleValue(object, "dropChance");
        if (!Double.isFinite(rawChance)) {
          throw new IllegalArgumentException("dropChance must be finite");
        }
        double chance = Math.max(0.0D, Math.min(1.0D, rawChance));
        if (chance != rawChance) {
          issues.add("entry " + index + " dropChance was clamped to " + chance);
        }
        RewardEntry entry =
            new RewardEntry(type, chance, intValue(object, "dropMin"), intValue(object, "dropMax"));
        if (!seenTypes.add(entry.type())) {
          issues.add("entry " + index + " duplicates " + entry.type() + " and was ignored");
          continue;
        }
        entries.add(entry);
      } catch (RuntimeException error) {
        issues.add("entry " + index + " was ignored: " + safeMessage(error));
      }
    }
    boolean usable = array.isEmpty() || !entries.isEmpty();
    if (!usable) issues.add("no valid reward entries were found; keeping the previous table");
    return new LoadResult(usable, entries, issues, null);
  }

  private static JsonArray encode(List<RewardEntry> entries) {
    JsonArray array = new JsonArray();
    Set<String> seenTypes = new HashSet<>();
    for (RewardEntry entry : entries) {
      Objects.requireNonNull(entry, "entry");
      if (!seenTypes.add(entry.type())) {
        throw new IllegalArgumentException("duplicate reward entity type: " + entry.type());
      }
      JsonObject object = new JsonObject();
      object.addProperty("type", entry.type());
      object.addProperty("dropChance", entry.dropChance());
      object.addProperty("dropMin", entry.dropMin());
      object.addProperty("dropMax", entry.dropMax());
      array.add(object);
    }
    return array;
  }

  private static JsonPrimitive primitive(JsonObject object, String name) {
    JsonElement value = object.get(name);
    if (value == null || !value.isJsonPrimitive()) {
      throw new IllegalArgumentException("missing or invalid " + name);
    }
    return value.getAsJsonPrimitive();
  }

  private static String stringValue(JsonObject object, String name) {
    JsonPrimitive value = primitive(object, name);
    if (!value.isString()) throw new IllegalArgumentException(name + " must be a string");
    return value.getAsString();
  }

  private static double doubleValue(JsonObject object, String name) {
    JsonPrimitive value = primitive(object, name);
    if (!value.isNumber()) throw new IllegalArgumentException(name + " must be a number");
    return value.getAsDouble();
  }

  private static int intValue(JsonObject object, String name) {
    JsonPrimitive value = primitive(object, name);
    if (!value.isNumber()) throw new IllegalArgumentException(name + " must be an integer");
    try {
      return value.getAsBigDecimal().intValueExact();
    } catch (ArithmeticException error) {
      throw new IllegalArgumentException(name + " must be a 32-bit integer", error);
    }
  }

  private static String safeMessage(RuntimeException error) {
    return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
  }
}
