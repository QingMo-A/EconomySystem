package com.mo.economy_system.common.network;

import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.List;
import java.util.Objects;

/** Loader-neutral response containing one immutable page of balance history. */
public record BalanceLogResponseMessage(
        String category,
        int offset,
        int limit,
        int total,
        List<BalanceLogEntry> logs
) implements EconomyNetworkMessage {
    public BalanceLogResponseMessage {
        Objects.requireNonNull(category, "category");
        logs = List.copyOf(Objects.requireNonNull(logs, "logs"));
    }
}
