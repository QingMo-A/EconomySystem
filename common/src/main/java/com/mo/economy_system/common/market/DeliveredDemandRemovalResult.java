package com.mo.economy_system.common.market;

import java.util.Objects;

public record DeliveredDemandRemovalResult(DeliveredDemandRemovalStatus status, MarketOrderRemoval removal) {
    public DeliveredDemandRemovalResult {
        Objects.requireNonNull(status, "status");
        if ((status == DeliveredDemandRemovalStatus.REMOVED) != (removal != null)) {
            throw new IllegalArgumentException("removal must be present exactly for REMOVED");
        }
    }
    public static DeliveredDemandRemovalResult failure(DeliveredDemandRemovalStatus status) {
        return new DeliveredDemandRemovalResult(status, null);
    }
    public static DeliveredDemandRemovalResult removed(MarketOrderRemoval removal) {
        return new DeliveredDemandRemovalResult(DeliveredDemandRemovalStatus.REMOVED, removal);
    }
}
