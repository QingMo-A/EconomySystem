package com.mo.economy_system.common.reward;

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

class RewardLanguageResourcesTest {
  private static final List<String> KEYS =
      List.of(
          RewardFeedback.SUCCESS,
          RewardFeedback.BALANCE_LIMIT,
          RewardFeedback.TRANSACTION_FAILED,
          RewardFeedback.STATE_UNKNOWN,
          "enchantment.economy_system.carefully",
          "enchantment.economy_system.carefully.desc",
          "enchantment.economy_system.bounty_hunter",
          "enchantment.economy_system.bounty_hunter.desc");

  @Test
  void bothTargetsContainRewardKeysWithMatchingPlaceholders() throws Exception {
    Path root = repositoryRoot();
    List<String> paths =
        List.of(
            "src/generated/resources/assets/economy_system/lang/en_us.json",
            "src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json");
    List<JsonObject> resources =
        paths.stream()
            .map(path -> read(root.resolve(path)))
            .toList();
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
    assertEquals(2, placeholders(resources.get(0).get(RewardFeedback.SUCCESS).getAsString()));
  }

  private static JsonObject read(Path path) {
    try {
      return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (Exception error) {
      throw new IllegalStateException(path.toString(), error);
    }
  }

  private static int placeholders(String value) {
    Matcher matcher = Pattern.compile("%(?:\\d+\\$)?s").matcher(value);
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
