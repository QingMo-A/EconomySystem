package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loader-neutral client cache for the latest balance-log page.
 *
 * <p>All page fields and entries are published through one immutable snapshot,
 * preventing readers from observing pagination metadata from a different
 * response than the displayed entries.</p>
 */
public final class ClientBalanceLogState {
    private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(
            new Snapshot(
                    BalanceLogRequestMessage.ALL_CATEGORIES,
                    0,
                    BalanceLogRequestMessage.DEFAULT_LIMIT,
                    0,
                    List.of()
            )
    );

    private ClientBalanceLogState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(
            String category,
            int offset,
            int limit,
            int total,
            List<BalanceLogEntry> logs
    ) {
        CURRENT.set(new Snapshot(category, offset, limit, total, logs));
    }

    public static void update(BalanceLogResponseMessage message) {
        Objects.requireNonNull(message, "message");
        update(message.category(), message.offset(), message.limit(), message.total(), message.logs());
    }

    /** One atomically published, immutable balance-log page. */
    public record Snapshot(
            String category,
            int offset,
            int limit,
            int total,
            List<BalanceLogEntry> logs
    ) {
        public Snapshot {
            Objects.requireNonNull(category, "category");
            logs = List.copyOf(Objects.requireNonNull(logs, "logs"));
        }
    }
}
