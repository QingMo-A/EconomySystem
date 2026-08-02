package com.mo.economy_system.common.market;
import java.util.Objects;
import java.util.Optional;
public final class CancelDemandOrderOutcome {
  private final CancelDemandOrderResult result; private final Optional<MarketOrder> authoritativeOrder; private final MarketMutationState mutationState;
  private CancelDemandOrderOutcome(CancelDemandOrderResult result, MarketOrder order, MarketMutationState state) { this.result=Objects.requireNonNull(result); authoritativeOrder=Optional.ofNullable(order); mutationState=Objects.requireNonNull(state); }
  public static CancelDemandOrderOutcome success(MarketOrder order) { requirePending(order); return new CancelDemandOrderOutcome(CancelDemandOrderResult.SUCCESS,order,MarketMutationState.CHANGED); }
  public static CancelDemandOrderOutcome validationFailure(CancelDemandOrderResult result) { failure(result); return new CancelDemandOrderOutcome(result,null,MarketMutationState.UNCHANGED); }
  public static CancelDemandOrderOutcome rolledBackFailure(CancelDemandOrderResult result,MarketOrder order) { failure(result); requirePending(order); return new CancelDemandOrderOutcome(result,order,MarketMutationState.UNCHANGED); }
  public static CancelDemandOrderOutcome changedFailure(CancelDemandOrderResult result,MarketOrder order) { failure(result); requirePending(order); return new CancelDemandOrderOutcome(result,order,MarketMutationState.CHANGED); }
  public static CancelDemandOrderOutcome uncertainFailure(CancelDemandOrderResult result) { failure(result); return new CancelDemandOrderOutcome(result,null,MarketMutationState.UNKNOWN); }
  private static void failure(CancelDemandOrderResult result) { if(Objects.requireNonNull(result)==CancelDemandOrderResult.SUCCESS)throw new IllegalArgumentException(); }
  private static void requirePending(MarketOrder order) { if(Objects.requireNonNull(order).type()!=MarketOrderType.DEMAND||order.delivered())throw new IllegalArgumentException(); }
  public CancelDemandOrderResult result(){return result;} public Optional<MarketOrder> authoritativeOrder(){return authoritativeOrder;} public MarketMutationState mutationState(){return mutationState;}
}
