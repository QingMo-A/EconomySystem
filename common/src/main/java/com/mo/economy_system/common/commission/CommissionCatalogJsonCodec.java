package com.mo.economy_system.common.commission;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict, loader-neutral JSON codec for the administrator-maintained personal commission bank. */
public final class CommissionCatalogJsonCodec {
  public static final int SCHEMA_VERSION = 1;
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private CommissionCatalogJsonCodec() {}

  public static CommissionCatalog decode(String json) {
    if (json == null || json.isBlank()) throw new IllegalArgumentException("commission catalog is empty");
    final JsonObject root;
    try {
      JsonElement parsed = JsonParser.parseString(json);
      if (!parsed.isJsonObject()) throw new IllegalArgumentException("catalog root must be an object");
      root = parsed.getAsJsonObject();
    } catch (RuntimeException failure) {
      if (failure instanceof IllegalArgumentException) throw (IllegalArgumentException) failure;
      throw new IllegalArgumentException("invalid commission catalog JSON", failure);
    }
    int schema = integer(root, "schema", SCHEMA_VERSION);
    if (schema != SCHEMA_VERSION) throw new IllegalArgumentException("unsupported commission catalog schema: " + schema);

    Map<String, List<CommissionRequester>> requesterPools = new LinkedHashMap<>();
    JsonObject requesterObject = object(root, "requesterPools");
    for (Map.Entry<String, JsonElement> entry : requesterObject.entrySet()) {
      JsonArray values = array(entry.getValue(), "requester pool " + entry.getKey());
      List<CommissionRequester> requesters = new ArrayList<>();
      for (JsonElement value : values) {
        JsonObject item = value.getAsJsonObject();
        requesters.add(new CommissionRequester(
            requiredString(item, "id"), requiredString(item, "displayName"),
            decimal(item, "quantityMultiplier", 1.0D), decimal(item, "rewardMultiplier", 1.0D),
            integer(item, "weight", 1), string(item, "rarity", "common"),
            string(item, "mailSignature", "")));
      }
      requesterPools.put(entry.getKey(), requesters);
    }

    Map<String, CommissionTargetPool> targetPools = new LinkedHashMap<>();
    JsonObject targetObject = object(root, "targetPools");
    for (Map.Entry<String, JsonElement> entry : targetObject.entrySet()) {
      JsonArray values = array(entry.getValue(), "target pool " + entry.getKey());
      List<CommissionTargetPool.Target> targets = new ArrayList<>();
      for (JsonElement value : values) {
        if (value.isJsonPrimitive()) {
          targets.add(new CommissionTargetPool.Target(value.getAsString(), 1));
        } else {
          JsonObject item = value.getAsJsonObject();
          targets.add(new CommissionTargetPool.Target(requiredString(item, "id"), integer(item, "weight", 1)));
        }
      }
      targetPools.put(entry.getKey(), new CommissionTargetPool(entry.getKey(), targets));
    }

    List<CommissionTemplate> templates = new ArrayList<>();
    for (JsonElement value : array(root, "templates")) {
      JsonObject item = value.getAsJsonObject();
      templates.add(new CommissionTemplate(
          requiredString(item, "id"), CommissionType.fromId(requiredString(item, "type")),
          requiredString(item, "requesterPool"), requiredString(item, "targetPool"),
          integer(item, "quantityMin", 1), integer(item, "quantityMax", 1), integer(item, "quantityStep", 1),
          CommissionRewardMode.fromId(string(item, "rewardMode", "per_unit")), integer(item, "rewardPerUnit", 0),
          decimal(item, "rewardMultiplierMin", 1.0D), decimal(item, "rewardMultiplierMax", 1.0D),
          integer(item, "weight", 1), requiredString(item, "category"), string(item, "rarity", "common"),
          longValue(item, "expirationMinMillis", 1), longValue(item, "expirationMaxMillis", 1),
          integer(item, "playerLimit", -1), string(item, "textTemplate", ""),
          string(item, "requiredProfession", ""), integer(item, "requiredProfessionLevel", 0),
          integer(item, "professionExperienceReward", 0)));
    }
    return new CommissionCatalog(templates, requesterPools, targetPools, decodeSettings(root));
  }

