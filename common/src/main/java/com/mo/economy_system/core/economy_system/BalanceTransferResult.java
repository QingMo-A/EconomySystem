package com.mo.economy_system.core.economy_system;

/** Outcome of one server-authoritative account transfer. */
public enum BalanceTransferResult {
    SUCCESS,
    INVALID_AMOUNT,
    SAME_ACCOUNT,
    INSUFFICIENT_FUNDS,
    RECIPIENT_BALANCE_LIMIT,
    TARGET_NOT_AVAILABLE
}
