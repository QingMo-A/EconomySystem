package com.mo.economy_system.common.redpacket;

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

class RedPacketLanguageResourcesTest {
  private static final List<String> KEYS =
      List.of(
          RedPacketFeedback.INSUFFICIENT_BALANCE,
          RedPacketFeedback.ALREADY_ACTIVE,
          RedPacketFeedback.CREATED,
          RedPacketFeedback.NO_AVAILABLE,
          RedPacketFeedback.ALREADY_CLAIMED,
          RedPacketFeedback.CLAIM_SUCCESS,
          RedPacketFeedback.CLAIM_BUTTON,
          RedPacketFeedback.CLAIM_HOVER,
          RedPacketFeedback.BROADCAST,
          RedPacketFeedback.NO_ACTIVE,
          RedPacketFeedback.CANCELLED,
          RedPacketFeedback.FULLY_CLAIMED,
          RedPacketFeedback.EXPIRED_REFUNDED,
          RedPacketFeedback.EXPIRED_BROADCAST,
          RedPacketFeedback.CLAIM_BROADCAST,
          RedPacketFeedback.BALANCE_LIMIT,
          RedPacketFeedback.TRANSACTION_FAILED,
          RedPacketFeedback.STATE_UNKNOWN);

  @Test
  void bothTargetsContainEveryKeyWithMatchingPlaceholders() throws Exception {
    Path root = repositoryRoot();
    List<String> paths =
        List.of(
            "src/generated/resources/assets/economy_system/lang/en_us.json",
            "src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json");
    List<JsonObject> resources =
        paths.stream()
            .map(
                path -> {
                  try {
                    return JsonParser.parseString(
                            Files.readString(root.resolve(path), StandardCharsets.UTF_8))
                        .getAsJsonObject();
                  } catch (Exception error) {
                    throw new IllegalStateException(path, error);
                  }
                })
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
    assertEquals(2, placeholders(resources.get(0).get(RedPacketFeedback.CLAIM_SUCCESS).getAsString()));
    assertEquals(3, placeholders(resources.get(0).get(RedPacketFeedback.CLAIM_BROADCAST).getAsString()));
    assertEquals(1, placeholders(resources.get(0).get(RedPacketFeedback.CANCELLED).getAsString()));
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
