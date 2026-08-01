package com.mo.economy_system.common.market;

public record DemandOrderRemovalResult(DemandOrderRemovalStatus status, MarketOrderRemoval removal) {
    public static DemandOrderRemovalResult failure(DemandOrderRemovalStatus status) {
        return new DemandOrderRemovalResult(status, null);
    }
    public static DemandOrderRemovalResult removed(MarketOrderRemoval removal) {
        return new DemandOrderRemovalResult(DemandOrderRemovalStatus.REMOVED, removal);
    }
}
