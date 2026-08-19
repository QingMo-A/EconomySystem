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
        "common/src/main/resources/assets/economy_system/lang/en_us.json",
        "common/src/main/resources/assets/economy_system/lang/zh_cn.json")) {
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
        "screen.territory.buff",
        "screen.territory.buff.title_named",
        "screen.territory.buff.search",
        "screen.territory.buff.loading",
        "screen.territory.buff.empty",
        "screen.territory.buff.sync_failed",
        "screen.territory.buff.sync_timeout",
        "screen.territory.buff.retry",
        "screen.territory.buff.level",
        "screen.territory.buff.status.unlocked",
        "screen.territory.buff.status.locked",
        "screen.territory.buff.cost",
        "screen.territory.buff.esc",
        "screen.territory.buff.availability.items",
        "screen.territory.buff.availability.experience",
        "screen.territory.buff.availability.both",
        "screen.territory.buff.availability.invalid",
        "message.territory.buff.requirements_missing",
        "screen.territory.buff.tooltip.id",
        "screen.territory.buff.tooltip.name",
        "screen.territory.buff.tooltip.level",
        "screen.territory.buff.tooltip.max_level",
        "screen.territory.buff.tooltip.effect",
        "screen.territory.buff.tooltip.unlocked",
        "screen.territory.buff.tooltip.locked",
        "screen.territory.buff.tooltip.cost",
        "screen.territory.buff.tooltip.cost.invalid",
        "screen.territory.buff.tooltip.cost.item_known",
        "screen.territory.buff.tooltip.cost.item_unknown",
        "screen.territory.buff.tooltip.cost.experience_known",
        "screen.territory.buff.tooltip.cost.experience_unknown",
        "screen.territory.buff.tooltip.cost.currency",
        "screen.territory.buff.tooltip.cost.none",
        "screen.territory.detail.title",
        "screen.territory.detail.access.title",
        "screen.territory.detail.rules.title",
        "screen.territory.detail.transfer.title",
        "screen.territory.detail.territory",
        "screen.territory.detail.owner",
        "screen.territory.detail.member_count",
        "screen.territory.detail.search",
        "screen.territory.detail.loading",
        "screen.territory.detail.sync_failed",
        "screen.territory.detail.sync_timeout",
        "screen.territory.detail.retry",
        "screen.territory.detail.empty",
        "screen.territory.detail.access.empty",
        "screen.territory.detail.rules.empty",
        "screen.territory.detail.transfer.empty",
        "screen.territory.detail.access.allowed",
        "screen.territory.detail.access.denied",
        "screen.territory.detail.rule.description",
        "screen.home.balance",
        "screen.home.trade",
        "screen.home.sell_orders",
        "screen.home.demand_orders",
        "screen.home.leaderboard",
        "screen.home.leaderboard.self",
        "screen.home.loading",
        "screen.home.sync_failed",
        "screen.home.sync_timeout",
        "screen.home.retry",
        "screen.home.leaderboard.empty",
        "screen.home.version",
        "screen.invite.sync_failed",
        "screen.invite.sync_timeout",
        "screen.invite.retry",
        "screen.territory.confirm.remove_title",
        "screen.territory.confirm.member_title",
        "screen.territory.confirm.remove_body",
        "screen.territory.confirm.member_body",
        "screen.territory.confirm.confirm",
        "screen.territory.confirm.cancel",
        "screen.market.create.sales_title",
        "screen.market.create.demand_title",
        "screen.market.create.inventory",
        "screen.market.create.selected",
        "screen.market.create.item_id",
        "screen.market.create.quantity",
        "screen.market.create.price",
        "screen.market.create.all",
        "screen.market.create.submit",
        "screen.market.create.back",
        "screen.market.create.no_item",
        "screen.market.create.unknown_item",
        "screen.market.create.invalid_quantity",
        "screen.market.create.invalid_price",
        "screen.market.confirm.title",
        "screen.market.confirm.buy_title",
        "screen.market.confirm.remove_sales_title",
        "screen.market.confirm.remove_demand_title",
        "screen.market.confirm.deliver_title",
        "screen.market.confirm.confirm_title",
        "screen.market.confirm.item",
        "screen.market.confirm.price",
        "screen.market.confirm.sales_warning",
        "screen.market.confirm.demand_warning",
        "screen.market.confirm.confirm",
        "screen.market.confirm.cancel",
        "message.market.expired.sales_return",
        "message.market.expired.demand_refunded",
        "message.market.expired.demand_delivered",
        "message.starter_kit.success",
        "message.starter_kit.already_claimed",
        "message.starter_kit.balance_limit",
        "message.starter_kit.persist_failed",
        "message.starter_kit.state_unknown",
        "message.update.available",
        "message.update.current",
        "message.update.unavailable",
        "message.update.invalid_response",
        "message.update.copy_link",
        "message.update.copy_link_hover",
        "screen.shop.purchase.title",
        "screen.shop.purchase.quantity",
        "screen.shop.purchase.unit_price",
        "screen.shop.purchase.total",
        "screen.shop.purchase.confirm",
        "screen.shop.purchase.back",
        "screen.shop.purchase.invalid_quantity",
        "screen.shop.purchase.inventory_full",
        "screen.shop.purchase.price_overflow",
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
      keys.add("message.territory.runtime.denied." + action);
    }
    keys.add("message.territory.runtime.enter");
    keys.add("message.territory.runtime.leave");
    keys.add("message.territory.runtime.welcome");
    keys.add("message.territory.runtime.owner");
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
