package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public record RemoveSalesOrderOutcome(RemoveSalesOrderResult result, Optional<MarketOrder> removedOrder, boolean marketChanged) {
    public RemoveSalesOrderOutcome {
        Objects.requireNonNull(result, "result"); Objects.requireNonNull(removedOrder, "removedOrder");
        if (result == RemoveSalesOrderResult.SUCCESS && (removedOrder.isEmpty() || !marketChanged))
            throw new IllegalArgumentException("success requires removed order and market change");
    }
    public static RemoveSalesOrderOutcome failure(RemoveSalesOrderResult result) { return new RemoveSalesOrderOutcome(result, Optional.empty(), false); }
    public static RemoveSalesOrderOutcome afterRemoval(RemoveSalesOrderResult result, MarketOrder order, boolean changed) { return new RemoveSalesOrderOutcome(result, Optional.of(order), changed); }
    public static RemoveSalesOrderOutcome success(MarketOrder order) { return afterRemoval(RemoveSalesOrderResult.SUCCESS, order, true); }
}
