package com.mo.economy_system.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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

class FinalBridgeLanguageResourcesTest {
  private static final Set<String> KEYS = keys();

  @Test
  void allFourResourcesAreStrictAndHaveMatchingBridgePlaceholders() throws Exception {
    Path root = repositoryRoot();
    Map<String, JsonObject> resources = new LinkedHashMap<>();
    for (String path : List.of(
        "src/generated/resources/assets/economy_system/lang/en_us.json",
        "src/generated/resources/assets/economy_system/lang/zh_cn.json",
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json")) {
      String content = Files.readString(root.resolve(path), StandardCharsets.UTF_8);
      assertNoDuplicateKeys(path, content);
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      assertTrue(json.keySet().containsAll(KEYS), path);
      resources.put(path, json);
    }
    for (String key : KEYS) {
      int expected = placeholders(resources.values().iterator().next().get(key).getAsString());
      for (Map.Entry<String, JsonObject> resource : resources.entrySet()) {
        assertEquals(
            expected,
            placeholders(resource.getValue().get(key).getAsString()),
            resource.getKey() + " " + key);
      }
    }
    assertEquals(3, placeholders(resources.values().iterator().next()
        .get("message.claim_wand.first_position_set").getAsString()));
    assertEquals(3, placeholders(resources.values().iterator().next()
        .get("message.claim_wand.resize_cost_details").getAsString()));
    assertEquals(2, placeholders(resources.values().iterator().next()
        .get("message.claim_wand.volume_change").getAsString()));
  }

  private static Set<String> keys() {
    Set<String> keys = new HashSet<>(List.of(
        "item.economy_system.claim_wand",
        "key.economy_system.open_delivery_box",
        "screen.delivery_box.title",
        "button.delivery_box.claim",
        "message.delivery_box.empty",
        "message.delivery_box.load_failed",
        "button.territory.manage",
        "screen.territory.manage",
        "message.territory_management.access",
        "button.territory.buff.unlock",
        "button.territory.buff.upgrade",
        "button.territory.buff.max",
        "button.territory.access.add",
        "button.territory.access.remove",
        "button.territory.transfer",
        "message.claim_wand.cancel",
        "message.claim_wand.confirm_expand",
        "message.claim_wand.confirm_shrink",
        "message.claim_wand.enter_resize_mode",
        "message.claim_wand.exit_resize_mode",
        "message.claim_wand.first_position_set",
        "message.claim_wand.second_position_set",
        "message.claim_wand.resize_cost_details",
        "message.claim_wand.resize_instruction",
        "message.claim_wand.timeout",
        "message.claim_wand.volume_change",
        "message.claim_wand.y_mismatch_error",
        "message.claim.resize_success",
        "message.claim.resize_failed",
        "message.claim.resize_insufficient_balance"));
    for (String value : List.of(
        "success", "not_found", "inventory_full", "invalid_item", "persist_failed",
        "state_unknown", "inventory_failed")) {
      keys.add("message.delivery.claim." + value);
    }
    for (String value : List.of(
        "unchanged", "overlap", "state_unknown", "refund_failed", "persist_failed", "changed",
        "payment_failed", "confirm_command", "invalid_bounds", "no_permission", "no_session",
        "not_found", "price_overflow", "unchanged_preview", "wrong_dimension")) {
      keys.add("message.claim.resize." + value);
    }
    for (String value : List.of(
        "success", "not_found", "not_owner", "wrong_dimension", "invalid_target", "self_target",
        "no_change", "invalid_buff", "already_unlocked", "not_unlocked", "max_level",
        "invalid_cost", "insufficient_balance", "insufficient_experience", "insufficient_items",
        "inventory_failed", "balance_failed", "persist_failed", "rollback_failed", "state_unknown",
        "load_failed")) {
      keys.add("message.territory.management." + value);
    }
    for (String action : List.of(
        "place_block", "break_block", "use_item", "interact_block", "open_container")) {
      keys.add("message.territory.rule." + action);
    }
    for (String level : List.of("owner_only", "members", "everyone")) {
      keys.add("message.territory.rule.level." + level);
    }
    return Set.copyOf(keys);
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

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    return Objects.requireNonNull(current, "repository root");
  }
}
