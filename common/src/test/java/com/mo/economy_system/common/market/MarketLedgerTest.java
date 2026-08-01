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
        ledger.restore(List.of(first,demand,last));
        assertEquals(DemandDeliveryTransitionResult.UPDATED,ledger.markDemandDelivered(demand.tradeId()));
        List<MarketOrder> updated=ledger.orders();assertEquals(List.of(first.tradeId(),demand.tradeId(),last.tradeId()),updated.stream().map(MarketOrder::tradeId).toList());
        MarketOrder delivered=updated.get(1);assertTrue(delivered.delivered());
        assertEquals(demand.type(),delivered.type());assertEquals(demand.item(),delivered.item());assertEquals(demand.quantity(),delivered.quantity());
        assertEquals(demand.totalPrice(),delivered.totalPrice());assertEquals(demand.sellerName(),delivered.sellerName());
        assertEquals(demand.sellerId(),delivered.sellerId());assertEquals(demand.listingTime(),delivered.listingTime());assertEquals(demand.expirationTime(),delivered.expirationTime());
        assertEquals(1,dirty.get());assertEquals(DemandDeliveryTransitionResult.ALREADY_DELIVERED,ledger.markDemandDelivered(demand.tradeId()));assertEquals(1,dirty.get());
    }

    @Test void demandTransitionRejectsMissingAndSalesOrders() {
        MarketLedger ledger=new MarketLedger(()->{});MarketOrder sale=order(MarketOrderType.SALES,false);ledger.restore(List.of(sale));
        assertEquals(DemandDeliveryTransitionResult.NOT_FOUND,ledger.markDemandDelivered(UUID.randomUUID()));
        assertEquals(DemandDeliveryTransitionResult.WRONG_ORDER_TYPE,ledger.markDemandDelivered(sale.tradeId()));
    }

    @Test void demandTransitionRollsBackWhenDirtyCallbackFails() {
        MarketLedger ledger=new MarketLedger(()->{throw new IllegalStateException("dirty");});MarketOrder demand=order(MarketOrderType.DEMAND,false);ledger.restore(List.of(demand));
        assertEquals(DemandDeliveryTransitionResult.PERSIST_FAILED,ledger.markDemandDelivered(demand.tradeId()));
        assertEquals(demand,ledger.orders().get(0));assertFalse(ledger.orders().get(0).delivered());
    }

    @Test void ordinaryRemoveRestoresOriginalIndexWhenDirtyFails() {
        AtomicInteger calls=new AtomicInteger();MarketLedger ledger=new MarketLedger(()->{if(calls.incrementAndGet()==1)throw new IllegalStateException("dirty");});
        MarketOrder first=order(MarketOrderType.SALES,false),second=order(MarketOrderType.DEMAND,false);ledger.restore(List.of(first,second));
        assertThrows(IllegalStateException.class,()->ledger.remove(first.tradeId()));assertEquals(List.of(first,second),ledger.orders());assertFalse(ledger.remove(UUID.randomUUID()));
    }

    @Test void transactionalDemandRemovalRestoresAtOriginalIndexAndRejectsInvalidOrders() {
        MarketLedger ledger=new MarketLedger(()->{});MarketOrder first=order(MarketOrderType.SALES,false),demand=order(MarketOrderType.DEMAND,false),delivered=order(MarketOrderType.DEMAND,true);ledger.restore(List.of(first,demand,delivered));
        assertEquals(DemandOrderRemovalStatus.WRONG_ORDER_TYPE,ledger.removeUndeliveredDemand(first.tradeId()).status());
        assertEquals(DemandOrderRemovalStatus.ALREADY_DELIVERED,ledger.removeUndeliveredDemand(delivered.tradeId()).status());
        assertEquals(DemandOrderRemovalStatus.NOT_FOUND,ledger.removeUndeliveredDemand(UUID.randomUUID()).status());
        DemandOrderRemovalResult removed=ledger.removeUndeliveredDemand(demand.tradeId());assertEquals(DemandOrderRemovalStatus.REMOVED,removed.status());assertEquals(List.of(first,delivered),ledger.orders());
        assertEquals(MarketOrderRestoreResult.RESTORED,removed.removal().restore().restore());assertEquals(List.of(first,demand,delivered),ledger.orders());
        assertEquals(MarketOrderRestoreResult.DUPLICATE_ID,removed.removal().restore().restore());
    }

    @Test void transactionalRemovalAndRestoreFailClosedOnDirtyFailure() {
        MarketOrder demand=order(MarketOrderType.DEMAND,false);MarketLedger removeFails=new MarketLedger(()->{throw new IllegalStateException();});removeFails.restore(List.of(demand));
        assertEquals(DemandOrderRemovalStatus.PERSIST_FAILED,removeFails.removeUndeliveredDemand(demand.tradeId()).status());assertEquals(List.of(demand),removeFails.orders());
        AtomicInteger calls=new AtomicInteger();MarketLedger restoreFails=new MarketLedger(()->{if(calls.incrementAndGet()>1)throw new IllegalStateException();});restoreFails.restore(List.of(demand));
        DemandOrderRemovalResult removed=restoreFails.removeUndeliveredDemand(demand.tradeId());assertEquals(MarketOrderRestoreResult.PERSIST_FAILED,removed.removal().restore().restore());assertTrue(restoreFails.orders().isEmpty());
    }

    private static MarketOrder order(MarketOrderType type,boolean delivered){return new MarketOrder(type,UUID.randomUUID(),MarketOrderCodecTest.item(),3,17,"seller",UUID.randomUUID(),10,99,delivered);}
}
