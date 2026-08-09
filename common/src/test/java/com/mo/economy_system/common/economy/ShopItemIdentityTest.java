package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ShopItemIdentityTest {
  @Test
  void preservesExistingIdentity() {
    assertEquals(
        "existing-id",
        ShopItemIdentity.existingOrDeterministic(
            "existing-id", "minecraft:stone", "{}", "payload"));
  }

  @Test
  void legacyFallbackIsStableAndSensitiveToItemPayload() {
    String first = ShopItemIdentity.existingOrDeterministic(
        "", "minecraft:stone", "{Damage:1}", "payload");
    String second = ShopItemIdentity.existingOrDeterministic(
        null, "minecraft:stone", "{Damage:1}", "payload");
    String changed = ShopItemIdentity.existingOrDeterministic(
        "", "minecraft:stone", "{Damage:2}", "payload");

    assertEquals(first, second);
    assertNotEquals(first, changed);
  }
}
