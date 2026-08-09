package com.mo.economy_system.common.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

/** Strictly extracts the two fields needed from a GitHub latest-release response. */
public final class UpdateReleaseJsonCodec {
  private static final int MAX_PAYLOAD_CHARS = 256_000;

  private UpdateReleaseJsonCodec() {}

  public static UpdateRelease decode(String payload) {
    if (payload == null || payload.length() > MAX_PAYLOAD_CHARS) {
      throw new IllegalArgumentException("release payload length");
    }
    assertNoDuplicateKeys(payload);
    JsonElement parsed;
    try {
      parsed = JsonParser.parseString(payload);
    } catch (RuntimeException error) {
      throw new IllegalArgumentException("release payload JSON", error);
    }
    if (parsed == null || !parsed.isJsonObject()) throw new IllegalArgumentException("release root");
    JsonObject object = parsed.getAsJsonObject();
    return new UpdateRelease(strictString(object, "tag_name"), strictString(object, "html_url"));
  }

  private static String strictString(JsonObject object, String name) {
    JsonElement value = object.get(name);
    if (value == null || !value.isJsonPrimitive()) throw new IllegalArgumentException(name);
    JsonPrimitive primitive = value.getAsJsonPrimitive();
    if (!primitive.isString()) throw new IllegalArgumentException(name);
    String result = primitive.getAsString().trim();
    if (result.isEmpty()) throw new IllegalArgumentException(name);
    return result;
  }

  private static void assertNoDuplicateKeys(String payload) {
    try (JsonReader reader = new JsonReader(new StringReader(payload))) {
      skipValue(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("trailing JSON");
    } catch (java.io.IOException | RuntimeException error) {
      if (error instanceof IllegalArgumentException illegal) throw illegal;
      throw new IllegalArgumentException("release payload JSON", error);
    }
  }

  private static void skipValue(JsonReader reader) throws java.io.IOException {
    switch (reader.peek()) {
      case BEGIN_OBJECT -> {
        reader.beginObject();
        Set<String> names = new HashSet<>();
        while (reader.hasNext()) {
          String name = reader.nextName();
          if (!names.add(name)) throw new IllegalArgumentException("duplicate field: " + name);
          skipValue(reader);
        }
        reader.endObject();
      }
      case BEGIN_ARRAY -> {
        reader.beginArray();
        while (reader.hasNext()) skipValue(reader);
        reader.endArray();
      }
      default -> reader.skipValue();
    }
  }
}
