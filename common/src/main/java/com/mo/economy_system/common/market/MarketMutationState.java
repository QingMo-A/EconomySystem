package com.mo.economy_system.common.market;

public enum MarketMutationState {
    UNCHANGED,
    CHANGED,
    UNKNOWN;

    public boolean requiresInvalidation() {
        return this != UNCHANGED;
    }
}
