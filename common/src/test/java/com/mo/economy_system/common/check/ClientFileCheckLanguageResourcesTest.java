package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ClientFileCheckLanguageResourcesTest {
  private static final Set<String> REQUIRED =
      Set.of(
          "message.check.sent",
          "message.check.player_only",
          "message.check.already_pending",
          "message.check.rate_limited",
          "message.check.store_full",
          "message.check.target_offline",
          "message.check.send_failed",
          "message.check.request_expired",
          "message.check.request_invalid",
          "message.check.requester_offline",
          "screen.check_consent.title",
          "screen.check_consent.requester",
          "screen.check_consent.type",
          "screen.check_consent.folder",
          "screen.check_consent.data_notice",
          "screen.check_consent.no_content_notice",
          "button.check_consent.allow",
          "button.check_consent.decline",
          "screen.check_result.title",
          "screen.check_result.target",
          "screen.check_result.type",
          "screen.check_result.success",
          "screen.check_result.declined",
          "screen.check_result.failed",
          "screen.check_result.truncated",
          "screen.check_result.only_remote",
          "screen.check_result.only_local",
          "screen.check_result.hash_changed",
          "screen.check_result.same",
          "screen.check_result.search",
          "screen.check_result.empty");

  @Test
  void generatedAndForgeLanguagesHaveStrictParity() throws Exception {
    Path root = repositoryRoot();
    JsonObject generatedEn =
        read(root.resolve("common/src/main/resources/assets/economy_system/lang/en_us.json"));
    JsonObject generatedZh =
        read(root.resolve("common/src/main/resources/assets/economy_system/lang/zh_cn.json"));
    JsonObject forgeEn =
        read(
            root.resolve(
                "common/src/main/resources/assets/economy_system/lang/en_us.json"));
    JsonObject forgeZh =
        read(
            root.resolve(
                "common/src/main/resources/assets/economy_system/lang/zh_cn.json"));
    assertTrue(generatedEn.keySet().containsAll(REQUIRED));
    assertTrue(generatedZh.keySet().containsAll(REQUIRED));
    assertTrue(forgeEn.keySet().containsAll(REQUIRED));
    assertTrue(forgeZh.keySet().containsAll(REQUIRED));
    for (String key : REQUIRED) {
      assertEquals(
          placeholders(generatedEn.get(key).getAsString()),
          placeholders(generatedZh.get(key).getAsString()));
      assertEquals(generatedEn.get(key), forgeEn.get(key));
      assertEquals(generatedZh.get(key), forgeZh.get(key));
      assertFalse(generatedEn.get(key).getAsString().contains("§"));
      assertNotEquals(generatedEn.get(key).getAsString(), generatedZh.get(key).getAsString());
    }
  }

  private static JsonObject read(Path path) throws Exception {
    String json = Files.readString(path, StandardCharsets.UTF_8);
    var matcher = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:").matcher(json);
    Set<String> keys = new HashSet<>();
    while (matcher.find())
      assertTrue(keys.add(matcher.group(1)), "duplicate language key " + matcher.group(1));
    return JsonParser.parseString(json).getAsJsonObject();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle")))
      current = current.getParent();
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }

  private static String placeholders(String value) {
    return value.replaceAll("[^%]", "");
  }
}
