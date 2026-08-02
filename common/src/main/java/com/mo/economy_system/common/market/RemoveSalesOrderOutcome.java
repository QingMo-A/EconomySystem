package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public record RemoveSalesOrderOutcome(RemoveSalesOrderResult result, Optional<MarketOrder> removedOrder,
                                      MarketMutationState mutationState) {
    public RemoveSalesOrderOutcome(RemoveSalesOrderResult result,Optional<MarketOrder> order,boolean changed){this(result,order,changed&&order.isEmpty()?MarketMutationState.UNKNOWN:changed?MarketMutationState.CHANGED:MarketMutationState.UNCHANGED);}
    public boolean marketChanged(){return mutationState.requiresInvalidation();}
    public RemoveSalesOrderOutcome {
        Objects.requireNonNull(result,"result");Objects.requireNonNull(removedOrder,"removedOrder");Objects.requireNonNull(mutationState,"mutationState");
        if(result==RemoveSalesOrderResult.SUCCESS&&(removedOrder.isEmpty()||mutationState!=MarketMutationState.CHANGED))throw new IllegalArgumentException("success requires order and CHANGED state");
    }
    public static RemoveSalesOrderOutcome success(MarketOrder order){return new RemoveSalesOrderOutcome(RemoveSalesOrderResult.SUCCESS,Optional.of(Objects.requireNonNull(order)),MarketMutationState.CHANGED);}
    public static RemoveSalesOrderOutcome failure(RemoveSalesOrderResult result){return validationFailure(result);}
    public static RemoveSalesOrderOutcome afterRemoval(RemoveSalesOrderResult result,MarketOrder order,boolean changed){return changed?changedFailure(result,order):rolledBackFailure(result,order);}
    public static RemoveSalesOrderOutcome validationFailure(RemoveSalesOrderResult result){requireFailure(result);return new RemoveSalesOrderOutcome(result,Optional.empty(),MarketMutationState.UNCHANGED);}
    public static RemoveSalesOrderOutcome rolledBackFailure(RemoveSalesOrderResult result,MarketOrder order){requireFailure(result);return new RemoveSalesOrderOutcome(result,Optional.of(Objects.requireNonNull(order)),MarketMutationState.UNCHANGED);}
    public static RemoveSalesOrderOutcome changedFailure(RemoveSalesOrderResult result,MarketOrder order){requireFailure(result);return new RemoveSalesOrderOutcome(result,Optional.of(Objects.requireNonNull(order)),MarketMutationState.CHANGED);}
    public static RemoveSalesOrderOutcome uncertainFailure(RemoveSalesOrderResult result){requireFailure(result);return new RemoveSalesOrderOutcome(result,Optional.empty(),MarketMutationState.UNKNOWN);}
    private static void requireFailure(RemoveSalesOrderResult result){Objects.requireNonNull(result);if(result==RemoveSalesOrderResult.SUCCESS)throw new IllegalArgumentException("failure factory cannot use SUCCESS");}
}
