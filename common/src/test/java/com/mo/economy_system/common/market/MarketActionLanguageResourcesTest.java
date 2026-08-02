package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class MarketActionLanguageResourcesTest {
  private static final Set<String> KEYS =
      Set.of(
          "message.market.purchase.failed",
          "message.market.purchase.insufficient_funds",
          "message.market.purchase.inventory_full",
          "message.market.purchase.item_failed",
          "message.market.purchase.not_found",
          "message.market.purchase.order_changed",
          "message.market.purchase.payment_failed",
          "message.market.purchase.persist_failed",
          "message.market.purchase.rollback_failed",
          "message.market.purchase.self",
          "message.market.purchase.seller_balance_limit",
          "message.market.purchase.seller_notice",
          "message.market.purchase.success",
          "message.market.purchase.wrong_type",
          "message.market.confirm_demand.success",
          "message.market.confirm_demand.not_found",
          "message.market.confirm_demand.wrong_type",
          "message.market.confirm_demand.not_delivered",
          "message.market.confirm_demand.not_owner",
          "message.market.confirm_demand.owner_offline",
          "message.market.confirm_demand.inventory_full",
          "message.market.confirm_demand.order_changed",
          "message.market.confirm_demand.persist_failed",
          "message.market.confirm_demand.item_failed",
          "message.market.confirm_demand.rollback_failed",
          "message.market.confirm_demand.failed",
          "message.market.confirm_demand.operator_notice",
          "message.market.remove_sales.success",
          "message.market.remove_sales.not_found",
          "message.market.remove_sales.wrong_type",
          "message.market.remove_sales.not_owner",
          "message.market.remove_sales.owner_offline",
          "message.market.remove_sales.inventory_full",
          "message.market.remove_sales.order_changed",
          "message.market.remove_sales.persist_failed",
          "message.market.remove_sales.item_failed",
          "message.market.remove_sales.rollback_failed",
          "message.market.remove_sales.failed",
          "message.market.remove_sales.operator_notice");

  @Test
  void generatedAndForgeLanguagesHaveSameMarketKeysAndPlaceholders() throws Exception {
    Path root = findRoot();
    Map<String, JsonObject> files = new LinkedHashMap<>();
    for (String path :
        List.of(
            "src/generated/resources/assets/economy_system/lang/zh_cn.json",
            "src/generated/resources/assets/economy_system/lang/en_us.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json",
            "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json")) {
      String content = Files.readString(root.resolve(path));
      assertNoDuplicateKeys(path, content);
      files.put(path, JsonParser.parseString(content).getAsJsonObject());
    }
    for (var entry : files.entrySet())
      for (String key : KEYS)
        assertTrue(entry.getValue().has(key), entry.getKey() + " missing " + key);
    Set<String> baseline = marketKeys(files.values().iterator().next());
    for (JsonObject json : files.values()) assertEquals(baseline, marketKeys(json));
    for (String key : baseline) {
      int expected = count(files.values().iterator().next().get(key).getAsString());
      for (JsonObject json : files.values())
        assertEquals(expected, count(json.get(key).getAsString()), key);
    }
    for (JsonObject json : files.values()) {
      assertEquals(3, count(json.get("message.market.purchase.success").getAsString()));
      assertEquals(4, count(json.get("message.market.purchase.seller_notice").getAsString()));
      assertEquals(2, count(json.get("message.market.confirm_demand.success").getAsString()));
      assertEquals(
          3, count(json.get("message.market.confirm_demand.operator_notice").getAsString()));
      assertEquals(2, count(json.get("message.market.remove_sales.success").getAsString()));
      assertEquals(3, count(json.get("message.market.remove_sales.operator_notice").getAsString()));
    }
  }

  private static Set<String> marketKeys(JsonObject json) {
    Set<String> keys = new TreeSet<>();
    for (String key : json.keySet())
      if (key.startsWith("message.market.purchase.")
          || key.startsWith("message.market.confirm_demand.")
          || key.startsWith("message.market.remove_sales.")) keys.add(key);
    return keys;
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

  private static int count(String text) {
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
