package com.mo.economy_system.common.market;

import java.util.Objects;

public record MarketExpirationOutcome(MarketOrder order, MarketExpirationResult result) {
  public MarketExpirationOutcome {
    Objects.requireNonNull(order, "order");
    Objects.requireNonNull(result, "result");
  }

  public boolean succeeded() {
    return result == MarketExpirationResult.REFUNDED
        || result == MarketExpirationResult.RETURNED_TO_DELIVERY;
  }
}
