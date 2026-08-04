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
            new AtomicReference<>(new Snapshot(0, List.of()));

    private ClientPlayerListState() {
    }

    public static Snapshot snapshot() {
        return CURRENT.get();
    }

    public static void update(List<PlayerSummary> players) {
        List<PlayerSummary> copy = List.copyOf(Objects.requireNonNull(players, "players"));
        CURRENT.updateAndGet(previous -> new Snapshot(nextRevision(previous.revision()), copy));
    }

    public static void update(ServerPlayerListResponseMessage message) {
        Objects.requireNonNull(message, "message");
        update(message.players());
    }

    /** One atomically published, immutable view of the known player list. */
    private static long nextRevision(long revision) {
        if (revision == Long.MAX_VALUE) {
            throw new IllegalStateException("player-list revision exhausted");
        }
        return revision + 1;
    }

    public record Snapshot(long revision, List<PlayerSummary> players) {
        public Snapshot {
            if (revision < 0) {
                throw new IllegalArgumentException("revision");
            }
            players = List.copyOf(Objects.requireNonNull(players, "players"));
        }
    }
}
