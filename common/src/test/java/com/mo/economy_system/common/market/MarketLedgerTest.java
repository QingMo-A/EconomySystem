package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MarketLedgerTest {
  @Test
  void expectedOrderTransitionIsAtomic() {
    AtomicInteger dirty = new AtomicInteger();
    MarketLedger l = new MarketLedger(dirty::incrementAndGet);
    MarketOrder o = order();
    assertTrue(l.add(o));
    long revision = l.revision();
    DemandDeliveryTransition t = l.markDemandDeliveredIfUnchanged(o.tradeId(), o);
    assertEquals(DemandDeliveryTransitionStatus.UPDATED, t.status());
    assertTrue(t.updatedOrder().orElseThrow().delivered());
    assertEquals(revision + 1, l.revision());
    assertEquals(2, dirty.get());
    assertEquals(
        DemandDeliveryTransitionStatus.ALREADY_DELIVERED,
        l.markDemandDeliveredIfUnchanged(o.tradeId(), o).status());
  }

  @Test
  void staleExpectedDoesNotDirty() {
    AtomicInteger dirty = new AtomicInteger();
    MarketLedger l = new MarketLedger(dirty::incrementAndGet);
    MarketOrder o = order();
    assertTrue(l.add(o));
    MarketOrder stale =
        new MarketOrder(
            o.type(),
            o.tradeId(),
            o.item(),
            o.quantity() + 1,
            o.totalPrice(),
            o.sellerName(),
            o.sellerId(),
            o.listingTime(),
            o.expirationTime(),
            false);
    assertEquals(
        DemandDeliveryTransitionStatus.ORDER_CHANGED,
        l.markDemandDeliveredIfUnchanged(o.tradeId(), stale).status());
    assertEquals(1, dirty.get());
    assertFalse(l.find(o.tradeId()).delivered());
  }

  private static MarketOrder order() {
    return new MarketOrder(
        MarketOrderType.DEMAND,
        UUID.randomUUID(),
        MarketOrderCodecTest.item(),
        2,
        10,
        "buyer",
        UUID.randomUUID(),
        1,
        2,
        false);
  }
}
