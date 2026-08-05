package com.mo.economy_system.common.check;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientFileCheckResultJsonCodec {
  private static final Set<String> ROOT_FIELDS =
      Set.of("schemaVersion", "status", "checkType", "files", "skipped", "errorCode");
  private static final Set<String> FILE_FIELDS = Set.of("fileName", "size", "sha256");
  private static final Set<String> SKIPPED_FIELDS = Set.of("fileName", "reason");

  private ClientFileCheckResultJsonCodec() {}

  public static String encode(ClientFileCheckResult result) {
    JsonObject root = new JsonObject();
    root.addProperty("schemaVersion", result.schemaVersion());
    root.addProperty("status", result.status().name());
    root.addProperty("checkType", result.checkType().id());
    JsonArray files = new JsonArray();
    for (ClientFileCheckEntry entry : result.files()) {
      JsonObject value = new JsonObject();
      value.addProperty("fileName", entry.fileName());
      value.addProperty("size", entry.size());
      value.addProperty("sha256", entry.sha256());
      files.add(value);
    }
    root.add("files", files);
    JsonArray skipped = new JsonArray();
    for (ClientFileCheckSkippedEntry entry : result.skipped()) {
      JsonObject value = new JsonObject();
      value.addProperty("fileName", entry.fileName());
      value.addProperty("reason", entry.reason());
      skipped.add(value);
    }
    root.add("skipped", skipped);
    if (result.errorCode() == null) root.add("errorCode", JsonNull.INSTANCE);
    else root.addProperty("errorCode", result.errorCode());
    String encoded = root.toString();
    if (encoded.length() > EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH)
      throw new IllegalArgumentException("encoded result exceeds wire limit");
    return encoded;
  }

  public static ClientFileCheckResult decode(String encoded) {
    if (encoded == null || encoded.length() > EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH)
      throw new IllegalArgumentException("result JSON length");
    assertNoDuplicateKeys(encoded);
    JsonElement parsed = JsonParser.parseString(encoded);
    if (!parsed.isJsonObject()) throw new IllegalArgumentException("result root");
    JsonObject root = parsed.getAsJsonObject();
    exactFields(root, ROOT_FIELDS);
    if (!root.get("schemaVersion").isJsonPrimitive()
        || !root.getAsJsonPrimitive("schemaVersion").isNumber()
        || !root.get("schemaVersion").toString().equals("1"))
      throw new IllegalArgumentException("schema version");
    int schema = 1;
    ClientFileCheckStatus status = ClientFileCheckStatus.valueOf(strictString(root, "status"));
    ClientFileCheckType type = ClientFileCheckType.fromId(strictString(root, "checkType"));
    JsonArray encodedFiles = root.getAsJsonArray("files");
    JsonArray encodedSkipped = root.getAsJsonArray("skipped");
    if (encodedFiles.size() > EconomyNetworkLimits.MAX_CHECK_FILES
        || encodedSkipped.size() > EconomyNetworkLimits.MAX_CHECK_SKIPPED_FILES)
      throw new IllegalArgumentException("result arrays");
    List<ClientFileCheckEntry> files = new ArrayList<>();
    for (JsonElement value : encodedFiles) {
      if (!value.isJsonObject()) throw new IllegalArgumentException("file entry");
      JsonObject object = value.getAsJsonObject();
      exactFields(object, FILE_FIELDS);
      JsonElement sizeValue = object.get("size");
      if (!sizeValue.isJsonPrimitive()
          || !sizeValue.getAsJsonPrimitive().isNumber()
          || !sizeValue.toString().matches("0|[1-9][0-9]*"))
        throw new IllegalArgumentException("file size");
      files.add(
          new ClientFileCheckEntry(
              strictString(object, "fileName"),
              sizeValue.getAsLong(),
              strictString(object, "sha256")));
    }
    List<ClientFileCheckSkippedEntry> skipped = new ArrayList<>();
    for (JsonElement value : encodedSkipped) {
      if (!value.isJsonObject()) throw new IllegalArgumentException("skipped entry");
      JsonObject object = value.getAsJsonObject();
      exactFields(object, SKIPPED_FIELDS);
      skipped.add(
          new ClientFileCheckSkippedEntry(
              strictString(object, "fileName"), strictString(object, "reason")));
    }
    JsonElement error = root.get("errorCode");
    String errorCode = error == null || error.isJsonNull() ? null : strictString(root, "errorCode");
    return new ClientFileCheckResult(schema, status, type, files, skipped, errorCode);
  }

  private static void exactFields(JsonObject object, Set<String> expected) {
    if (!object.keySet().equals(expected))
      throw new IllegalArgumentException("unknown or missing JSON field");
  }

  private static String strictString(JsonObject object, String field) {
    JsonElement value = object.get(field);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
      throw new IllegalArgumentException(field + " must be a string");
    return value.getAsString();
  }

  private static void assertNoDuplicateKeys(String encoded) {
    try (JsonReader reader = new JsonReader(new StringReader(encoded))) {
      reader.setLenient(false);
      readUnique(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT)
        throw new IllegalArgumentException("trailing JSON");
    } catch (Exception failure) {
      if (failure instanceof IllegalArgumentException invalid) throw invalid;
      throw new IllegalArgumentException("invalid JSON", failure);
    }
  }

  private static void readUnique(JsonReader reader) throws Exception {
    switch (reader.peek()) {
      case BEGIN_OBJECT -> {
        reader.beginObject();
        Set<String> names = new HashSet<>();
        while (reader.hasNext()) {
          String name = reader.nextName();
          if (!names.add(name)) throw new IllegalArgumentException("duplicate JSON key " + name);
          readUnique(reader);
        }
        reader.endObject();
      }
      case BEGIN_ARRAY -> {
        reader.beginArray();
        while (reader.hasNext()) readUnique(reader);
        reader.endArray();
      }
      case STRING -> reader.nextString();
      case NUMBER -> reader.nextString();
      case BOOLEAN -> reader.nextBoolean();
      case NULL -> reader.nextNull();
      default -> throw new IllegalArgumentException("invalid JSON token");
    }
  }
}
