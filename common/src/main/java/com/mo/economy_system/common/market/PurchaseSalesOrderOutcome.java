package com.mo.economy_system.common.market;

import java.util.Optional;
import java.util.Objects;

public record PurchaseSalesOrderOutcome(PurchaseSalesOrderResult result, Optional<MarketOrder> purchasedOrder,
                                        boolean marketChanged) {
    public PurchaseSalesOrderOutcome {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(purchasedOrder, "purchasedOrder");
        if (result == PurchaseSalesOrderResult.SUCCESS && (purchasedOrder.isEmpty() || !marketChanged))
            throw new IllegalArgumentException("success requires order and market change");
    }

    public static PurchaseSalesOrderOutcome failure(PurchaseSalesOrderResult result) {
        return new PurchaseSalesOrderOutcome(result, Optional.empty(), false);
    }
    public static PurchaseSalesOrderOutcome success(MarketOrder order) {
        return new PurchaseSalesOrderOutcome(PurchaseSalesOrderResult.SUCCESS, Optional.of(order), true);
    }
    public static PurchaseSalesOrderOutcome postRemovalFailure(PurchaseSalesOrderResult result, MarketOrder order, boolean changed) {
        return new PurchaseSalesOrderOutcome(result, Optional.of(order), changed);
    }
}
