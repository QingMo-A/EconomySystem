package com.mo.economy_system.common.recycle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Strict, loader-neutral JSON codec for administrator-maintained recycling offers. */
public final class RecycleConfigJsonCodec {
  public static final int SCHEMA_VERSION = 1;
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private RecycleConfigJsonCodec() {}

  public static RecycleConfig decode(String json) {
    if (json == null || json.isBlank()) throw new IllegalArgumentException("recycle config is empty");
    final JsonObject root;
    try {
      JsonElement parsed = JsonParser.parseString(json);
      if (!parsed.isJsonObject()) throw new IllegalArgumentException("recycle config root must be an object");
      root = parsed.getAsJsonObject();
    } catch (RuntimeException failure) {
      if (failure instanceof IllegalArgumentException) throw (IllegalArgumentException) failure;
      throw new IllegalArgumentException("invalid recycle config JSON", failure);
    }
    int schema = integer(root, "schema", SCHEMA_VERSION);
    if (schema != SCHEMA_VERSION) throw new IllegalArgumentException("unsupported recycle config schema: " + schema);
    long cycleMillis = longValue(root, "cycleMillis", -1L);
    if (cycleMillis <= 0) throw new IllegalArgumentException("cycleMillis must be positive");
    JsonArray values = array(root, "offers");
    List<RecycleOffer> offers = new ArrayList<>();
    for (JsonElement value : values) {
      if (!value.isJsonObject()) throw new IllegalArgumentException("recycle offer must be an object");
      JsonObject item = value.getAsJsonObject();
      offers.add(new RecycleOffer(
          requiredString(item, "itemId"),
          integer(item, "baseUnitPrice", -1),
          integer(item, "highUnitPrice", 0),
          integer(item, "highQuota", 0),
          booleanValue(item, "fallbackToBaseWhenHighQuotaExhausted", true)));
    }
    return new RecycleConfig(Duration.ofMillis(cycleMillis), offers);
  }

  public static String encode(RecycleConfig config) {
    if (config == null) throw new NullPointerException("config");
    JsonObject root = new JsonObject();
    root.addProperty("schema", SCHEMA_VERSION);
    root.addProperty("cycleMillis", config.cycle().toMillis());
    JsonArray offers = new JsonArray();
    for (RecycleOffer offer : config.offers()) {
      JsonObject item = new JsonObject();
      item.addProperty("itemId", offer.itemId());
      item.addProperty("baseUnitPrice", offer.baseUnitPrice());
      item.addProperty("highUnitPrice", offer.highUnitPrice());
      item.addProperty("highQuota", offer.highQuota());
      item.addProperty("fallbackToBaseWhenHighQuotaExhausted", offer.fallbackToBaseWhenHighQuotaExhausted());
      offers.add(item);
    }
    root.add("offers", offers);
    return GSON.toJson(root);
  }

  private static JsonArray array(JsonObject root, String key) {
    if (!root.has(key) || !root.get(key).isJsonArray()) throw new IllegalArgumentException("missing array: " + key);
    return root.getAsJsonArray(key);
  }

  private static String requiredString(JsonObject root, String key) {
    if (!root.has(key) || root.get(key).isJsonNull() || root.get(key).getAsString().isBlank()) {
      throw new IllegalArgumentException("missing field: " + key);
    }
    return root.get(key).getAsString();
  }

  private static int integer(JsonObject root, String key, int fallback) {
    return root.has(key) ? root.get(key).getAsInt() : fallback;
  }

  private static long longValue(JsonObject root, String key, long fallback) {
    return root.has(key) ? root.get(key).getAsLong() : fallback;
  }

  private static boolean booleanValue(JsonObject root, String key, boolean fallback) {
    return root.has(key) ? root.get(key).getAsBoolean() : fallback;
  }
}
