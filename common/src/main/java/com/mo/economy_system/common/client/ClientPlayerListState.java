package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loader-neutral client cache for the latest known economy-account players.
 *
 * <p>The complete list is atomically replaced with an immutable defensive
 * copy, so readers never observe a response while it is being updated.</p>
 */
public final class ClientPlayerListState {
    private static final AtomicReference<Snapshot> CURRENT =
            new AtomicReference<>(new Snapshot(List.of()));

    private ClientPlayerListState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(List<PlayerSummary> players) {
        CURRENT.set(new Snapshot(players));
    }

    public static void update(ServerPlayerListResponseMessage message) {
        Objects.requireNonNull(message, "message");
        update(message.players());
    }

    /** One atomically published, immutable view of the known player list. */
    public record Snapshot(List<PlayerSummary> players) {
        public Snapshot {
            players = List.copyOf(Objects.requireNonNull(players, "players"));
        }
    }
}
