package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class ConfirmDemandOrderOutcome {
  private final ConfirmDemandOrderResult result;
  private final Optional<MarketOrder> confirmedOrder;
  private final MarketMutationState mutationState;

  private ConfirmDemandOrderOutcome(
      ConfirmDemandOrderResult result,
      Optional<MarketOrder> confirmedOrder,
      MarketMutationState mutationState) {
    this.result = Objects.requireNonNull(result, "result");
    this.confirmedOrder = Objects.requireNonNull(confirmedOrder, "confirmedOrder");
    this.mutationState = Objects.requireNonNull(mutationState, "mutationState");
  }

  public ConfirmDemandOrderResult result() {
    return result;
  }

  public Optional<MarketOrder> confirmedOrder() {
    return confirmedOrder;
  }

  public MarketMutationState mutationState() {
    return mutationState;
  }

  public static ConfirmDemandOrderOutcome success(MarketOrder order) {
    return new ConfirmDemandOrderOutcome(
        ConfirmDemandOrderResult.SUCCESS,
        Optional.of(Objects.requireNonNull(order, "order")),
        MarketMutationState.CHANGED);
  }

  public static ConfirmDemandOrderOutcome validationFailure(ConfirmDemandOrderResult result) {
    requireFailure(result);
    return new ConfirmDemandOrderOutcome(result, Optional.empty(), MarketMutationState.UNCHANGED);
  }

  public static ConfirmDemandOrderOutcome rolledBackFailure(
      ConfirmDemandOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new ConfirmDemandOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.UNCHANGED);
  }

  public static ConfirmDemandOrderOutcome changedFailure(
      ConfirmDemandOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new ConfirmDemandOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.CHANGED);
  }

  public static ConfirmDemandOrderOutcome uncertainFailure(ConfirmDemandOrderResult result) {
    requireFailure(result);
    return new ConfirmDemandOrderOutcome(result, Optional.empty(), MarketMutationState.UNKNOWN);
  }

  private static void requireFailure(ConfirmDemandOrderResult result) {
    Objects.requireNonNull(result, "result");
    if (result == ConfirmDemandOrderResult.SUCCESS) {
      throw new IllegalArgumentException("failure factory cannot use SUCCESS");
    }
  }
}
