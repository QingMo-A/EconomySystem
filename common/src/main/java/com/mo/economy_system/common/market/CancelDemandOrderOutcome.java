package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class CancelDemandOrderOutcome {
  private final CancelDemandOrderResult result;
  private final Optional<MarketOrder> transactionOrder;
  private final MarketMutationState mutationState;

  private CancelDemandOrderOutcome(
      CancelDemandOrderResult result, MarketOrder transactionOrder, MarketMutationState state) {
    this.result = Objects.requireNonNull(result, "result");
    this.transactionOrder = Optional.ofNullable(transactionOrder);
    this.mutationState = Objects.requireNonNull(state, "mutationState");
  }

  public static CancelDemandOrderOutcome success(MarketOrder transactionOrder) {
    requirePendingDemand(transactionOrder);
    return new CancelDemandOrderOutcome(
        CancelDemandOrderResult.SUCCESS, transactionOrder, MarketMutationState.CHANGED);
  }

  public static CancelDemandOrderOutcome validationFailure(CancelDemandOrderResult result) {
    requireFailure(result);
    return new CancelDemandOrderOutcome(result, null, MarketMutationState.UNCHANGED);
  }

  public static CancelDemandOrderOutcome rolledBackFailure(
      CancelDemandOrderResult result, MarketOrder transactionOrder) {
    requireFailure(result);
    requirePendingDemand(transactionOrder);
    return new CancelDemandOrderOutcome(result, transactionOrder, MarketMutationState.UNCHANGED);
  }

  public static CancelDemandOrderOutcome changedFailure(
      CancelDemandOrderResult result, MarketOrder transactionOrder) {
    requireFailure(result);
    if (transactionOrder != null) requirePendingDemand(transactionOrder);
    return new CancelDemandOrderOutcome(result, transactionOrder, MarketMutationState.CHANGED);
  }

  public static CancelDemandOrderOutcome uncertainFailure(CancelDemandOrderResult result) {
    requireFailure(result);
    return new CancelDemandOrderOutcome(result, null, MarketMutationState.UNKNOWN);
  }

  private static void requireFailure(CancelDemandOrderResult result) {
    if (Objects.requireNonNull(result, "result") == CancelDemandOrderResult.SUCCESS)
      throw new IllegalArgumentException("failure factory cannot use SUCCESS");
  }

  private static void requirePendingDemand(MarketOrder order) {
    Objects.requireNonNull(order, "transactionOrder");
    if (order.type() != MarketOrderType.DEMAND || order.delivered())
      throw new IllegalArgumentException("transaction order must be an undelivered demand order");
  }

  public CancelDemandOrderResult result() { return result; }
  public Optional<MarketOrder> transactionOrder() { return transactionOrder; }
  public MarketMutationState mutationState() { return mutationState; }
}
