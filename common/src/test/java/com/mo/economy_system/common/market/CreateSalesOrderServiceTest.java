package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateSalesOrderServiceTest {
    @Test void createsUnitTemplateKeepsQuantitySeparateAndChargesCeilingTax() {
        FakeInventory inventory = new FakeInventory(20); FakeAccount account = new FakeAccount(100); FakeRepository repo = new FakeRepository();
        assertEquals(CreateSalesOrderResult.SUCCESS, execute(new CreateSalesOrderMessage(0, 12, 101), inventory, account, repo));
        assertEquals(8, inventory.count); assertEquals(89, account.balance); assertEquals(1, repo.order.item().count());
        assertEquals(12, repo.order.quantity()); assertEquals(101, repo.order.totalPrice());
    }

    @Test void validatesWithoutMutation() {
        for (CreateSalesOrderMessage message : List.of(new CreateSalesOrderMessage(-1, 1, 1), new CreateSalesOrderMessage(0, 0, 1),
                new CreateSalesOrderMessage(0, 1, 0), new CreateSalesOrderMessage(0, 21, 1))) {
            FakeInventory inventory = new FakeInventory(20); FakeAccount account = new FakeAccount(100); FakeRepository repo = new FakeRepository();
            assertNotEquals(CreateSalesOrderResult.SUCCESS, execute(message, inventory, account, repo));
            assertEquals(20, inventory.count); assertEquals(100, account.balance); assertNull(repo.order);
        }
    }

    @Test void rollsBackItemsAndTaxWhenRepositoryRejects() {
        FakeInventory inventory = new FakeInventory(20); FakeAccount account = new FakeAccount(100); FakeRepository repo = new FakeRepository(); repo.reject = true;
        assertEquals(CreateSalesOrderResult.ORDER_PERSIST_FAILED, execute(new CreateSalesOrderMessage(0, 5, 10), inventory, account, repo));
        assertEquals(20, inventory.count); assertEquals(100, account.balance); assertNull(repo.order);
    }

    @Test void repositoryCapacityAndFundsAreCheckedBeforeMutation() {
        FakeInventory inventory = new FakeInventory(20); FakeAccount account = new FakeAccount(0); FakeRepository repo = new FakeRepository();
        assertEquals(CreateSalesOrderResult.INSUFFICIENT_FUNDS, execute(new CreateSalesOrderMessage(0, 1, 1), inventory, account, repo));
        account.balance = 100; repo.full = true;
        assertEquals(CreateSalesOrderResult.REPOSITORY_FULL, execute(new CreateSalesOrderMessage(0, 1, 1), inventory, account, repo));
        assertEquals(20, inventory.count); assertEquals(100, account.balance);
    }

    private static CreateSalesOrderResult execute(CreateSalesOrderMessage message, FakeInventory inventory, FakeAccount account, FakeRepository repo) {
        return CreateSalesOrderService.execute(message, new CreateSalesOrderService.Context(inventory, account, repo,
                UUID.fromString("00000000-0000-0000-0000-000000000010"), "seller",
                () -> UUID.fromString("00000000-0000-0000-0000-000000000020"), () -> 1000L));
    }
    private static final class FakeInventory implements CreateSalesOrderService.Inventory {
        int count; FakeInventory(int count) { this.count = count; }
        public int slotCount() { return 1; } public Object copySlot(int slot) { return count == 0 ? null : new Object(); }
        public Object unitTemplate(Object stack) { return stack; }
        public ItemStackSnapshotResult<ItemStackSnapshot> capture(Object template) { return ItemStackSnapshotResult.success(MarketOrderCodecTest.item()); }
        public long countMatching(Object template) { return count; }
        public CreateSalesOrderService.Removal removeMatching(Object template, int quantity) { int before = count; count -= quantity; return () -> { count = before; return true; }; }
    }
    private static final class FakeAccount implements CreateSalesOrderService.Account {
        int balance; FakeAccount(int balance) { this.balance = balance; }
        public boolean canDebit(int amount) { return balance >= amount; }
        public boolean debit(int amount) { if (!canDebit(amount)) return false; balance -= amount; return true; }
        public boolean credit(int amount) { balance += amount; return true; }
    }
    private static final class FakeRepository implements CreateSalesOrderService.Repository {
        boolean full, reject; MarketOrder order;
        public boolean isFull() { return full; }
        public boolean add(MarketOrder value) { if (reject) return false; order = value; return true; }
    }
}
