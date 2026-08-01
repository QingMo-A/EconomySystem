package com.mo.economy_system.common.market;

import java.util.Optional;

public record PurchaseSalesOrderOutcome(PurchaseSalesOrderResult result, Optional<MarketOrder> purchasedOrder,
                                        boolean marketChanged) {
    public PurchaseSalesOrderOutcome {
        purchasedOrder = purchasedOrder == null ? Optional.empty() : purchasedOrder;
    }

    public static PurchaseSalesOrderOutcome failure(PurchaseSalesOrderResult result) {
        return new PurchaseSalesOrderOutcome(result, Optional.empty(), false);
    }
}
