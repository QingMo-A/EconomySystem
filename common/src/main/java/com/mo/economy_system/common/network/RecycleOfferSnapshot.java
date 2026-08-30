package com.mo.economy_system.common.network;

import java.util.Objects;

/** Server-authoritative recycling quote plus the player's current inventory count. */
public record RecycleOfferSnapshot(String itemId, int baseUnitPrice, int highUnitPrice,
                                   int highQuotaRemaining, int ownedCount, int maxStackSize,
                                   boolean fallbackToBaseWhenHighQuotaExhausted) {
  public RecycleOfferSnapshot {
    itemId = Objects.requireNonNull(itemId, "itemId").trim();
    if (itemId.isEmpty() || baseUnitPrice <= 0 || highUnitPrice < 0 || highQuotaRemaining < 0
        || ownedCount < 0 || maxStackSize <= 0) throw new IllegalArgumentException("invalid recycle offer snapshot");
    if (highUnitPrice == 0 && highQuotaRemaining != 0) throw new IllegalArgumentException("high quota without high price");
  }

  public int currentUnitPrice() { return highUnitPrice > 0 && highQuotaRemaining > 0 ? highUnitPrice : baseUnitPrice; }
  public int maxSubmitAmount() { return Math.min(ownedCount, EconomyNetworkLimits.MAX_RECYCLE_SUBMIT_AMOUNT); }
}
