package com.mo.economy_system.common.recycle;

import java.util.Objects;

/** Immutable server-side quote for one recyclable item id. Prices are per item. */
public record RecycleOffer(
    String itemId,
    int baseUnitPrice,
    int highUnitPrice,
    int highQuota,
    boolean fallbackToBaseWhenHighQuotaExhausted) {
  public RecycleOffer {
    itemId = Objects.requireNonNull(itemId, "itemId").trim();
    if (itemId.isEmpty()) throw new IllegalArgumentException("itemId must not be blank");
    if (baseUnitPrice <= 0) throw new IllegalArgumentException("baseUnitPrice must be positive");
    if (highUnitPrice < 0 || highQuota < 0) {
      throw new IllegalArgumentException("high price and quota must be non-negative");
    }
    if (highQuota == 0 && highUnitPrice != 0) {
      throw new IllegalArgumentException("highUnitPrice requires a positive highQuota");
    }
    if (highUnitPrice > 0 && highUnitPrice < baseUnitPrice) {
      throw new IllegalArgumentException("highUnitPrice must not be below baseUnitPrice");
    }
  }

  public RecycleOffer(String itemId, int baseUnitPrice) {
    this(itemId, baseUnitPrice, 0, 0, true);
  }

  public boolean hasHighPrice() {
    return highUnitPrice > 0 && highQuota > 0;
  }
}
