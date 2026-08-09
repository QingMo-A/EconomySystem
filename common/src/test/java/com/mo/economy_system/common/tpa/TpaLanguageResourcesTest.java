package com.mo.economy_system.common.tpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TpaLanguageResourcesTest {
  private static final List<String> KEYS =
      List.of(
          TpaFeedback.SELF,
          TpaFeedback.NO_POTION,
          TpaFeedback.REQUEST_SENT,
          TpaFeedback.REQUEST_RECEIVED,
          TpaFeedback.ACCEPT,
          TpaFeedback.ACCEPT_BUTTON,
          TpaFeedback.DENY,
          TpaFeedback.DENY_BUTTON,
          TpaFeedback.NO_REQUEST,
          TpaFeedback.SENDER_OFFLINE,
          TpaFeedback.SENDER_NO_POTION,
          TpaFeedback.TELEPORTED,
          TpaFeedback.ACCEPTED,
          TpaFeedback.DENIED_SENDER,
          TpaFeedback.DENIED_TARGET,
          TpaFeedback.TIMEOUT_SENDER,
          TpaFeedback.TIMEOUT_TARGET,
          TpaFeedback.TARGET_BUSY,
          TpaFeedback.SENDER_BUSY,
          TpaFeedback.CAPACITY,
          TpaFeedback.INVENTORY_FAILED,
          TpaFeedback.TELEPORT_FAILED,
          TpaFeedback.STATE_UNKNOWN,
          TpaFeedback.ROLLBACK_FAILED,
          "item.economy_system.wormhole_potion");

  @Test
  void activeAndCanonicalResourcesContainEveryKeyWithMatchingPlaceholders() {
    Path root = repositoryRoot();
    List<String> paths =
        List.of(
            "src/generated/resources/assets/economy_system/lang/en_us.json",
            "src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "targets/neoforge-1.21.1/src/generated/resources/assets/economy_system/lang/en_us.json",
            "targets/neoforge-1.21.1/src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json");
    List<JsonObject> resources = paths.stream().map(path -> read(root.resolve(path))).toList();
    for (String key : KEYS) {
      for (int index = 0; index < resources.size(); index++) {
        assertTrue(resources.get(index).has(key), paths.get(index) + " missing " + key);
      }
      int expected = placeholders(resources.get(0).get(key).getAsString());
      for (int index = 1; index < resources.size(); index++) {
        assertEquals(
            expected,
            placeholders(resources.get(index).get(key).getAsString()),
            paths.get(index) + " " + key);
      }
    }
  }

  private static JsonObject read(Path path) {
    try {
      return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (Exception error) {
      throw new IllegalStateException(path.toString(), error);
    }
  }

  private static int placeholders(String text) {
    Matcher matcher = Pattern.compile("%(?:\\d+\\$)?s").matcher(text);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    return Objects.requireNonNull(current, "repository root");
  }
}
