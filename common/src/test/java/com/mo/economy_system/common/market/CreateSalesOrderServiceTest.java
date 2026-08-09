package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;

import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.platform.item.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateSalesOrderServiceTest {
    @Test void successSeparatesQuantityAndTotalPriceAndMutatesOnce() {
        Fixture f = new Fixture(); f.inventory.count = 20;
        assertEquals(CreateSalesOrderResult.SUCCESS, f.execute(12, 101));
        assertEquals(8, f.inventory.count); assertEquals(989, f.account.balance); assertEquals(1, f.account.debits);
        assertEquals(1, f.repository.orders.size()); MarketOrder order = f.repository.orders.get(0);
        assertEquals(1, order.item().count()); assertEquals(12, order.quantity()); assertEquals(101, order.totalPrice());
    }

    @Test void allPreconditionsLeaveEveryStateUnchanged() {
        List<CreateSalesOrderMessage> invalid = List.of(new CreateSalesOrderMessage(-1,1,1), new CreateSalesOrderMessage(1,1,1),
                new CreateSalesOrderMessage(0,0,1), new CreateSalesOrderMessage(0,1,0), new CreateSalesOrderMessage(0,21,1));
        for (CreateSalesOrderMessage message : invalid) { Fixture f = new Fixture(); f.inventory.count = 20;
            assertNotEquals(CreateSalesOrderResult.SUCCESS, f.execute(message)); assertUnchanged(f, 20, 1000); }
    }

    @Test void emptySlotAndSnapshotFailureDoNotMutate() {
        Fixture empty = new Fixture(); empty.inventory.count = 0;
        assertEquals(CreateSalesOrderResult.EMPTY_SLOT, empty.execute(1,1)); assertUnchanged(empty,0,1000);
        Fixture rejected = new Fixture(); rejected.inventory.captureFails = true;
        assertEquals(CreateSalesOrderResult.SNAPSHOT_REJECTED, rejected.execute(1,1)); assertUnchanged(rejected,20,1000);
    }

    @Test void nullIdAndClockOverflowDoNotMutate() {
        Fixture nullId = new Fixture(); nullId.id = null;
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, nullId.execute(1,1)); assertUnchanged(nullId,20,1000);
        Fixture overflow = new Fixture(); overflow.now = Long.MAX_VALUE;
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, overflow.execute(1,1)); assertUnchanged(overflow,20,1000);
    }

    @Test void taxUsesIntegerCeilingAtAllBoundaries() {
        assertEquals(1, CreateSalesOrderService.taxFor(1)); assertEquals(1, CreateSalesOrderService.taxFor(9));
        assertEquals(1, CreateSalesOrderService.taxFor(10)); assertEquals(2, CreateSalesOrderService.taxFor(11));
        assertEquals(214748365, CreateSalesOrderService.taxFor(Integer.MAX_VALUE));
    }

    @Test void longMatchingCountCanExceedIntegerRange() {
        Fixture f = new Fixture(); f.inventory.available = (long) Integer.MAX_VALUE + 10L;
        assertEquals(CreateSalesOrderResult.SUCCESS, f.execute(1,1));
    }

    @Test void failedOrPartiallyFailedRemovalIsAlreadyRestored() {
        Fixture failed = new Fixture(); failed.inventory.removalFails = true;
        assertEquals(CreateSalesOrderResult.INVENTORY_MUTATION_FAILED, failed.execute(5,10)); assertUnchanged(failed,20,1000);
        Fixture partial = new Fixture(); partial.inventory.partialThenFails = true;
        assertEquals(CreateSalesOrderResult.INVENTORY_MUTATION_FAILED, partial.execute(5,10)); assertUnchanged(partial,20,1000);
    }

    @Test void removalExceptionDoesNotEscapeAndAdapterStateIsRestored() {
        Fixture f = new Fixture(); f.inventory.removeThrows = true;
        assertEquals(CreateSalesOrderResult.INVENTORY_MUTATION_FAILED, f.execute(5,10)); assertUnchanged(f,20,1000);
    }

    @Test void failedRemovalRestoreIsRollbackFailure() {
        Fixture f = new Fixture(); f.inventory.removalFails = true; f.inventory.failureRestoreFails = true;
        assertEquals(CreateSalesOrderResult.ROLLBACK_FAILED, f.execute(5,10));
    }

    @Test void knownDebitFailureRestoresInventoryAndUnknownDebitStateIsSurfaced() {
        Fixture rejected = new Fixture(); rejected.account.debitFalse = true;
        assertEquals(CreateSalesOrderResult.TAX_MUTATION_FAILED, rejected.execute(5,10)); assertUnchanged(rejected,20,1000);
        Fixture thrown = new Fixture(); thrown.account.debitThrows = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, thrown.execute(5,10)); assertUnchanged(thrown,20,1000);
        Fixture returnedNull = new Fixture(); returnedNull.account.debitNull = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, returnedNull.execute(5,10)); assertUnchanged(returnedNull,20,1000);
        Fixture mutatedThenThrew = new Fixture(); mutatedThenThrew.account.debitMutatesThenThrows = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, mutatedThenThrew.execute(5,10));
        assertEquals(20, mutatedThenThrew.inventory.count); assertEquals(999, mutatedThenThrew.account.balance);
        assertEquals(0, mutatedThenThrew.account.creditCalls);
    }

    @Test void repositoryFalseOrExceptionRestoresTaxAndInventory() {
        Fixture rejected = new Fixture(); rejected.repository.reject = true;
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, rejected.execute(5,10)); assertUnchanged(rejected,20,1000);
        Fixture thrown = new Fixture(); thrown.repository.throwsOnAdd = true;
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, thrown.execute(5,10)); assertUnchanged(thrown,20,1000);
    }

    @Test void knownCreditFailureAndUnknownCreditStateStillRestoreInventory() {
        Fixture rejected = new Fixture(); rejected.repository.reject = true; rejected.account.creditFalse = true;
        assertEquals(CreateSalesOrderResult.ROLLBACK_FAILED, rejected.execute(5,10));
        assertEquals(20, rejected.inventory.count); assertEquals(1, rejected.inventory.rollbackCalls);

        Fixture thrown = new Fixture(); thrown.repository.reject = true; thrown.account.creditThrows = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, thrown.execute(5,10));
        assertEquals(20, thrown.inventory.count); assertEquals(1, thrown.inventory.rollbackCalls);

        Fixture returnedNull = new Fixture(); returnedNull.repository.reject = true; returnedNull.account.creditNull = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, returnedNull.execute(5,10));
        assertEquals(20, returnedNull.inventory.count); assertEquals(1, returnedNull.inventory.rollbackCalls);

        Fixture mutatedThenThrew = new Fixture(); mutatedThenThrew.repository.reject = true;
        mutatedThenThrew.account.creditMutatesThenThrows = true;
        assertEquals(CreateSalesOrderResult.STATE_UNKNOWN, mutatedThenThrew.execute(5,10));
        assertEquals(20, mutatedThenThrew.inventory.count); assertEquals(1000, mutatedThenThrew.account.balance);
    }

    @Test void inventoryRollbackFailureStillAttemptsTaxCompensation() {
        for (boolean throwsRollback : List.of(false, true)) { Fixture f = new Fixture(); f.repository.reject = true;
            if (throwsRollback) f.inventory.rollbackThrows = true; else f.inventory.rollbackFalse = true;
            assertEquals(CreateSalesOrderResult.ROLLBACK_FAILED, f.execute(5,10));
            assertEquals(1000, f.account.balance); assertEquals(1, f.account.creditCalls); assertEquals(1, f.inventory.rollbackCalls); }
    }

    @Test void duplicateIdPreservesOldOrderAndCompensates() {
        Fixture f = new Fixture(); MarketOrder old = f.order(f.id); f.repository.orders.add(old);
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, f.execute(5,10));
        assertEquals(List.of(old), f.repository.orders); assertEquals(20, f.inventory.count); assertEquals(1000, f.account.balance);
    }

    @Test void fundsAndCapacityFailBeforeMutation() {
        Fixture funds = new Fixture(); funds.account.balance = 0;
        assertEquals(CreateSalesOrderResult.INSUFFICIENT_FUNDS, funds.execute(1,11)); assertUnchanged(funds,20,0);
        Fixture full = new Fixture(); full.repository.full = true;
        assertEquals(CreateSalesOrderResult.REPOSITORY_FULL, full.execute(1,11)); assertUnchanged(full,20,1000);
    }

    private static void assertUnchanged(Fixture f, int items, int balance) {
        assertEquals(items, f.inventory.count); assertEquals(balance, f.account.balance); assertTrue(f.repository.orders.isEmpty());
    }

    private static final class Fixture {
        final FakeInventory inventory = new FakeInventory(); final FakeAccount account = new FakeAccount();
        final FakeRepository repository = new FakeRepository(); final List<String> reports = new ArrayList<>();
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000020"); long now = 1000L;
        CreateSalesOrderResult execute(int quantity, int price) { return execute(new CreateSalesOrderMessage(0,quantity,price)); }
        CreateSalesOrderResult execute(CreateSalesOrderMessage message) { return CreateSalesOrderService.execute(message,
                new CreateSalesOrderService.Context(inventory,account,repository,UUID.randomUUID(),"seller",()->id,()->now,
                        (tradeId,stage,result,cause,compensation)->reports.add(stage+":"+result))); }
        MarketOrder order(UUID tradeId) { return new MarketOrder(MarketOrderType.SALES,tradeId,MarketOrderCodecTest.item(),1,1,"old",UUID.randomUUID(),1,2,false); }
    }
    private static final class FakeInventory implements CreateSalesOrderService.Inventory {
        int count=20, rollbackCalls; long available=-1; boolean captureFails, removalFails, partialThenFails, removeThrows,
                failureRestoreFails, rollbackFalse, rollbackThrows;
        public int slotCount(){return 1;} public Object copySlot(int slot){return count==0?null:new Object();}
        public Object unitTemplate(Object stack){return stack;}
        public ItemStackSnapshotResult<ItemStackSnapshot> capture(Object template){return captureFails
                ? ItemStackSnapshotResult.failure(ItemStackSnapshotError.UNSUPPORTED_COMPONENT,"test")
                : ItemStackSnapshotResult.success(MarketOrderCodecTest.item());}
        public long countMatching(Object template){return available>=0?available:count;}
        public CreateSalesOrderService.RemovalResult removeMatching(Object template,int quantity){
            int before=count;
            if(removeThrows){count=before;throw new IllegalStateException("remove");}
            if(partialThenFails){count-=Math.min(2,quantity);count=before;return CreateSalesOrderService.RemovalResult.failure(true);}
            if(removalFails)return CreateSalesOrderService.RemovalResult.failure(!failureRestoreFails);
            count-=quantity;
            return CreateSalesOrderService.RemovalResult.success(()->{rollbackCalls++;if(rollbackThrows)throw new IllegalStateException("rollback");if(rollbackFalse)return false;count=before;return true;});
        }
    }
    private static final class FakeAccount implements CreateSalesOrderService.Account {
        int balance=1000,debits,creditCalls;
        boolean debitFalse,debitThrows,debitNull,debitMutatesThenThrows;
        boolean creditFalse,creditThrows,creditNull,creditMutatesThenThrows;
        public boolean canDebit(int amount){return balance>=amount;}
        public BalanceMutationResult debitExact(int amount){debits++;if(debitMutatesThenThrows){balance-=amount;throw new IllegalStateException("debit");}if(debitThrows)throw new IllegalStateException("debit");if(debitNull)return null;if(debitFalse)return BalanceMutationResult.PERSIST_FAILED;balance-=amount;return BalanceMutationResult.SUCCESS;}
        public BalanceMutationResult creditExact(int amount){creditCalls++;if(creditMutatesThenThrows){balance+=amount;throw new IllegalStateException("credit");}if(creditThrows)throw new IllegalStateException("credit");if(creditNull)return null;if(creditFalse)return BalanceMutationResult.PERSIST_FAILED;balance+=amount;return BalanceMutationResult.SUCCESS;}
    }
    private static final class FakeRepository implements CreateSalesOrderService.Repository {
        final List<MarketOrder> orders=new ArrayList<>(); boolean full,reject,throwsOnAdd;
        public boolean isFull(){return full;}
        public boolean add(MarketOrder order){if(throwsOnAdd)throw new IllegalStateException("repo");if(reject||orders.stream().anyMatch(o->o.tradeId().equals(order.tradeId())))return false;orders.add(order);return true;}
    }
}
