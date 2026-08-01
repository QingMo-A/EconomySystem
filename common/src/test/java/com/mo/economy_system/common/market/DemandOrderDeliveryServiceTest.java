package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemandOrderDeliveryServiceTest {
    @Test void successfulDeliveryPaysOnceAndTransitionsOnce() {
        Fixture f=new Fixture();assertEquals(DemandOrderDeliveryResult.SUCCESS,f.execute());
        assertState(f,15,117,true,1,0,1);assertEquals(DemandOrderDeliveryResult.ALREADY_DELIVERED,f.execute());assertState(f,15,117,true,1,0,1);
    }
    @Test void multipleStacksAreAggregatedWithLongCount() { Fixture f=new Fixture();f.inventory.available=(long)Integer.MAX_VALUE+1;assertEquals(DemandOrderDeliveryResult.SUCCESS,f.execute()); }
    @Test void invalidOrdersDoNotMutateAnything() {
        Fixture missing=new Fixture();missing.repository.order=null;assertEquals(DemandOrderDeliveryResult.NOT_FOUND,missing.execute());assertState(missing,20,100,false,0,0,0);
        Fixture self=new Fixture();self.supplier=self.repository.order.sellerId();assertEquals(DemandOrderDeliveryResult.SELF_DELIVERY,self.execute());assertState(self,20,100,false,0,0,0);
        Fixture insufficient=new Fixture();insufficient.inventory.available=4;assertEquals(DemandOrderDeliveryResult.INSUFFICIENT_ITEMS,insufficient.execute());assertState(insufficient,20,100,false,0,0,0);
    }
    @Test void partialRemovalAndRemovalExceptionRestoreBeforeFailure() {
        Fixture partial=new Fixture();partial.inventory.partialFailure=true;assertEquals(DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED,partial.execute());assertState(partial,20,100,false,0,0,1);
        Fixture thrown=new Fixture();thrown.inventory.removeThrows=true;assertEquals(DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED,thrown.execute());assertState(thrown,20,100,false,0,0,1);
    }
    @Test void paymentFalseOrExceptionRestoresItems() {
        Fixture rejected=new Fixture();rejected.account.creditFalse=true;assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED,rejected.execute());assertState(rejected,20,100,false,1,0,1);
        Fixture thrown=new Fixture();thrown.account.creditThrows=true;assertEquals(DemandOrderDeliveryResult.PAYMENT_FAILED,thrown.execute());assertState(thrown,20,100,false,1,0,1);
    }
    @Test void recipientBalanceLimitRejectsBeforeInventoryMutation() {
        Fixture f=new Fixture();f.account.canCredit=false;assertEquals(DemandOrderDeliveryResult.RECIPIENT_BALANCE_LIMIT,f.execute());assertState(f,20,100,false,0,0,0);
    }
    @Test void ledgerFailureRevertsPaymentAndItems() {
        Fixture f=new Fixture();f.repository.transition=DemandDeliveryTransitionResult.PERSIST_FAILED;
        assertEquals(DemandOrderDeliveryResult.LEDGER_UPDATE_FAILED,f.execute());assertState(f,20,100,false,1,1,1);
    }
    @Test void failedPaymentRevertStillRestoresItems() {
        Fixture f=new Fixture();f.repository.transition=DemandDeliveryTransitionResult.PERSIST_FAILED;f.account.debitFalse=true;
        assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED,f.execute());assertEquals(20,f.inventory.count);assertEquals(117,f.account.balance);assertEquals(1,f.inventory.rollbackCalls);assertEquals(1,f.account.debitCalls);
    }
    @Test void failedItemRestoreStillAttemptsPaymentRevert() {
        Fixture f=new Fixture();f.repository.transition=DemandDeliveryTransitionResult.PERSIST_FAILED;f.inventory.rollbackThrows=true;
        assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED,f.execute());assertEquals(100,f.account.balance);assertEquals(1,f.account.debitCalls);assertEquals(1,f.inventory.rollbackCalls);
    }
    @Test void rollbackFailureIsReported() {
        Fixture f=new Fixture();f.repository.transition=DemandDeliveryTransitionResult.PERSIST_FAILED;f.account.debitThrows=true;
        assertEquals(DemandOrderDeliveryResult.ROLLBACK_FAILED,f.execute());assertTrue(f.reports.stream().anyMatch(s->s.contains("ROLLBACK_FAILED")));
    }
    private static void assertState(Fixture f,int items,int balance,boolean delivered,int credits,int debits,int removals){assertEquals(items,f.inventory.count);assertEquals(balance,f.account.balance);assertEquals(delivered,f.repository.order!=null&&f.repository.order.delivered());assertEquals(credits,f.account.creditCalls);assertEquals(debits,f.account.debitCalls);assertEquals(removals,f.inventory.removeCalls);}
    private static final class Fixture {
        UUID supplier=UUID.randomUUID();FakeInventory inventory=new FakeInventory();FakeAccount account=new FakeAccount();FakeRepository repository=new FakeRepository();List<String> reports=new ArrayList<>();
        DemandOrderDeliveryResult execute(){return DemandOrderDeliveryService.execute(repository.id(),new DemandOrderDeliveryService.Context(supplier,inventory,account,repository,(id,stage,result,cause,c)->reports.add(stage+":"+result)));}
    }
    private static final class FakeInventory implements DemandOrderDeliveryService.Inventory {
        int count=20,removeCalls,rollbackCalls;long available=-1;boolean partialFailure,removeThrows,rollbackThrows;
        public Object restoreTemplate(MarketOrder order){return new Object();}public long countMatching(Object t){return available<0?count:available;}
        public DemandOrderDeliveryService.RemovalResult removeMatching(Object t,int q){removeCalls++;int before=count;if(removeThrows){count=before;throw new IllegalStateException();}if(partialFailure){count-=2;count=before;return DemandOrderDeliveryService.RemovalResult.failure(true);}count-=q;return DemandOrderDeliveryService.RemovalResult.success(()->{rollbackCalls++;if(rollbackThrows)throw new IllegalStateException();count=before;return true;});}
    }
    private static final class FakeAccount implements DemandOrderDeliveryService.Account {
        int balance=100,creditCalls,debitCalls;boolean canCredit=true,creditFalse,creditThrows,debitFalse,debitThrows;
        public boolean canCreditExact(int a){return canCredit&&(long)balance+a<=Integer.MAX_VALUE;}
        public BalanceMutationResult creditExact(int a){creditCalls++;if(creditThrows)throw new IllegalStateException();if(creditFalse)return BalanceMutationResult.PERSIST_FAILED;balance+=a;return BalanceMutationResult.SUCCESS;}
        public BalanceMutationResult debitExact(int a){debitCalls++;if(debitThrows)throw new IllegalStateException();if(debitFalse)return BalanceMutationResult.PERSIST_FAILED;balance-=a;return BalanceMutationResult.SUCCESS;}
    }
    private static final class FakeRepository implements DemandOrderDeliveryService.Repository {
        MarketOrder order=new MarketOrder(MarketOrderType.DEMAND,UUID.randomUUID(),MarketOrderCodecTest.item(),5,17,"buyer",UUID.randomUUID(),1,2,false);DemandDeliveryTransitionResult transition=DemandDeliveryTransitionResult.UPDATED;
        UUID id(){return order==null?UUID.randomUUID():order.tradeId();}public MarketOrder find(UUID id){return order;}public DemandDeliveryTransitionResult markDelivered(UUID id){if(transition==DemandDeliveryTransitionResult.UPDATED)order=new MarketOrder(order.type(),order.tradeId(),order.item(),order.quantity(),order.totalPrice(),order.sellerName(),order.sellerId(),order.listingTime(),order.expirationTime(),true);return transition;}
    }
}
