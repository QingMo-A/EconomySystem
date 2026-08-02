package com.mo.economy_system.common.market;

public record DemandOrderRemovalResult(DemandOrderRemovalStatus status, MarketOrderRemoval removal) {
    public DemandOrderRemovalResult {
        java.util.Objects.requireNonNull(status, "status");
        if ((status == DemandOrderRemovalStatus.REMOVED) != (removal != null)) {
            throw new IllegalArgumentException("removal must exist exactly for REMOVED");
        }
    }
    public static DemandOrderRemovalResult failure(DemandOrderRemovalStatus status) {
        if (status == DemandOrderRemovalStatus.REMOVED) throw new IllegalArgumentException("REMOVED is not a failure");
        return new DemandOrderRemovalResult(status, null);
    }
    public static DemandOrderRemovalResult removed(MarketOrderRemoval removal) {
        return new DemandOrderRemovalResult(DemandOrderRemovalStatus.REMOVED, java.util.Objects.requireNonNull(removal, "removal"));
    }
}
