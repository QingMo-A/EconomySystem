package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TerritoryRemovalLanguageResourcesTest {
  private static final Set<String> KEYS =
      Set.of(
          "button.territory.delete",
          "button.territory.delete_short",
          "button.territory.delete_confirm",
          "button.territory.delete_cancel",
          "screen.territory_remove.title",
          "screen.territory_remove.warning",
          "screen.territory_remove.irreversible",
          "screen.territory_remove.no_refund",
          "message.territory.remove.success",
          "message.territory.remove.not_found",
          "message.territory.remove.no_permission",
          "message.territory.remove.rate_limited",
          "message.territory.remove.persist_failed",
          "message.territory.remove.state_unknown",
          "message.territory.remove.resize_cancelled",
          "message.claim.resize.persist_failed",
          "message.claim.resize.state_unknown",
          "message.claim.resize.refund_failed",
          "message.claim.resize.overlap",
          "message.claim.resize.unchanged");

  @Test
  void removalAndResizeResourcesAreStrictAndEquivalent() throws Exception {
    Path root = findRoot();
    Map<String, JsonObject> files = new LinkedHashMap<>();
    for (String path :
        List.of(
            "src/generated/resources/assets/economy_system/lang/en_us.json",
            "src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json")) {
      String content = Files.readString(root.resolve(path));
      assertNoDuplicateKeys(path, content);
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      assertTrue(json.keySet().containsAll(KEYS), path);
      for (String key : KEYS)
        assertFalse(json.get(key).getAsString().contains("§"), path + " " + key);
      files.put(path, json);
    }
    JsonObject generatedEnglish = files.values().iterator().next();
    for (String key : KEYS) {
      int expected = placeholders(generatedEnglish.get(key).getAsString());
      for (var entry : files.entrySet())
        assertEquals(
            expected, placeholders(entry.getValue().get(key).getAsString()), entry.getKey());
    }
    assertEquals(
        1, placeholders(generatedEnglish.get("message.territory.remove.success").getAsString()));
    assertEquals(
        1,
        placeholders(
            generatedEnglish.get("message.territory.remove.resize_cancelled").getAsString()));
    JsonObject generatedChinese =
        files.get("src/generated/resources/assets/economy_system/lang/zh_cn.json");
    assertNotEquals(
        generatedEnglish.get("screen.territory_remove.warning"),
        generatedChinese.get("screen.territory_remove.warning"));
    for (String key : KEYS) {
      assertEquals(
          files.get("src/generated/resources/assets/economy_system/lang/en_us.json").get(key),
          files
              .get("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json")
              .get(key));
      assertEquals(
          files.get("src/generated/resources/assets/economy_system/lang/zh_cn.json").get(key),
          files
              .get("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json")
              .get(key));
    }
  }

  private static void assertNoDuplicateKeys(String path, String content) throws Exception {
    Set<String> names = new HashSet<>();
    try (JsonReader reader = new JsonReader(new StringReader(content))) {
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        assertTrue(names.add(name), path + " duplicate key " + name);
        reader.skipValue();
      }
      reader.endObject();
    }
  }

  private static int placeholders(String text) {
    Matcher matcher = Pattern.compile("%(?:\\d+\\$)?s").matcher(text);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  private static Path findRoot() {
    Path path = Path.of("").toAbsolutePath();
    while (path != null && !Files.exists(path.resolve("settings.gradle"))) path = path.getParent();
    return Objects.requireNonNull(path);
  }
}
