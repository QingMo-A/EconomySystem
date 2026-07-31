package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.AccountBalance;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loader-neutral client cache for the latest balance response.
 *
 * <p>The balance and account list are published as one immutable snapshot so
 * render and network threads cannot observe a partially updated response.</p>
 */
public final class ClientBalanceState {
    private static final AtomicReference<Snapshot> CURRENT =
            new AtomicReference<>(new Snapshot(0, List.of()));

    private ClientBalanceState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(int balance, List<AccountBalance> accounts) {
        CURRENT.set(new Snapshot(balance, accounts));
    }

    public static void update(com.mo.economy_system.common.network.BalanceResponseMessage message) {
        Objects.requireNonNull(message, "message");
        update(message.balance(), message.accounts());
    }

    /** One atomically published, immutable view of the client's balance data. */
    public record Snapshot(int balance, List<AccountBalance> accounts) {
        public Snapshot {
            accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
        }
    }
}
