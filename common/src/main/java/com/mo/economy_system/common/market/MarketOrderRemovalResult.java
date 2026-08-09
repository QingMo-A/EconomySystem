package com.mo.economy_system.common.market;

import java.util.Objects;

public record MarketOrderRemovalResult(
    MarketOrderRemovalStatus status, MarketOrderRemoval removal) {
  public MarketOrderRemovalResult {
    Objects.requireNonNull(status, "status");
    if ((status == MarketOrderRemovalStatus.REMOVED) != (removal != null)) {
      throw new IllegalArgumentException("removal must exist exactly for REMOVED");
    }
  }

  public static MarketOrderRemovalResult failure(MarketOrderRemovalStatus status) {
    if (status == MarketOrderRemovalStatus.REMOVED) {
      throw new IllegalArgumentException("REMOVED is not a failure");
    }
    return new MarketOrderRemovalResult(status, null);
  }

  public static MarketOrderRemovalResult removed(MarketOrderRemoval removal) {
    return new MarketOrderRemovalResult(
        MarketOrderRemovalStatus.REMOVED, Objects.requireNonNull(removal, "removal"));
  }
}
