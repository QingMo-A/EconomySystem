package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.PlayerSummary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientPlayerListStateTest {
    @Test
    void revisionAdvancesForEveryResponseIncludingEmptyLists() {
        long initial = ClientPlayerListState.snapshot().revision();
        ClientPlayerListState.update(List.of());
        assertEquals(initial + 1, ClientPlayerListState.snapshot().revision());
        ClientPlayerListState.update(List.of());
        assertEquals(initial + 2, ClientPlayerListState.snapshot().revision());
    }

    @Test
    void publishesAnImmutableDefensiveSnapshot() {
        PlayerSummary alice = new PlayerSummary(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Alice"
        );
        PlayerSummary bob = new PlayerSummary(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Bob"
        );
        List<PlayerSummary> source = new ArrayList<>();
        source.add(alice);

        ClientPlayerListState.update(source);
        source.add(bob);

        ClientPlayerListState.Snapshot snapshot = ClientPlayerListState.snapshot();
        assertEquals(List.of(alice), snapshot.players());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.players().add(bob)
        );
    }

    @Test
    void concurrentReadersObserveOneAtomicRevisionAndListPair() throws Exception {
        PlayerSummary player = new PlayerSummary(UUID.randomUUID(), "Alice");
        long before = ClientPlayerListState.snapshot().revision();
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean mismatch = new AtomicBoolean();
        Thread reader = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < 10_000; i++) {
                    ClientPlayerListState.Snapshot snapshot = ClientPlayerListState.snapshot();
                    if (snapshot.revision() > before && !snapshot.players().equals(List.of(player))) {
                        mismatch.set(true);
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        reader.start();
        start.countDown();
        ClientPlayerListState.update(List.of(player));
        reader.join();
        assertEquals(false, mismatch.get());
    }
}
