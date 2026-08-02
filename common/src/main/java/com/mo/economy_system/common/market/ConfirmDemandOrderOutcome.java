package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public record ConfirmDemandOrderOutcome(ConfirmDemandOrderResult result, Optional<MarketOrder> confirmedOrder,
                                        MarketMutationState mutationState) {
    public ConfirmDemandOrderOutcome(ConfirmDemandOrderResult result, Optional<MarketOrder> order, boolean changed){this(result,order,changed&&order.isEmpty()?MarketMutationState.UNKNOWN:changed?MarketMutationState.CHANGED:MarketMutationState.UNCHANGED);}
    public boolean marketChanged(){return mutationState.requiresInvalidation();}
    public ConfirmDemandOrderOutcome {
        Objects.requireNonNull(result, "result"); Objects.requireNonNull(confirmedOrder, "confirmedOrder"); Objects.requireNonNull(mutationState, "mutationState");
        if (result == ConfirmDemandOrderResult.SUCCESS && (confirmedOrder.isEmpty() || mutationState != MarketMutationState.CHANGED)) throw new IllegalArgumentException("success requires order and CHANGED state");
    }
    public static ConfirmDemandOrderOutcome success(MarketOrder order){return new ConfirmDemandOrderOutcome(ConfirmDemandOrderResult.SUCCESS,Optional.of(Objects.requireNonNull(order)),MarketMutationState.CHANGED);}
    public static ConfirmDemandOrderOutcome failure(ConfirmDemandOrderResult result){return validationFailure(result);}
    public static ConfirmDemandOrderOutcome afterRemoval(ConfirmDemandOrderResult result,MarketOrder order,boolean changed){return changed?changedFailure(result,order):rolledBackFailure(result,order);}
    public static ConfirmDemandOrderOutcome validationFailure(ConfirmDemandOrderResult result){requireFailure(result);return new ConfirmDemandOrderOutcome(result,Optional.empty(),MarketMutationState.UNCHANGED);}
    public static ConfirmDemandOrderOutcome rolledBackFailure(ConfirmDemandOrderResult result,MarketOrder order){requireFailure(result);return new ConfirmDemandOrderOutcome(result,Optional.of(Objects.requireNonNull(order)),MarketMutationState.UNCHANGED);}
    public static ConfirmDemandOrderOutcome changedFailure(ConfirmDemandOrderResult result,MarketOrder order){requireFailure(result);return new ConfirmDemandOrderOutcome(result,Optional.of(Objects.requireNonNull(order)),MarketMutationState.CHANGED);}
    public static ConfirmDemandOrderOutcome uncertainFailure(ConfirmDemandOrderResult result){requireFailure(result);return new ConfirmDemandOrderOutcome(result,Optional.empty(),MarketMutationState.UNKNOWN);}
    private static void requireFailure(ConfirmDemandOrderResult result){Objects.requireNonNull(result);if(result==ConfirmDemandOrderResult.SUCCESS)throw new IllegalArgumentException("failure factory cannot use SUCCESS");}
}
