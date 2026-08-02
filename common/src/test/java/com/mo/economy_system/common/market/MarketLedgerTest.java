package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.CompoundTag;
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

  @Test void revisionTracksCommittedMutationsAndPersistenceLoadDoesNotIncrement() {
    MarketLedger ledger = new MarketLedger(() -> {}); MarketOrder demand = order(MarketOrderType.DEMAND, false);
    assertTrue(ledger.add(demand)); assertFalse(ledger.add(demand)); assertEquals(1, ledger.revision());
    assertEquals(DemandDeliveryTransitionStatus.UPDATED, ledger.markDemandDeliveredIfUnchanged(demand.tradeId(), demand).status());
    assertEquals(2, ledger.revision()); assertTrue(ledger.remove(demand.tradeId())); assertEquals(3, ledger.revision());
    ledger.loadFromPersistence(List.of(demand), 17); assertEquals(17, ledger.revision());
  }

  @Test void dirtyFailureAndRevisionExhaustionLeaveStateUntouched() {
    MarketOrder sale = order(MarketOrderType.SALES, false); MarketLedger failing = new MarketLedger(() -> { throw new IllegalStateException(); });
    assertThrows(IllegalStateException.class, () -> failing.add(sale)); assertTrue(failing.orders().isEmpty()); assertEquals(0, failing.revision());
    MarketLedger exhausted = new MarketLedger(() -> {}); exhausted.loadFromPersistence(List.of(), Long.MAX_VALUE);
    assertThrows(IllegalStateException.class, () -> exhausted.add(sale)); assertTrue(exhausted.orders().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> exhausted.loadFromPersistence(List.of(), -1));
  }

  @Test void idsAreUniqueListsImmutableAndChangesMarkDirty() {
    AtomicInteger dirty = new AtomicInteger(); MarketLedger ledger = new MarketLedger(dirty::incrementAndGet); MarketOrder sale = order(MarketOrderType.SALES, false);
    assertTrue(ledger.add(sale)); assertFalse(ledger.add(sale)); assertThrows(UnsupportedOperationException.class, () -> ledger.orders().clear());
    assertTrue(ledger.remove(sale.tradeId())); assertEquals(2, dirty.get());
  }

  @Test void persistenceRejectsDuplicateIds() {
    MarketLedger ledger = new MarketLedger(() -> {}); MarketOrder sale = order(MarketOrderType.SALES, false);
    assertThrows(IllegalArgumentException.class, () -> ledger.loadFromPersistence(List.of(sale, sale), 0)); assertTrue(ledger.orders().isEmpty());
  }

  @Test void ordinaryRemoveRestoresOriginalIndexWhenDirtyFails() {
    AtomicInteger calls = new AtomicInteger(); MarketLedger ledger = new MarketLedger(() -> { if (calls.incrementAndGet() == 1) throw new IllegalStateException(); });
    MarketOrder first = order(MarketOrderType.SALES, false), second = order(MarketOrderType.DEMAND, false); ledger.loadFromPersistence(List.of(first, second), 0);
    assertThrows(IllegalStateException.class, () -> ledger.remove(first.tradeId())); assertEquals(List.of(first, second), ledger.orders()); assertFalse(ledger.remove(UUID.randomUUID()));
  }

  @Test void undeliveredDemandRemovalRestoresAtOriginalIndexAndRejectsInvalidOrders() {
    MarketLedger ledger = new MarketLedger(() -> {}); MarketOrder sale = order(MarketOrderType.SALES, false), demand = order(MarketOrderType.DEMAND, false), delivered = order(MarketOrderType.DEMAND, true);
    ledger.loadFromPersistence(List.of(sale, demand, delivered), 0);
    assertEquals(DemandOrderRemovalStatus.WRONG_ORDER_TYPE, ledger.removeUndeliveredDemand(sale.tradeId()).status());
    assertEquals(DemandOrderRemovalStatus.ALREADY_DELIVERED, ledger.removeUndeliveredDemand(delivered.tradeId()).status());
    assertEquals(DemandOrderRemovalStatus.NOT_FOUND, ledger.removeUndeliveredDemand(UUID.randomUUID()).status());
    DemandOrderRemovalResult removed = ledger.removeUndeliveredDemand(demand.tradeId()); assertEquals(List.of(sale, delivered), ledger.orders());
    assertEquals(MarketOrderRestoreResult.RESTORED, removed.removal().restore().restore()); assertEquals(List.of(sale, demand, delivered), ledger.orders());
    assertEquals(MarketOrderRestoreResult.DUPLICATE_ID, removed.removal().restore().restore());
  }

  @Test void undeliveredDemandRemovalAndRestoreDirtyFailuresAreAtomic() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false); MarketLedger removeFails = new MarketLedger(() -> { throw new IllegalStateException(); }); removeFails.loadFromPersistence(List.of(demand), 0);
    assertEquals(DemandOrderRemovalStatus.PERSIST_FAILED, removeFails.removeUndeliveredDemand(demand.tradeId()).status()); assertEquals(List.of(demand), removeFails.orders());
    AtomicInteger calls = new AtomicInteger(); MarketLedger restoreFails = new MarketLedger(() -> { if (calls.incrementAndGet() > 1) throw new IllegalStateException(); }); restoreFails.loadFromPersistence(List.of(demand), 0);
    DemandOrderRemovalResult removed = restoreFails.removeUndeliveredDemand(demand.tradeId()); assertEquals(MarketOrderRestoreResult.PERSIST_FAILED, removed.removal().restore().restore()); assertTrue(restoreFails.orders().isEmpty());
  }

  @Test void salesRemovalRestoresOriginalIndexAndAdvancesRevision() {
    MarketLedger ledger = new MarketLedger(() -> {}); MarketOrder first = order(MarketOrderType.DEMAND, false), sale = order(MarketOrderType.SALES, false), last = order(MarketOrderType.DEMAND, false); ledger.loadFromPersistence(List.of(first, sale, last), 4);
    assertEquals(SalesOrderRemovalStatus.WRONG_ORDER_TYPE, ledger.removeSalesTransactional(first.tradeId()).status()); assertEquals(SalesOrderRemovalStatus.NOT_FOUND, ledger.removeSalesTransactional(UUID.randomUUID()).status());
    SalesOrderRemovalResult removed = ledger.removeSalesTransactional(sale.tradeId()); assertEquals(5, ledger.revision()); assertEquals(List.of(first, last), ledger.orders());
    assertEquals(MarketOrderRestoreResult.RESTORED, removed.removal().restore().restore()); assertEquals(6, ledger.revision()); assertEquals(List.of(first, sale, last), ledger.orders());
  }

  @Test void salesRemovalAndRestoreDirtyFailuresAreAtomic() {
    MarketOrder sale = order(MarketOrderType.SALES, false); MarketLedger removeFails = new MarketLedger(() -> { throw new IllegalStateException(); }); removeFails.loadFromPersistence(List.of(sale), 3);
    assertEquals(SalesOrderRemovalStatus.PERSIST_FAILED, removeFails.removeSalesTransactional(sale.tradeId()).status()); assertEquals(3, removeFails.revision());
    AtomicInteger calls = new AtomicInteger(); MarketLedger restoreFails = new MarketLedger(() -> { if (calls.incrementAndGet() > 1) throw new IllegalStateException(); }); restoreFails.loadFromPersistence(List.of(sale), 7);
    SalesOrderRemovalResult removed = restoreFails.removeSalesTransactional(sale.tradeId()); assertEquals(MarketOrderRestoreResult.PERSIST_FAILED, removed.removal().restore().restore()); assertEquals(8, restoreFails.revision());
  }

  @Test void deliveredDemandRemovalIsTransactionalAndRejectsInvalidOrders() {
    MarketLedger ledger = new MarketLedger(() -> {}); MarketOrder sale = order(MarketOrderType.SALES, false), pending = order(MarketOrderType.DEMAND, false), delivered = order(MarketOrderType.DEMAND, true); ledger.loadFromPersistence(List.of(sale, pending, delivered), 4);
    assertEquals(DeliveredDemandRemovalStatus.WRONG_ORDER_TYPE, ledger.removeDeliveredDemandTransactional(sale.tradeId()).status()); assertEquals(DeliveredDemandRemovalStatus.NOT_DELIVERED, ledger.removeDeliveredDemandTransactional(pending.tradeId()).status());
    DeliveredDemandRemovalResult removed = ledger.removeDeliveredDemandTransactional(delivered.tradeId()); assertEquals(5, ledger.revision()); assertEquals(MarketOrderRestoreResult.RESTORED, removed.removal().restore().restore()); assertEquals(List.of(sale, pending, delivered), ledger.orders());
  }

  @Test void transitionFailuresPreserveOrderRevisionAndDirtyCount() {
    AtomicInteger dirty = new AtomicInteger(); MarketLedger ledger = new MarketLedger(dirty::incrementAndGet); MarketOrder sale = order(MarketOrderType.SALES, false), demand = order(MarketOrderType.DEMAND, false); ledger.loadFromPersistence(List.of(sale, demand), 9);
    assertEquals(DemandDeliveryTransitionStatus.NOT_FOUND, ledger.markDemandDeliveredIfUnchanged(UUID.randomUUID(), demand).status());
    assertEquals(DemandDeliveryTransitionStatus.WRONG_ORDER_TYPE, ledger.markDemandDeliveredIfUnchanged(sale.tradeId(), sale).status());
    MarketOrder stale = new MarketOrder(demand.type(), demand.tradeId(), demand.item(), 99, demand.totalPrice(), demand.sellerName(), demand.sellerId(), demand.listingTime(), demand.expirationTime(), false);
    assertEquals(DemandDeliveryTransitionStatus.ORDER_CHANGED, ledger.markDemandDeliveredIfUnchanged(demand.tradeId(), stale).status()); assertEquals(9, ledger.revision()); assertEquals(0, dirty.get());
  }

  @Test void deliveredTransitionDirtyFailureRestoresOrderAndRevision() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false); MarketLedger ledger = new MarketLedger(() -> { throw new IllegalStateException(); }); ledger.loadFromPersistence(List.of(demand), 6);
    assertEquals(DemandDeliveryTransitionStatus.PERSIST_FAILED, ledger.markDemandDeliveredIfUnchanged(demand.tradeId(), demand).status()); assertEquals(List.of(demand), ledger.orders()); assertEquals(6, ledger.revision());
  }

  @Test void deliveredTransitionRejectsNullArgumentsAndExhaustedRevision() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false); MarketLedger ledger = new MarketLedger(() -> {}); ledger.loadFromPersistence(List.of(demand), Long.MAX_VALUE);
    assertThrows(NullPointerException.class, () -> ledger.markDemandDeliveredIfUnchanged(null, demand)); assertThrows(NullPointerException.class, () -> ledger.markDemandDeliveredIfUnchanged(demand.tradeId(), null));
    assertEquals(DemandDeliveryTransitionStatus.PERSIST_FAILED, ledger.markDemandDeliveredIfUnchanged(demand.tradeId(), demand).status()); assertFalse(ledger.find(demand.tradeId()).delivered());
  }

  @Test
  void expectedDemandRemovalCommitsOnceAndRestoresAtOriginalIndex() {
    MarketLedger ledger = new MarketLedger(() -> {});
    MarketOrder first = order(MarketOrderType.SALES, false);
    MarketOrder demand = order(MarketOrderType.DEMAND, false);
    MarketOrder last = order(MarketOrderType.DEMAND, false);
    ledger.loadFromPersistence(List.of(first, demand, last), 12);

    DemandOrderRemovalResult result =
        ledger.removeUndeliveredDemandIfUnchanged(demand.tradeId(), demand);

    assertEquals(DemandOrderRemovalStatus.REMOVED, result.status());
    assertEquals(List.of(first, last), ledger.orders());
    assertEquals(13, ledger.revision());
    assertEquals(MarketOrderRestoreResult.RESTORED, result.removal().restore().restore());
    assertEquals(List.of(first, demand, last), ledger.orders());
    assertEquals(14, ledger.revision());
    assertEquals(MarketOrderRestoreResult.DUPLICATE_ID, result.removal().restore().restore());
  }

  @Test
  void expectedDemandRemovalRejectsEveryStaleFieldWithoutMutation() {
    AtomicInteger dirty = new AtomicInteger();
    MarketLedger ledger = new MarketLedger(dirty::incrementAndGet);
    MarketOrder current = order(MarketOrderType.DEMAND, false);
    ledger.loadFromPersistence(List.of(current), 7);
    List<MarketOrder> staleOrders =
        List.of(
            new MarketOrder(current.type(), current.tradeId(), differentItem(), current.quantity(),
                current.totalPrice(), current.sellerName(), current.sellerId(),
                current.listingTime(), current.expirationTime(), false),
            new MarketOrder(current.type(), current.tradeId(), current.item(),
                current.quantity() + 1, current.totalPrice(), current.sellerName(),
                current.sellerId(), current.listingTime(), current.expirationTime(), false),
            new MarketOrder(current.type(), current.tradeId(), current.item(), current.quantity(),
                current.totalPrice() + 1, current.sellerName(), current.sellerId(),
                current.listingTime(), current.expirationTime(), false),
            new MarketOrder(current.type(), current.tradeId(), current.item(), current.quantity(),
                current.totalPrice(), current.sellerName(), UUID.randomUUID(),
                current.listingTime(), current.expirationTime(), false),
            new MarketOrder(current.type(), current.tradeId(), current.item(), current.quantity(),
                current.totalPrice(), current.sellerName(), current.sellerId(),
                current.listingTime() + 1, current.expirationTime() + 1, false));

    for (MarketOrder stale : staleOrders) {
      assertEquals(
          DemandOrderRemovalStatus.ORDER_CHANGED,
          ledger.removeUndeliveredDemandIfUnchanged(current.tradeId(), stale).status());
    }
    assertEquals(List.of(current), ledger.orders());
    assertEquals(7, ledger.revision());
    assertEquals(0, dirty.get());
  }

  @Test
  void expectedDemandRemovalDirtyFailureRestoresStateAndRevision() {
    MarketOrder demand = order(MarketOrderType.DEMAND, false);
    MarketLedger ledger = new MarketLedger(() -> { throw new IllegalStateException("dirty"); });
    ledger.loadFromPersistence(List.of(demand), 5);

    DemandOrderRemovalResult result =
        ledger.removeUndeliveredDemandIfUnchanged(demand.tradeId(), demand);

    assertEquals(DemandOrderRemovalStatus.PERSIST_FAILED, result.status());
    assertEquals(List.of(demand), ledger.orders());
    assertEquals(5, ledger.revision());
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

  private static ItemStackSnapshot differentItem() {
    return ItemStackSnapshot.create(
            "minecraft:dirt",
            1,
            Optional.empty(),
            List.of(),
            Map.of(),
            Map.of(),
            true,
            true,
            0,
            0,
            false,
            true,
            OptionalInt.empty(),
            true,
            OptionalInt.empty(),
            new CompoundTag())
        .orElseThrow();
  }

  private static MarketOrder order(MarketOrderType type, boolean delivered) {
    MarketOrder base = order();
    return new MarketOrder(type, base.tradeId(), base.item(), base.quantity(), base.totalPrice(),
        base.sellerName(), base.sellerId(), base.listingTime(), base.expirationTime(), delivered);
  }
}
