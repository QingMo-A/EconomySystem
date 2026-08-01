package com.mo.economy_system.common.market;

public enum DemandDeliveryTransitionResult {
    UPDATED,
    NOT_FOUND,
    WRONG_ORDER_TYPE,
    ALREADY_DELIVERED,
    PERSIST_FAILED
}
