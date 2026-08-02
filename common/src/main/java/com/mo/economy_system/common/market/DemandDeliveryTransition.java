package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class DemandDeliveryTransition {
  private final DemandDeliveryTransitionStatus status;
  private final Optional<MarketOrder> previousOrder;
  private final Optional<MarketOrder> updatedOrder;

  private DemandDeliveryTransition(
      DemandDeliveryTransitionStatus status,
      Optional<MarketOrder> previous,
      Optional<MarketOrder> updated) {
    this.status = Objects.requireNonNull(status);
    this.previousOrder = Objects.requireNonNull(previous);
    this.updatedOrder = Objects.requireNonNull(updated);
  }

  public static DemandDeliveryTransition updated(MarketOrder previous, MarketOrder updated) {
    Objects.requireNonNull(previous);
    Objects.requireNonNull(updated);
    if (previous.type() != MarketOrderType.DEMAND
        || previous.delivered()
        || updated.type() != MarketOrderType.DEMAND
        || !updated.delivered()) throw new IllegalArgumentException("invalid delivered transition");
    MarketOrder expected =
        new MarketOrder(
            previous.type(),
            previous.tradeId(),
            previous.item(),
            previous.quantity(),
            previous.totalPrice(),
            previous.sellerName(),
            previous.sellerId(),
            previous.listingTime(),
            previous.expirationTime(),
            true);
    if (!expected.equals(updated))
      throw new IllegalArgumentException("orders differ beyond delivered");
    return new DemandDeliveryTransition(
        DemandDeliveryTransitionStatus.UPDATED, Optional.of(previous), Optional.of(updated));
  }

  public static DemandDeliveryTransition failure(DemandDeliveryTransitionStatus status) {
    Objects.requireNonNull(status);
    if (status == DemandDeliveryTransitionStatus.UPDATED) throw new IllegalArgumentException();
    return new DemandDeliveryTransition(status, Optional.empty(), Optional.empty());
  }

  public DemandDeliveryTransitionStatus status() {
    return status;
  }

  public Optional<MarketOrder> previousOrder() {
    return previousOrder;
  }

  public Optional<MarketOrder> updatedOrder() {
    return updatedOrder;
  }
}
