package com.mo.economy_system.common.market;

import java.util.*;

public final class DemandOrderDeliveryOutcome {
  private final DemandOrderDeliveryResult result;
  private final Optional<MarketOrder> deliveredOrder;
  private final MarketMutationState mutationState;

  private DemandOrderDeliveryOutcome(
      DemandOrderDeliveryResult r, Optional<MarketOrder> o, MarketMutationState s) {
    result = Objects.requireNonNull(r);
    deliveredOrder = Objects.requireNonNull(o);
    mutationState = Objects.requireNonNull(s);
  }

  public DemandOrderDeliveryResult result() {
    return result;
  }

  public Optional<MarketOrder> deliveredOrder() {
    return deliveredOrder;
  }

  public MarketMutationState mutationState() {
    return mutationState;
  }

  public static DemandOrderDeliveryOutcome success(MarketOrder o) {
    requireDemand(o, true);
    return new DemandOrderDeliveryOutcome(
        DemandOrderDeliveryResult.SUCCESS,
        Optional.of(Objects.requireNonNull(o)),
        MarketMutationState.CHANGED);
  }

  public static DemandOrderDeliveryOutcome validationFailure(DemandOrderDeliveryResult r) {
    failure(r);
    return new DemandOrderDeliveryOutcome(r, Optional.empty(), MarketMutationState.UNCHANGED);
  }

  public static DemandOrderDeliveryOutcome rolledBackFailure(
      DemandOrderDeliveryResult r, MarketOrder o) {
    failure(r);
    requireDemand(o, false);
    return new DemandOrderDeliveryOutcome(
        r, Optional.of(Objects.requireNonNull(o)), MarketMutationState.UNCHANGED);
  }

  public static DemandOrderDeliveryOutcome changedFailure(
      DemandOrderDeliveryResult r, MarketOrder o) {
    failure(r);
    requireDemand(o, false);
    return new DemandOrderDeliveryOutcome(
        r, Optional.of(Objects.requireNonNull(o)), MarketMutationState.CHANGED);
  }

  public static DemandOrderDeliveryOutcome uncertainFailure(DemandOrderDeliveryResult r) {
    failure(r);
    return new DemandOrderDeliveryOutcome(r, Optional.empty(), MarketMutationState.UNKNOWN);
  }

  private static void failure(DemandOrderDeliveryResult r) {
    if (Objects.requireNonNull(r) == DemandOrderDeliveryResult.SUCCESS)
      throw new IllegalArgumentException();
  }

  private static void requireDemand(MarketOrder order, boolean delivered) {
    Objects.requireNonNull(order, "order");
    if (order.type() != MarketOrderType.DEMAND || order.delivered() != delivered)
      throw new IllegalArgumentException("invalid demand delivery order state");
  }
}
