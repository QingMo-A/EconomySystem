package com.mo.economy_system.common.economy;

public enum ShopPurchaseResult {
    SUCCESS,
    INVALID_QUANTITY,
    INVALID_ITEM,
    INVALID_PRICE,
    INSUFFICIENT_FUNDS,
    INVENTORY_FULL,
    DELIVERY_FAILED,
    PAYMENT_FAILED,
    REFUND_FAILED,
    ROLLBACK_FAILED,
    STATE_UNKNOWN
}
