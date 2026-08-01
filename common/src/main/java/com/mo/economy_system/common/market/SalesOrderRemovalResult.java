package com.mo.economy_system.common.market;

public record SalesOrderRemovalResult(SalesOrderRemovalStatus status, MarketOrderRemoval removal) {
    public static SalesOrderRemovalResult failure(SalesOrderRemovalStatus status) {
        return new SalesOrderRemovalResult(status, null);
    }

    public static SalesOrderRemovalResult removed(MarketOrderRemoval removal) {
        return new SalesOrderRemovalResult(SalesOrderRemovalStatus.REMOVED, removal);
    }
}
