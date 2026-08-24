package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

/** Result of one compare-and-swap partial/full market fill reservation. */
public final class MarketPartialFillTransition {
  @FunctionalInterface
  public interface Rollback {
    MarketPartialFillRollbackResult rollback();
  }

  private final MarketPartialFillStatus status;
  private final Optional<MarketOrder> previousOrder;
  private final Optional<MarketOrder> remainingOrder;
  private final int filledQuantity;
  private final int amount;
  private final Optional<Rollback> rollback;

  private MarketPartialFillTransition(
      MarketPartialFillStatus status,
      Optional<MarketOrder> previousOrder,
      Optional<MarketOrder> remainingOrder,
      int filledQuantity,
      int amount,
      Optional<Rollback> rollback) {
    this.status = Objects.requireNonNull(status);
    this.previousOrder = Objects.requireNonNull(previousOrder);
    this.remainingOrder = Objects.requireNonNull(remainingOrder);
    this.filledQuantity = filledQuantity;
    this.amount = amount;
    this.rollback = Objects.requireNonNull(rollback);
  }

  public static MarketPartialFillTransition applied(
      MarketOrder previous,
      MarketOrder remaining,
      int filledQuantity,
      int amount,
      Rollback rollback) {
    Objects.requireNonNull(previous);
    Objects.requireNonNull(rollback);
    if (filledQuantity <= 0 || amount <= 0) throw new IllegalArgumentException("invalid fill");
    MarketPartialFillStatus status = remaining == null
        ? MarketPartialFillStatus.REMOVED : MarketPartialFillStatus.UPDATED;
    return new MarketPartialFillTransition(status, Optional.of(previous), Optional.ofNullable(remaining),
        filledQuantity, amount, Optional.of(rollback));
  }

  public static MarketPartialFillTransition failure(MarketPartialFillStatus status) {
    Objects.requireNonNull(status);
    if (status == MarketPartialFillStatus.UPDATED || status == MarketPartialFillStatus.REMOVED) {
      throw new IllegalArgumentException("success status cannot be a failure");
    }
    return new MarketPartialFillTransition(status, Optional.empty(), Optional.empty(), 0, 0, Optional.empty());
  }

  public MarketPartialFillStatus status() { return status; }
  public Optional<MarketOrder> previousOrder() { return previousOrder; }
  public Optional<MarketOrder> remainingOrder() { return remainingOrder; }
  public int filledQuantity() { return filledQuantity; }
  public int amount() { return amount; }
  public Optional<Rollback> rollback() { return rollback; }
  public boolean applied() {
    return status == MarketPartialFillStatus.UPDATED || status == MarketPartialFillStatus.REMOVED;
  }
}
