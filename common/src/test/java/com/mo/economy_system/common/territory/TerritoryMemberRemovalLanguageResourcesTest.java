package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalLanguageResourcesTest {
  private static final Set<String> KEYS =
      Set.of(
          "button.territory.member_remove",
          "button.territory.member_remove_confirm",
          "button.territory.member_remove_cancel",
          "screen.territory_members.title",
          "screen.territory_members.empty",
          "screen.territory_members.search",
          "screen.territory_member_remove.title",
          "screen.territory_member_remove.warning",
          "screen.territory_member_remove.territory",
          "screen.territory_member_remove.target",
          "message.territory.member_remove.success",
          "message.territory.member_remove.target_notice",
          "message.territory.member_remove.territory_not_found",
          "message.territory.member_remove.no_permission",
          "message.territory.member_remove.cannot_remove_owner",
          "message.territory.member_remove.target_not_member",
          "message.territory.member_remove.rate_limited",
          "message.territory.member_remove.persist_failed",
          "message.territory.member_remove.state_unknown");

  @Test
  void allFourFilesAreStrictUniqueAndCanonical() throws Exception {
    Path root = findRoot();
    Map<String, JsonObject> files = new LinkedHashMap<>();
    for (String path :
        List.of(
            "common/src/main/resources/assets/economy_system/lang/en_us.json",
            "common/src/main/resources/assets/economy_system/lang/zh_cn.json")) {
      String content = Files.readString(root.resolve(path));
      assertNoDuplicateKeys(path, content);
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      assertTrue(json.keySet().containsAll(KEYS), path);
      for (String key : KEYS)
        assertFalse(json.get(key).getAsString().contains("§"), path + " " + key);
      files.put(path, json);
    }
    JsonObject generatedEnglish =
        files.get("common/src/main/resources/assets/economy_system/lang/en_us.json");
    JsonObject generatedChinese =
        files.get("common/src/main/resources/assets/economy_system/lang/zh_cn.json");
    JsonObject forgeEnglish =
        files.get("common/src/main/resources/assets/economy_system/lang/en_us.json");
    JsonObject forgeChinese =
        files.get("common/src/main/resources/assets/economy_system/lang/zh_cn.json");
    for (String key : KEYS) {
      assertEquals(generatedEnglish.get(key), forgeEnglish.get(key), key);
      assertEquals(generatedChinese.get(key), forgeChinese.get(key), key);
      assertEquals(
          placeholders(generatedEnglish.get(key).getAsString()),
          placeholders(generatedChinese.get(key).getAsString()),
          key);
      assertNotEquals(generatedEnglish.get(key), generatedChinese.get(key), key);
    }
    assertEquals(
        2,
        placeholders(
            generatedEnglish.get("message.territory.member_remove.success").getAsString()));
    assertEquals(
        1,
        placeholders(
            generatedEnglish.get("message.territory.member_remove.target_notice").getAsString()));
    assertEquals(
        1,
        placeholders(
            generatedEnglish.get("screen.territory_member_remove.territory").getAsString()));
    assertEquals(
        1,
        placeholders(generatedEnglish.get("screen.territory_member_remove.target").getAsString()));
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
