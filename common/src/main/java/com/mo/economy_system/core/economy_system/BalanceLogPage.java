package com.mo.economy_system.core.economy_system;

import java.util.List;
import java.util.Objects;

/** A loader-neutral, immutable page of balance history. */
public record BalanceLogPage(
        List<BalanceLogEntry> logs,
        String category,
        int offset,
        int limit,
        int total
) {
    public BalanceLogPage {
        logs = List.copyOf(Objects.requireNonNull(logs, "logs"));
    }
}
