package com.mo.economy_system.ui.delivery;

import java.util.Objects;

/** Loader-neutral snapshot of one occupied player inventory slot. */
public record MailboxComposeInventoryItem(int slot, String itemId, int count) {
  public MailboxComposeInventoryItem {
    if (slot < 0 || slot >= 36 || count < 1) throw new IllegalArgumentException("invalid inventory item");
    itemId = Objects.requireNonNull(itemId, "itemId");
    if (itemId.isBlank()) throw new IllegalArgumentException("blank item id");
  }
}
