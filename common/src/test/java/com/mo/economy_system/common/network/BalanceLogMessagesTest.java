package com.mo.economy_system.common.network;

import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BalanceLogMessagesTest {
    @Test
    void requestNormalizesTheSameBoundsAsTheNeoForgePacket() {
        BalanceLogRequestMessage defaults = new BalanceLogRequestMessage(null, -4, 0);
        assertEquals(BalanceLogRequestMessage.ALL_CATEGORIES, defaults.category());
        assertEquals(0, defaults.offset());
        assertEquals(1, defaults.limit());

        BalanceLogRequestMessage capped = new BalanceLogRequestMessage("  ", 12, 500);
        assertEquals(BalanceLogRequestMessage.ALL_CATEGORIES, capped.category());
        assertEquals(12, capped.offset());
        assertEquals(BalanceLogRequestMessage.MAX_LIMIT, capped.limit());

        assertEquals(
                new BalanceLogRequestMessage("market", 7, 25),
                new BalanceLogRequestMessage("market", 7, 25)
        );
    }

    @Test
    void responseDefensivelyCopiesEntriesAndPreservesPaginationFields() {
        BalanceLogEntry first = new BalanceLogEntry(1L, "market", "sale", 8, 2, 10);
        BalanceLogEntry second = new BalanceLogEntry(2L, "transfer", "gift", -3, 10, 7);
        List<BalanceLogEntry> source = new ArrayList<>();
        source.add(first);

        BalanceLogResponseMessage response = new BalanceLogResponseMessage(
                "market",
                20,
                10,
                45,
                source
        );
        source.add(second);

        assertEquals("market", response.category());
        assertEquals(20, response.offset());
        assertEquals(10, response.limit());
        assertEquals(45, response.total());
        assertEquals(List.of(first), response.logs());
        assertThrows(UnsupportedOperationException.class, () -> response.logs().add(second));
    }

    @Test
    void responseRejectsNullReferences() {
        BalanceLogEntry entry = new BalanceLogEntry(1L, "system", "set", 1, 0, 1);

        assertThrows(
                NullPointerException.class,
                () -> new BalanceLogResponseMessage(null, 0, 50, 1, List.of(entry))
        );
        assertThrows(
                NullPointerException.class,
                () -> new BalanceLogResponseMessage("system", 0, 50, 1, null)
        );

        List<BalanceLogEntry> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(
                NullPointerException.class,
                () -> new BalanceLogResponseMessage("system", 0, 50, 1, withNull)
        );
    }
}
