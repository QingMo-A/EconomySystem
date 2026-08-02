package com.mo.economy_system.common.market;

import java.util.Objects;

public record MarketOrderRemoval(MarketOrder order, Restore restore) {
    public MarketOrderRemoval {
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(restore, "restore");
    }
    public interface Restore { MarketOrderRestoreResult restore(); }
}
