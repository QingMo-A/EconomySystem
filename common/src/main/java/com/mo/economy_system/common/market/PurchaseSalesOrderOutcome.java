package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class PurchaseSalesOrderOutcome {
  private final PurchaseSalesOrderResult result;
  private final Optional<MarketOrder> purchasedOrder;
  private final MarketMutationState mutationState;

  private PurchaseSalesOrderOutcome(
      PurchaseSalesOrderResult result,
      Optional<MarketOrder> purchasedOrder,
      MarketMutationState mutationState) {
    this.result = Objects.requireNonNull(result, "result");
    this.purchasedOrder = Objects.requireNonNull(purchasedOrder, "purchasedOrder");
    this.mutationState = Objects.requireNonNull(mutationState, "mutationState");
  }

  public PurchaseSalesOrderResult result() {
    return result;
  }

  public Optional<MarketOrder> purchasedOrder() {
    return purchasedOrder;
  }

  public MarketMutationState mutationState() {
    return mutationState;
  }

  public static PurchaseSalesOrderOutcome success(MarketOrder order) {
    return new PurchaseSalesOrderOutcome(
        PurchaseSalesOrderResult.SUCCESS,
        Optional.of(Objects.requireNonNull(order, "order")),
        MarketMutationState.CHANGED);
  }

  public static PurchaseSalesOrderOutcome validationFailure(PurchaseSalesOrderResult result) {
    requireFailure(result);
    return new PurchaseSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNCHANGED);
  }

  public static PurchaseSalesOrderOutcome rolledBackFailure(
      PurchaseSalesOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new PurchaseSalesOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.UNCHANGED);
  }

  public static PurchaseSalesOrderOutcome changedFailure(
      PurchaseSalesOrderResult result, MarketOrder order) {
    requireFailure(result);
    return new PurchaseSalesOrderOutcome(
        result, Optional.of(Objects.requireNonNull(order, "order")), MarketMutationState.CHANGED);
  }

  public static PurchaseSalesOrderOutcome uncertainFailure(PurchaseSalesOrderResult result) {
    requireFailure(result);
    return new PurchaseSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNKNOWN);
  }

  private static void requireFailure(PurchaseSalesOrderResult result) {
    Objects.requireNonNull(result, "result");
    if (result == PurchaseSalesOrderResult.SUCCESS) {
      throw new IllegalArgumentException("failure factory cannot use SUCCESS");
    }
  }
}
