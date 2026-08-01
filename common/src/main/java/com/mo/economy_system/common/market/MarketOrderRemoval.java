package com.mo.economy_system.common.market;

public record MarketOrderRemoval(MarketOrder order, Restore restore) {
    public interface Restore { MarketOrderRestoreResult restore(); }
}
