package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Loader-neutral request for one page of the current player's balance history. */
public record BalanceLogRequestMessage(
        String category,
        int offset,
        int limit
) implements EconomyNetworkMessage {
    public static final String ALL_CATEGORIES = "全部";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES;

    public BalanceLogRequestMessage {
        category = category == null || category.isBlank() ? ALL_CATEGORIES : category;
        offset = Math.max(0, offset);
        limit = Math.max(1, Math.min(MAX_LIMIT, limit));
    }

    public BalanceLogRequestMessage() {
        this(ALL_CATEGORIES, 0, DEFAULT_LIMIT);
    }
}
