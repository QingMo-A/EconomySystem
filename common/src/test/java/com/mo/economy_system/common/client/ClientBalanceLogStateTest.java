package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientBalanceLogStateTest {
    @Test
    void atomicallyReplacesTheWholeImmutablePageSnapshot() {
        BalanceLogEntry oldEntry = new BalanceLogEntry(1L, "market", "sale", 5, 0, 5);
        List<BalanceLogEntry> oldSource = new ArrayList<>();
        oldSource.add(oldEntry);
        ClientBalanceLogState.update("market", 0, 10, 11, oldSource);
        ClientBalanceLogState.Snapshot oldSnapshot = ClientBalanceLogState.snapshot();

        oldSource.clear();
        BalanceLogEntry newEntry = new BalanceLogEntry(2L, "transfer", "gift", -2, 5, 3);
        ClientBalanceLogState.update(new BalanceLogResponseMessage(
                "transfer",
                10,
                10,
                21,
                List.of(newEntry)
        ));
        ClientBalanceLogState.Snapshot newSnapshot = ClientBalanceLogState.snapshot();

        assertNotSame(oldSnapshot, newSnapshot);
        assertEquals("market", oldSnapshot.category());
        assertEquals(0, oldSnapshot.offset());
        assertEquals(10, oldSnapshot.limit());
        assertEquals(11, oldSnapshot.total());
        assertEquals(List.of(oldEntry), oldSnapshot.logs());

        assertEquals("transfer", newSnapshot.category());
        assertEquals(10, newSnapshot.offset());
        assertEquals(10, newSnapshot.limit());
        assertEquals(21, newSnapshot.total());
        assertEquals(List.of(newEntry), newSnapshot.logs());
        assertThrows(UnsupportedOperationException.class, () -> newSnapshot.logs().clear());
    }

    @Test
    void rejectsNullUpdates() {
        assertThrows(NullPointerException.class, () -> ClientBalanceLogState.update(null));
        assertThrows(
                NullPointerException.class,
                () -> ClientBalanceLogState.update("system", 0, 50, 0, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> ClientBalanceLogState.update(null, 0, 50, 0, List.of())
        );
    }
}