  public static String encode(CommissionCatalog catalog) {
    if (catalog == null) throw new NullPointerException("catalog");
    JsonObject root = new JsonObject();
    root.addProperty("schema", SCHEMA_VERSION);
    JsonObject settings = new JsonObject();
    PersonalCommissionSettings s = catalog.settings();
    settings.addProperty("refreshBaseIntervalMillis", s.refreshBaseIntervalMillis());
    settings.addProperty("refreshJitterMillis", s.refreshJitterMillis());
    settings.addProperty("minCommissionsPerRefresh", s.minCommissionsPerRefresh());
    settings.addProperty("maxCommissionsPerRefresh", s.maxCommissionsPerRefresh());
    settings.addProperty("maxActivePersonalCommissions", s.maxActivePersonalCommissions());
    settings.addProperty("defaultExpirationMinMillis", s.defaultExpirationMinMillis());
    settings.addProperty("defaultExpirationMaxMillis", s.defaultExpirationMaxMillis());
    settings.addProperty("rewardMultiplierMin", s.rewardMultiplierMin());
    settings.addProperty("rewardMultiplierMax", s.rewardMultiplierMax());
    JsonObject categoryWeights = new JsonObject();
    s.categoryWeights().forEach(categoryWeights::addProperty);
    settings.add("categoryWeights", categoryWeights);
    root.add("settings", settings);

    JsonObject requesterPools = new JsonObject();
    catalog.requesterPools().forEach((pool, values) -> {
      JsonArray array = new JsonArray();
      values.forEach(value -> {
        JsonObject item = new JsonObject();
        item.addProperty("id", value.id()); item.addProperty("displayName", value.displayName());
        item.addProperty("quantityMultiplier", value.quantityMultiplier()); item.addProperty("rewardMultiplier", value.rewardMultiplier());
        item.addProperty("weight", value.weight()); item.addProperty("rarity", value.rarity()); item.addProperty("mailSignature", value.mailSignature());
        array.add(item);
      });
      requesterPools.add(pool, array);
    });
    root.add("requesterPools", requesterPools);

    JsonObject targetPools = new JsonObject();
    catalog.targetPools().forEach((pool, value) -> {
      JsonArray array = new JsonArray();
      value.targets().forEach(target -> { JsonObject item = new JsonObject(); item.addProperty("id", target.id()); item.addProperty("weight", target.weight()); array.add(item); });
      targetPools.add(pool, array);
    });
    root.add("targetPools", targetPools);

    JsonArray templates = new JsonArray();
    catalog.templates().forEach(value -> {
      JsonObject item = new JsonObject();
      item.addProperty("id", value.id()); item.addProperty("type", value.type().id());
      item.addProperty("requesterPool", value.requesterPool()); item.addProperty("targetPool", value.targetPool());
      item.addProperty("quantityMin", value.quantityMin()); item.addProperty("quantityMax", value.quantityMax()); item.addProperty("quantityStep", value.quantityStep());
      item.addProperty("rewardMode", value.rewardMode().id()); item.addProperty("rewardPerUnit", value.rewardPerUnit());
      item.addProperty("rewardMultiplierMin", value.rewardMultiplierMin()); item.addProperty("rewardMultiplierMax", value.rewardMultiplierMax());
      item.addProperty("weight", value.weight()); item.addProperty("category", value.category()); item.addProperty("rarity", value.rarity());
      item.addProperty("expirationMinMillis", value.expirationMinMillis()); item.addProperty("expirationMaxMillis", value.expirationMaxMillis());
      item.addProperty("playerLimit", value.playerLimit()); item.addProperty("textTemplate", value.textTemplate());
      item.addProperty("requiredProfession", value.requiredProfession()); item.addProperty("requiredProfessionLevel", value.requiredProfessionLevel()); item.addProperty("professionExperienceReward", value.professionExperienceReward());
      templates.add(item);
    });
    root.add("templates", templates);
    return GSON.toJson(root);
  }

  private static PersonalCommissionSettings decodeSettings(JsonObject root) {
    JsonObject s = root.has("settings") ? object(root, "settings") : new JsonObject();
    PersonalCommissionSettings d = PersonalCommissionSettings.defaults();
    Map<String, Integer> categories = new LinkedHashMap<>();
    if (s.has("categoryWeights")) {
      for (Map.Entry<String, JsonElement> entry : object(s, "categoryWeights").entrySet()) categories.put(entry.getKey(), entry.getValue().getAsInt());
    } else categories.putAll(d.categoryWeights());
    return new PersonalCommissionSettings(
        longValue(s, "refreshBaseIntervalMillis", d.refreshBaseIntervalMillis()), longValue(s, "refreshJitterMillis", d.refreshJitterMillis()),
        integer(s, "minCommissionsPerRefresh", d.minCommissionsPerRefresh()), integer(s, "maxCommissionsPerRefresh", d.maxCommissionsPerRefresh()), integer(s, "maxActivePersonalCommissions", d.maxActivePersonalCommissions()),
        longValue(s, "defaultExpirationMinMillis", d.defaultExpirationMinMillis()), longValue(s, "defaultExpirationMaxMillis", d.defaultExpirationMaxMillis()), decimal(s, "rewardMultiplierMin", d.rewardMultiplierMin()), decimal(s, "rewardMultiplierMax", d.rewardMultiplierMax()), categories);
  }

  private static JsonObject object(JsonObject root, String key) { if (!root.has(key) || !root.get(key).isJsonObject()) throw new IllegalArgumentException("missing object: " + key); return root.getAsJsonObject(key); }
  private static JsonArray array(JsonObject root, String key) { if (!root.has(key) || !root.get(key).isJsonArray()) throw new IllegalArgumentException("missing array: " + key); return root.getAsJsonArray(key); }
  private static JsonArray array(JsonElement value, String key) { if (!value.isJsonArray()) throw new IllegalArgumentException(key + " must be an array"); return value.getAsJsonArray(); }
  private static String requiredString(JsonObject root, String key) { return string(root, key, null) == null ? missing(key) : string(root, key, null); }
  private static String missing(String key) { throw new IllegalArgumentException("missing field: " + key); }
  private static String string(JsonObject root, String key, String fallback) { return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback; }
  private static int integer(JsonObject root, String key, int fallback) { return root.has(key) ? root.get(key).getAsInt() : fallback; }
  private static long longValue(JsonObject root, String key, long fallback) { return root.has(key) ? root.get(key).getAsLong() : fallback; }
  private static double decimal(JsonObject root, String key, double fallback) { return root.has(key) ? root.get(key).getAsDouble() : fallback; }
}
