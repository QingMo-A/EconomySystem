package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CheckedFileTransferLanguageResourcesTest {
  private static final Set<String> REQUIRED =
      Set.of(
          "screen.transfer_result.type",
          "screen.transfer_result.state",
          "screen.transfer_terminal.title",
          "screen.transfer_terminal.status",
          "screen.transfer_terminal.reason",
          "button.transfer.close",
          "message.transfer.artifact_pending",
          "message.transfer.temp_directory_provider_unsafe",
          "message.transfer.stale_session",
          "message.transfer.declined",
          "message.transfer.not_found",
          "message.transfer.stale_check",
          "message.transfer.file_changed_recheck_required",
          "message.transfer.temp_storage_limit",
          "message.transfer.expired",
          "message.transfer.save_name_exhausted",
          "message.transfer.save_parent_unsafe",
          "message.transfer.source_missing",
          "message.transfer.source_changed",
          "message.transfer.save_cleanup_pending",
          "message.transfer.move_failed",
          "message.transfer.not_pending",
          "message.transfer.delete_failed",
          "message.transfer.invalid_server_response",
          "message.transfer.status.declined",
          "message.transfer.status.not_found",
          "message.transfer.status.failed",
          "message.transfer.state.pending",
          "message.transfer.state.cleanup_pending",
          "message.transfer.state.saved",
          "message.transfer.state.discarded");
  private static final Pattern KEY = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:");
  private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[sd]");

  @Test
  void generatedAndForgeTransferLanguagesHaveStrictParity() throws Exception {
    Path root = repositoryRoot();
    JsonObject generatedEn =
        read(root.resolve("src/generated/resources/assets/economy_system/lang/en_us.json"));
    JsonObject generatedZh =
        read(root.resolve("src/generated/resources/assets/economy_system/lang/zh_cn.json"));
    JsonObject forgeEn = read(root.resolve(
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json"));
    JsonObject forgeZh = read(root.resolve(
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json"));

    for (String key : REQUIRED) {
      assertTrue(generatedEn.has(key), key);
      assertTrue(generatedZh.has(key), key);
      assertEquals(generatedEn.get(key), forgeEn.get(key), key);
      assertEquals(generatedZh.get(key), forgeZh.get(key), key);
      String english = generatedEn.get(key).getAsString();
      String chinese = generatedZh.get(key).getAsString();
      assertEquals(placeholders(english), placeholders(chinese), key);
      assertFalse(english.contains("§"), key);
      assertFalse(chinese.contains("§"), key);
      assertNotEquals(english, chinese, key);
    }
  }

  private static JsonObject read(Path path) throws Exception {
    String json = Files.readString(path, StandardCharsets.UTF_8);
    var matcher = KEY.matcher(json);
    Set<String> keys = new HashSet<>();
    while (matcher.find()) assertTrue(keys.add(matcher.group(1)), "duplicate key " + matcher.group(1));
    return JsonParser.parseString(json).getAsJsonObject();
  }

  private static List<String> placeholders(String value) {
    List<String> placeholders = new ArrayList<>();
    var matcher = PLACEHOLDER.matcher(value);
    while (matcher.find()) placeholders.add(matcher.group());
    return placeholders;
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}
