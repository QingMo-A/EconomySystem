package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.AccountBalance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientBalanceStateTest {
    @Test
    void publishesAnImmutableDefensiveSnapshot() {
        List<AccountBalance> source = new ArrayList<>();
        source.add(new AccountBalance("Alice", 12));

        ClientBalanceState.update(34, source);
        source.add(new AccountBalance("Bob", 56));

        ClientBalanceState.Snapshot snapshot = ClientBalanceState.snapshot();
        assertEquals(34, snapshot.balance());
        assertEquals(List.of(new AccountBalance("Alice", 12)), snapshot.accounts());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.accounts().add(new AccountBalance("Carol", 78))
        );
    }
}
