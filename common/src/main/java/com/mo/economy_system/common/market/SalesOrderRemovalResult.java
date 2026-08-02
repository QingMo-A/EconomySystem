package com.mo.economy_system.common.market;

import java.util.Objects;

public record SalesOrderRemovalResult(SalesOrderRemovalStatus status, MarketOrderRemoval removal) {
    public SalesOrderRemovalResult {
        Objects.requireNonNull(status, "status");
        if ((status == SalesOrderRemovalStatus.REMOVED) != (removal != null))
            throw new IllegalArgumentException("removal must be present exactly for REMOVED");
    }
    public static SalesOrderRemovalResult failure(SalesOrderRemovalStatus status) {
        return new SalesOrderRemovalResult(status, null);
    }

    public static SalesOrderRemovalResult removed(MarketOrderRemoval removal) {
        return new SalesOrderRemovalResult(SalesOrderRemovalStatus.REMOVED, removal);
    }
}
