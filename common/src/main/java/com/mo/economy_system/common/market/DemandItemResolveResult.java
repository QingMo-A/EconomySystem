package com.mo.economy_system.common.market;

public record DemandItemResolveResult(ResolvedDemandItem value, Error error) {
    public enum Error { INVALID_ITEM_ID, ITEM_NOT_FOUND, SNAPSHOT_REJECTED }

    public static DemandItemResolveResult success(ResolvedDemandItem value) {
        return new DemandItemResolveResult(value, null);
    }

    public static DemandItemResolveResult failure(Error error) {
        return new DemandItemResolveResult(null, error);
    }

    public boolean isSuccess() { return value != null && error == null; }
}
