package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class RemoveSalesOrderOutcome {
  private final RemoveSalesOrderResult result;
  private final Optional<MarketOrder> removedOrder;
  private final MarketMutationState mutationState;

  private RemoveSalesOrderOutcome(
      RemoveSalesOrderResult result,
      Optional<MarketOrder> removedOrder,
      MarketMutationState mutationState) {
    this.result = Objects.requireNonNull(result, "result");
    this.removedOrder = Objects.requireNonNull(removedOrder, "removedOrder");
    this.mutationState = Objects.requireNonNull(mutationState, "mutationState");
  }

  public RemoveSalesOrderResult result() {
    return result;
  }

  public Optional<MarketOrder> removedOrder() {
    return removedOrder;
  }

  public MarketMutationState mutationState() {
    return mutationState;
  }

  public static RemoveSalesOrderOutcome success(MarketOrder order) {
    return new RemoveSalesOrderOutcome(
        RemoveSalesOrderResult.SUCCESS,
        Optional.of(Objects.requireNonNull(order, "order")),
        MarketMutationState.CHANGED);
  }

  public static RemoveSalesOrderOutcome validationFailure(RemoveSalesOrderResult result) {
    requireFailure(result);
    return new RemoveSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNCHANGED);
  }

  public static RemoveSalesOrderOutcome rolledBackFailure(
      RemoveSalesOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new RemoveSalesOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.UNCHANGED);
  }

  public static RemoveSalesOrderOutcome changedFailure(
      RemoveSalesOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new RemoveSalesOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.CHANGED);
  }

  public static RemoveSalesOrderOutcome uncertainFailure(RemoveSalesOrderResult result) {
    requireFailure(result);
    return new RemoveSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNKNOWN);
  }

  private static void requireFailure(RemoveSalesOrderResult result) {
    Objects.requireNonNull(result, "result");
    if (result == RemoveSalesOrderResult.SUCCESS) {
      throw new IllegalArgumentException("failure factory cannot use SUCCESS");
    }
  }
}
