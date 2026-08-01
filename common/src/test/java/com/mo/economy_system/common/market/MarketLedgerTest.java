package com.mo.economy_system.common.market;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MarketLedgerTest {
    @Test void idsAreUniqueListsImmutableAndChangesMarkDirty() {
        AtomicInteger dirty = new AtomicInteger(); MarketLedger ledger = new MarketLedger(dirty::incrementAndGet);
        MarketOrder order = new MarketOrder(MarketOrderType.SALES, UUID.randomUUID(), MarketOrderCodecTest.item(), 1, 1,
                "a", UUID.randomUUID(), 1, 2, false);
        assertTrue(ledger.add(order)); assertFalse(ledger.add(order)); assertEquals(1, dirty.get());
        assertThrows(UnsupportedOperationException.class, () -> ledger.orders().clear());
        assertTrue(ledger.remove(order.tradeId())); assertEquals(2, dirty.get());
    }

    @Test void restoreRejectsDuplicateIdsWithoutChangingExistingState() {
        MarketLedger ledger = new MarketLedger(() -> {}); UUID id = UUID.randomUUID();
        MarketOrder order = new MarketOrder(MarketOrderType.SALES, id, MarketOrderCodecTest.item(), 1, 1, "a", UUID.randomUUID(), 1, 2, false);
        assertThrows(IllegalArgumentException.class, () -> ledger.restore(List.of(order, order)));
        assertTrue(ledger.orders().isEmpty());
    }
}
