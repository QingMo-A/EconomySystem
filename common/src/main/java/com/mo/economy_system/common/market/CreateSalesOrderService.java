package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;

import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Validates protocol 8 and applies inventory, tax, and order changes with compensation. */
public final class CreateSalesOrderService {
    private CreateSalesOrderService() {}

    public static CreateSalesOrderResult execute(CreateSalesOrderMessage message, Context context) {
        if (message == null || context == null) return CreateSalesOrderResult.INVALID_CONTEXT;
        if (message.slot() < 0 || message.slot() >= context.inventory().slotCount()) return CreateSalesOrderResult.INVALID_SLOT;
        if (message.quantity() <= 0) return CreateSalesOrderResult.INVALID_QUANTITY;
        if (message.totalPrice() <= 0) return CreateSalesOrderResult.INVALID_PRICE;
        Object selected = context.inventory().copySlot(message.slot());
        if (selected == null) return CreateSalesOrderResult.EMPTY_SLOT;
        Object template = context.inventory().unitTemplate(selected);
        ItemStackSnapshotResult<ItemStackSnapshot> captured = context.inventory().capture(template);
        if (!captured.isSuccess()) return CreateSalesOrderResult.SNAPSHOT_REJECTED;
        long available = context.inventory().countMatching(template);
        if (available < message.quantity()) return CreateSalesOrderResult.INSUFFICIENT_ITEMS;
        long taxLong = ((long) message.totalPrice() + 9L) / 10L;
        if (taxLong < 1L || taxLong > Integer.MAX_VALUE) return CreateSalesOrderResult.TAX_OVERFLOW;
        int tax = (int) taxLong;
        if (!context.account().canDebit(tax)) return CreateSalesOrderResult.INSUFFICIENT_FUNDS;
        if (context.repository().isFull()) return CreateSalesOrderResult.REPOSITORY_FULL;

        long now = context.clock().getAsLong();
        long expiration;
        try {
            expiration = Math.addExact(now, MarketOrder.EXPIRATION_DURATION_MILLIS);
        } catch (ArithmeticException exception) {
            return CreateSalesOrderResult.ORDER_PERSIST_FAILED;
        }
        UUID tradeId = context.ids().get();
        if (tradeId == null) return CreateSalesOrderResult.ORDER_PERSIST_FAILED;
        MarketOrder order = new MarketOrder(MarketOrderType.SALES, tradeId, captured.orElseThrow(),
                message.quantity(), message.totalPrice(), context.sellerName(), context.sellerId(), now,
                expiration, false);

        Removal removal = context.inventory().removeMatching(template, message.quantity());
        if (removal == null) return CreateSalesOrderResult.INVENTORY_MUTATION_FAILED;
        if (!context.account().debit(tax)) {
            return removal.rollback() ? CreateSalesOrderResult.TAX_MUTATION_FAILED : CreateSalesOrderResult.ROLLBACK_FAILED;
        }
        boolean added;
        try {
            added = context.repository().add(order);
        } catch (RuntimeException exception) {
            added = false;
        }
        if (!added) {
            boolean taxRestored = context.account().credit(tax);
            boolean itemsRestored = removal.rollback();
            return taxRestored && itemsRestored ? CreateSalesOrderResult.ORDER_PERSIST_FAILED : CreateSalesOrderResult.ROLLBACK_FAILED;
        }
        return CreateSalesOrderResult.SUCCESS;
    }

    public record Context(Inventory inventory, Account account, Repository repository, UUID sellerId,
                          String sellerName, Supplier<UUID> ids, LongSupplier clock) {
        public Context { Objects.requireNonNull(inventory); Objects.requireNonNull(account); Objects.requireNonNull(repository);
            Objects.requireNonNull(sellerId); Objects.requireNonNull(sellerName); Objects.requireNonNull(ids); Objects.requireNonNull(clock); }
    }
    public interface Inventory {
        int slotCount(); Object copySlot(int slot); Object unitTemplate(Object stack);
        ItemStackSnapshotResult<ItemStackSnapshot> capture(Object template);
        long countMatching(Object template); Removal removeMatching(Object template, int quantity);
    }
    public interface Removal { boolean rollback(); }
    public interface Account { boolean canDebit(int amount); boolean debit(int amount); boolean credit(int amount); }
    public interface Repository { boolean isFull(); boolean add(MarketOrder order); }
}
