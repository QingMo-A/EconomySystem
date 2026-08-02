package com.mo.economy_system.common.market;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class MarketLedgerTest {
    @Test void revisionTracksOnlyCommittedMutationsAndLoadsWithoutIncrement(){MarketLedger ledger=new MarketLedger(()->{});assertEquals(0,ledger.revision());MarketOrder demand=order(MarketOrderType.DEMAND,false);assertTrue(ledger.add(demand));assertEquals(1,ledger.revision());assertFalse(ledger.add(demand));assertEquals(1,ledger.revision());assertEquals(DemandDeliveryTransitionResult.UPDATED,ledger.markDemandDelivered(demand.tradeId()));assertEquals(2,ledger.revision());assertTrue(ledger.remove(demand.tradeId()));assertEquals(3,ledger.revision());ledger.loadFromPersistence(List.of(demand),17);assertEquals(17,ledger.view().revision());}
    @Test void dirtyFailureAndRevisionExhaustionLeaveStateUntouched(){MarketOrder order=order(MarketOrderType.SALES,false);MarketLedger failing=new MarketLedger(()->{throw new IllegalStateException();});assertThrows(IllegalStateException.class,()->failing.add(order));assertEquals(0,failing.revision());assertTrue(failing.orders().isEmpty());MarketLedger exhausted=new MarketLedger(()->{});exhausted.loadFromPersistence(List.of(),Long.MAX_VALUE);assertThrows(IllegalStateException.class,()->exhausted.add(order));assertEquals(Long.MAX_VALUE,exhausted.revision());assertTrue(exhausted.orders().isEmpty());assertThrows(IllegalArgumentException.class,()->exhausted.loadFromPersistence(List.of(),-1));}
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
        assertThrows(IllegalArgumentException.class, () -> ledger.loadFromPersistence(List.of(order, order), 0));
        assertTrue(ledger.orders().isEmpty());
    }

    @Test void dirtyFailureRemovesOnlyTheNewOrder() {
        MarketLedger ledger = new MarketLedger(() -> { throw new IllegalStateException("dirty"); });
        MarketOrder order = new MarketOrder(MarketOrderType.SALES, UUID.randomUUID(), MarketOrderCodecTest.item(), 1, 1,
                "a", UUID.randomUUID(), 1, 2, false);
        assertThrows(IllegalStateException.class, () -> ledger.add(order));
        assertTrue(ledger.orders().isEmpty());
    }

    @Test void marksDemandDeliveredOnceInPlaceAndPreservesEveryOtherField() {
        AtomicInteger dirty=new AtomicInteger();MarketLedger ledger=new MarketLedger(dirty::incrementAndGet);
        MarketOrder first=order(MarketOrderType.SALES,false),demand=order(MarketOrderType.DEMAND,false),last=order(MarketOrderType.SALES,false);
        ledger.loadFromPersistence(List.of(first,demand,last), 0);
        assertEquals(DemandDeliveryTransitionResult.UPDATED,ledger.markDemandDelivered(demand.tradeId()));
        List<MarketOrder> updated=ledger.orders();assertEquals(List.of(first.tradeId(),demand.tradeId(),last.tradeId()),updated.stream().map(MarketOrder::tradeId).toList());
        MarketOrder delivered=updated.get(1);assertTrue(delivered.delivered());
        assertEquals(demand.type(),delivered.type());assertEquals(demand.item(),delivered.item());assertEquals(demand.quantity(),delivered.quantity());
        assertEquals(demand.totalPrice(),delivered.totalPrice());assertEquals(demand.sellerName(),delivered.sellerName());
        assertEquals(demand.sellerId(),delivered.sellerId());assertEquals(demand.listingTime(),delivered.listingTime());assertEquals(demand.expirationTime(),delivered.expirationTime());
        assertEquals(1,dirty.get());assertEquals(DemandDeliveryTransitionResult.ALREADY_DELIVERED,ledger.markDemandDelivered(demand.tradeId()));assertEquals(1,dirty.get());
    }

    @Test void demandTransitionRejectsMissingAndSalesOrders() {
        MarketLedger ledger=new MarketLedger(()->{});MarketOrder sale=order(MarketOrderType.SALES,false);ledger.loadFromPersistence(List.of(sale),0);
        assertEquals(DemandDeliveryTransitionResult.NOT_FOUND,ledger.markDemandDelivered(UUID.randomUUID()));
        assertEquals(DemandDeliveryTransitionResult.WRONG_ORDER_TYPE,ledger.markDemandDelivered(sale.tradeId()));
    }

    @Test void demandTransitionRollsBackWhenDirtyCallbackFails() {
        MarketLedger ledger=new MarketLedger(()->{throw new IllegalStateException("dirty");});MarketOrder demand=order(MarketOrderType.DEMAND,false);ledger.loadFromPersistence(List.of(demand),0);
        assertEquals(DemandDeliveryTransitionResult.PERSIST_FAILED,ledger.markDemandDelivered(demand.tradeId()));
        assertEquals(demand,ledger.orders().get(0));assertFalse(ledger.orders().get(0).delivered());
    }

    @Test void ordinaryRemoveRestoresOriginalIndexWhenDirtyFails() {
        AtomicInteger calls=new AtomicInteger();MarketLedger ledger=new MarketLedger(()->{if(calls.incrementAndGet()==1)throw new IllegalStateException("dirty");});
        MarketOrder first=order(MarketOrderType.SALES,false),second=order(MarketOrderType.DEMAND,false);ledger.loadFromPersistence(List.of(first,second),0);
        assertThrows(IllegalStateException.class,()->ledger.remove(first.tradeId()));assertEquals(List.of(first,second),ledger.orders());assertFalse(ledger.remove(UUID.randomUUID()));
    }

    @Test void transactionalDemandRemovalRestoresAtOriginalIndexAndRejectsInvalidOrders() {
        MarketLedger ledger=new MarketLedger(()->{});MarketOrder first=order(MarketOrderType.SALES,false),demand=order(MarketOrderType.DEMAND,false),delivered=order(MarketOrderType.DEMAND,true);ledger.loadFromPersistence(List.of(first,demand,delivered),0);
        assertEquals(DemandOrderRemovalStatus.WRONG_ORDER_TYPE,ledger.removeUndeliveredDemand(first.tradeId()).status());
        assertEquals(DemandOrderRemovalStatus.ALREADY_DELIVERED,ledger.removeUndeliveredDemand(delivered.tradeId()).status());
        assertEquals(DemandOrderRemovalStatus.NOT_FOUND,ledger.removeUndeliveredDemand(UUID.randomUUID()).status());
        DemandOrderRemovalResult removed=ledger.removeUndeliveredDemand(demand.tradeId());assertEquals(DemandOrderRemovalStatus.REMOVED,removed.status());assertEquals(List.of(first,delivered),ledger.orders());
        assertEquals(MarketOrderRestoreResult.RESTORED,removed.removal().restore().restore());assertEquals(List.of(first,demand,delivered),ledger.orders());
        assertEquals(MarketOrderRestoreResult.DUPLICATE_ID,removed.removal().restore().restore());
    }

    @Test void transactionalRemovalAndRestoreFailClosedOnDirtyFailure() {
        MarketOrder demand=order(MarketOrderType.DEMAND,false);MarketLedger removeFails=new MarketLedger(()->{throw new IllegalStateException();});removeFails.loadFromPersistence(List.of(demand),0);
        assertEquals(DemandOrderRemovalStatus.PERSIST_FAILED,removeFails.removeUndeliveredDemand(demand.tradeId()).status());assertEquals(List.of(demand),removeFails.orders());
        AtomicInteger calls=new AtomicInteger();MarketLedger restoreFails=new MarketLedger(()->{if(calls.incrementAndGet()>1)throw new IllegalStateException();});restoreFails.loadFromPersistence(List.of(demand),0);
        DemandOrderRemovalResult removed=restoreFails.removeUndeliveredDemand(demand.tradeId());assertEquals(MarketOrderRestoreResult.PERSIST_FAILED,removed.removal().restore().restore());assertTrue(restoreFails.orders().isEmpty());
    }

    @Test void transactionalSalesRemovalRestoresOriginalIndexAndAdvancesRevision() {
        MarketLedger ledger=new MarketLedger(()->{});MarketOrder first=order(MarketOrderType.DEMAND,false),sale=order(MarketOrderType.SALES,false),last=order(MarketOrderType.DEMAND,false);
        ledger.loadFromPersistence(List.of(first,sale,last),4);
        assertEquals(SalesOrderRemovalStatus.WRONG_ORDER_TYPE,ledger.removeSalesTransactional(first.tradeId()).status());
        assertEquals(SalesOrderRemovalStatus.NOT_FOUND,ledger.removeSalesTransactional(UUID.randomUUID()).status());
        SalesOrderRemovalResult removed=ledger.removeSalesTransactional(sale.tradeId());assertEquals(SalesOrderRemovalStatus.REMOVED,removed.status());assertEquals(5,ledger.revision());assertEquals(List.of(first,last),ledger.orders());
        assertEquals(MarketOrderRestoreResult.RESTORED,removed.removal().restore().restore());assertEquals(6,ledger.revision());assertEquals(List.of(first,sale,last),ledger.orders());
        assertEquals(MarketOrderRestoreResult.DUPLICATE_ID,removed.removal().restore().restore());
    }

    @Test void salesRemovalAndRestoreDirtyFailuresAreAtomic() {
        MarketOrder sale=order(MarketOrderType.SALES,false);MarketLedger removeFails=new MarketLedger(()->{throw new IllegalStateException();});removeFails.loadFromPersistence(List.of(sale),3);
        assertEquals(SalesOrderRemovalStatus.PERSIST_FAILED,removeFails.removeSalesTransactional(sale.tradeId()).status());assertEquals(List.of(sale),removeFails.orders());assertEquals(3,removeFails.revision());
        AtomicInteger calls=new AtomicInteger();MarketLedger restoreFails=new MarketLedger(()->{if(calls.incrementAndGet()>1)throw new IllegalStateException();});restoreFails.loadFromPersistence(List.of(sale),7);
        SalesOrderRemovalResult removed=restoreFails.removeSalesTransactional(sale.tradeId());assertEquals(MarketOrderRestoreResult.PERSIST_FAILED,removed.removal().restore().restore());assertTrue(restoreFails.orders().isEmpty());assertEquals(8,restoreFails.revision());
    }

    private static MarketOrder order(MarketOrderType type,boolean delivered){return new MarketOrder(type,UUID.randomUUID(),MarketOrderCodecTest.item(),3,17,"seller",UUID.randomUUID(),10,99,delivered);}
}
