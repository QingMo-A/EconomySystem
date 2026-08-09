package com.mo.economy_system.ui.market;

import java.util.Objects;

/** A loader-neutral inventory slot snapshot used by the sales form. */
public record MarketInventoryItem(int slot, String itemId, int count, int maxStackSize) {
  public MarketInventoryItem {
    if (slot < 0 || count < 0 || maxStackSize < 1) {
      throw new IllegalArgumentException("invalid inventory item");
    }
    itemId = Objects.requireNonNull(itemId, "itemId").trim();
    if (itemId.isEmpty() || itemId.length() > 256) {
      throw new IllegalArgumentException("item id must be non-empty");
    }
  }

  public boolean filled() {
    return count > 0;
  }
}
