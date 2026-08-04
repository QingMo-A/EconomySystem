package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class TerritoryInviteLanguageResourcesTest {
  private static final Set<String> KEYS = Set.of(
      "screen.invite.title", "screen.invite.search", "screen.invite.loading",
      "screen.invite.empty", "screen.invite.territory", "button.invite.back",
      "button.invite.invite", "button.invite.accept", "button.invite.decline",
      "button.territory.invite", "message.invite.sent", "message.invite.received",
      "message.invite.accept", "message.invite.decline", "message.invite.territory_not_found",
      "message.invite.no_permission", "message.invite.target_offline",
      "message.invite.cannot_invite_owner", "message.invite.cannot_invite_self",
      "message.invite.already_member", "message.invite.already_pending",
      "message.invite.rate_limited", "message.invite.store_full",
      "message.invite.create_failed", "message.invite.accepted", "message.invite.declined",
      "message.invite.accepted_by", "message.invite.declined_by", "message.invite.no_pending",
      "message.invite.not_found", "message.invite.not_target", "message.invite.owner_changed",
      "message.invite.persist_failed", "message.invite.state_unknown", "message.invite.busy",
      "message.invite.multiple_pending");

  @Test void actualResourcesAreStrictCanonicalAndPlaceholderCompatible() throws Exception {
    Path root = findRoot();
    Map<String, JsonObject> files = new LinkedHashMap<>();
    for (String path : List.of(
        "src/generated/resources/assets/economy_system/lang/en_us.json",
        "src/generated/resources/assets/economy_system/lang/zh_cn.json",
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json",
        "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json")) {
      String content = Files.readString(root.resolve(path));
      assertNoDuplicateKeys(path, content);
      JsonObject json = JsonParser.parseString(content).getAsJsonObject();
      assertEquals(KEYS, inviteKeys(json), path);
      files.put(path, json);
    }
    JsonObject baseline = files.values().iterator().next();
    for (String key : KEYS) {
      int expected = placeholders(baseline.get(key).getAsString());
      for (var file : files.entrySet())
        assertEquals(expected, placeholders(file.getValue().get(key).getAsString()),
            file.getKey() + " " + key);
    }
    assertEquals(2, placeholders(baseline.get("message.invite.sent").getAsString()));
    assertEquals(2, placeholders(baseline.get("message.invite.received").getAsString()));
    assertEquals(1, placeholders(baseline.get("message.invite.accepted").getAsString()));
    assertEquals(1, placeholders(baseline.get("message.invite.declined").getAsString()));
    assertEquals(2, placeholders(baseline.get("message.invite.accepted_by").getAsString()));
    assertEquals(2, placeholders(baseline.get("message.invite.declined_by").getAsString()));

    JsonObject forgeEnglish = files.get("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json");
    JsonObject forgeChinese = files.get("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json");
    assertTrue(KEYS.stream().anyMatch(key -> !forgeEnglish.get(key).equals(forgeChinese.get(key))));
    assertEquals("我的领地", forgeChinese.get("screen.territory.title").getAsString());
    assertEquals("已传送至 %s", forgeChinese.get("message.teleport.success").getAsString());
    assertEquals("购买失败", forgeChinese.get("message.market.purchase.failed").getAsString());
  }

  private static Set<String> inviteKeys(JsonObject json) {
    Set<String> result = new HashSet<>();
    for (String key : json.keySet())
      if (key.startsWith("screen.invite.") || key.startsWith("button.invite.")
          || key.equals("button.territory.invite") || key.startsWith("message.invite."))
        if (KEYS.contains(key)) result.add(key);
    return result;
  }

  private static void assertNoDuplicateKeys(String path, String content) throws Exception {
    Set<String> names = new HashSet<>();
    try (JsonReader reader = new JsonReader(new StringReader(content))) {
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        assertTrue(names.add(name), path + " contains duplicate key " + name);
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
    return Objects.requireNonNull(path, "workspace root");
  }
}
