package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ShopCatalogDefaultsTest {
  @Test
  void baselineCatalogIsStableCompleteAndUniquelyAddressable() {
    var first = ShopCatalogDefaults.snapshots();
    var second = ShopCatalogDefaults.snapshots();

    assertEquals(77, first.size());
    assertEquals(first, second);
    assertEquals(77, new HashSet<>(first.stream().map(item -> item.shopItemId()).toList()).size());
    assertTrue(first.stream().anyMatch(item -> item.itemId().equals("economy_system:recall_potion")));
    assertTrue(first.stream().anyMatch(item -> item.itemId().equals("economy_system:wormhole_potion")));
    assertTrue(first.stream().anyMatch(item -> item.itemId().equals("minecraft:cherry_log")));
    assertTrue(first.stream().anyMatch(item -> item.itemId().equals("minecraft:black_wool")));
    assertEquals(2, first.stream().filter(item -> item.itemId().equals("minecraft:enchanted_book")).count());
    assertTrue(first.stream().allMatch(item -> item.basePrice() > 0));
    assertTrue(first.stream().allMatch(item -> item.currentPrice() == item.basePrice()));
    assertFalse(first.stream().anyMatch(item -> item.description().isBlank()));
  }
}
