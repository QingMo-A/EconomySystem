package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public record PurchaseSalesOrderOutcome(
        PurchaseSalesOrderResult result,
        Optional<MarketOrder> purchasedOrder,
        MarketMutationState mutationState
) {
    public boolean marketChanged() { return mutationState.requiresInvalidation(); }
    public PurchaseSalesOrderOutcome {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(purchasedOrder, "purchasedOrder");
        Objects.requireNonNull(mutationState, "mutationState");
        if (result == PurchaseSalesOrderResult.SUCCESS
                && (purchasedOrder.isEmpty() || mutationState != MarketMutationState.CHANGED)) {
            throw new IllegalArgumentException("success requires an order and CHANGED state");
        }
    }

    public static PurchaseSalesOrderOutcome success(MarketOrder order) {
        return new PurchaseSalesOrderOutcome(PurchaseSalesOrderResult.SUCCESS,
                Optional.of(Objects.requireNonNull(order)), MarketMutationState.CHANGED);
    }

    public static PurchaseSalesOrderOutcome validationFailure(PurchaseSalesOrderResult result) {
        requireFailure(result);
        return new PurchaseSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNCHANGED);
    }

    public static PurchaseSalesOrderOutcome rolledBackFailure(PurchaseSalesOrderResult result, MarketOrder order) {
        requireFailure(result);
        return new PurchaseSalesOrderOutcome(result, Optional.of(Objects.requireNonNull(order)), MarketMutationState.UNCHANGED);
    }

    public static PurchaseSalesOrderOutcome changedFailure(PurchaseSalesOrderResult result, MarketOrder order) {
        requireFailure(result);
        return new PurchaseSalesOrderOutcome(result, Optional.of(Objects.requireNonNull(order)), MarketMutationState.CHANGED);
    }

    public static PurchaseSalesOrderOutcome uncertainFailure(PurchaseSalesOrderResult result) {
        requireFailure(result);
        return new PurchaseSalesOrderOutcome(result, Optional.empty(), MarketMutationState.UNKNOWN);
    }

    private static void requireFailure(PurchaseSalesOrderResult result) {
        Objects.requireNonNull(result, "result");
        if (result == PurchaseSalesOrderResult.SUCCESS) throw new IllegalArgumentException("failure factory cannot use SUCCESS");
    }
}
