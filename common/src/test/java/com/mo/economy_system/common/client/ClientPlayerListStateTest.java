package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.PlayerSummary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientPlayerListStateTest {
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
}
