package com.mo.economy_system.common.economy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Stable identity fallback for legacy shop entries that predate {@code shopItemId}. */
public final class ShopItemIdentity {
  private ShopItemIdentity() {}

  public static String existingOrDeterministic(
      String shopItemId, String itemId, String legacyNbt, String itemData) {
    if (shopItemId != null && !shopItemId.isBlank()) return shopItemId;
    String seed = value(itemId) + '\n' + value(legacyNbt) + '\n' + value(itemData);
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
