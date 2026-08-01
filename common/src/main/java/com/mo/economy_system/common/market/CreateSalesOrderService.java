package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;

import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Validates protocol 8 and applies inventory, tax, and order changes with independent compensation. */
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
        int tax = taxFor(message.totalPrice());
        if (tax < 1) return CreateSalesOrderResult.TAX_OVERFLOW;
        if (!context.account().canDebit(tax)) return CreateSalesOrderResult.INSUFFICIENT_FUNDS;
        if (context.repository().isFull()) return CreateSalesOrderResult.REPOSITORY_FULL;

        long now = context.clock().getAsLong();
        long expiration;
        try {
            expiration = Math.addExact(now, MarketOrder.EXPIRATION_DURATION_MILLIS);
        } catch (ArithmeticException exception) {
            report(context, null, "expiration", CreateSalesOrderResult.ORDER_PERSIST_FAILED, null, Compensation.notRequired());
            return CreateSalesOrderResult.ORDER_PERSIST_FAILED;
        }
        UUID tradeId = context.ids().get();
        if (tradeId == null) return CreateSalesOrderResult.ORDER_PERSIST_FAILED;
        MarketOrder order = new MarketOrder(MarketOrderType.SALES, tradeId, captured.orElseThrow(),
                message.quantity(), message.totalPrice(), context.sellerName(), context.sellerId(), now, expiration, false);

        RemovalResult removal;
        try {
            removal = context.inventory().removeMatching(template, message.quantity());
        } catch (RuntimeException exception) {
            report(context, tradeId, "inventory-remove", CreateSalesOrderResult.INVENTORY_MUTATION_FAILED, exception, Compensation.notRequired());
            return CreateSalesOrderResult.INVENTORY_MUTATION_FAILED;
        }
        if (removal == null || !removal.succeeded() || removal.rollback() == null) {
            CreateSalesOrderResult result = removal != null && !removal.failureRestored()
                    ? CreateSalesOrderResult.ROLLBACK_FAILED : CreateSalesOrderResult.INVENTORY_MUTATION_FAILED;
            Compensation compensation = removal != null && !removal.failureRestored()
                    ? new Compensation(false, true, true, false, null, null) : Compensation.notRequired();
            report(context, tradeId, "inventory-remove", result, null, compensation);
            return result;
        }

        BalanceMutationResult debitResult;
        RuntimeException debitException = null;
        try {
            debitResult = context.account().debitExact(tax);
        } catch (RuntimeException exception) {
            debitResult = BalanceMutationResult.PERSIST_FAILED;
            debitException = exception;
        }
        if (debitResult != BalanceMutationResult.SUCCESS) {
            Compensation compensation = compensate(context, tax, false, removal.rollback());
            CreateSalesOrderResult result = compensation.complete()
                    ? CreateSalesOrderResult.TAX_MUTATION_FAILED : CreateSalesOrderResult.ROLLBACK_FAILED;
            report(context, tradeId, "tax-debit", result, debitException, compensation);
            return result;
        }

        boolean added;
        RuntimeException repositoryException = null;
        try {
            added = context.repository().add(order);
        } catch (RuntimeException exception) {
            added = false;
            repositoryException = exception;
        }
        if (!added) {
            Compensation compensation = compensate(context, tax, true, removal.rollback());
            CreateSalesOrderResult result = compensation.complete()
                    ? CreateSalesOrderResult.ORDER_PERSIST_FAILED : CreateSalesOrderResult.ROLLBACK_FAILED;
            report(context, tradeId, "repository-add", result, repositoryException, compensation);
            return result;
        }
        return CreateSalesOrderResult.SUCCESS;
    }

    public static int taxFor(int totalPrice) {
        if (totalPrice <= 0) return -1;
        long tax = ((long) totalPrice + 9L) / 10L;
        return tax > Integer.MAX_VALUE ? -1 : (int) tax;
    }

    private static Compensation compensate(Context context, int tax, boolean taxDebited, Removal removal) {
        boolean taxAttempted = taxDebited;
        boolean taxRestored = !taxDebited;
        RuntimeException taxError = null;
        if (taxDebited) {
            try { taxRestored = context.account().creditExact(tax) == BalanceMutationResult.SUCCESS; }
            catch (RuntimeException exception) { taxRestored = false; taxError = exception; }
        }
        boolean inventoryRestored;
        RuntimeException inventoryError = null;
        try { inventoryRestored = removal.rollback(); }
        catch (RuntimeException exception) { inventoryRestored = false; inventoryError = exception; }
        return new Compensation(taxAttempted, taxRestored, true, inventoryRestored, taxError, inventoryError);
    }

    private static void report(Context context, UUID tradeId, String stage, CreateSalesOrderResult result,
                               RuntimeException cause, Compensation compensation) {
        try { context.reporter().report(tradeId, stage, result, cause, compensation); }
        catch (RuntimeException ignored) { /* Logging/reporting must never change the transaction result. */ }
    }

    public record Context(Inventory inventory, Account account, Repository repository, UUID sellerId,
                          String sellerName, Supplier<UUID> ids, LongSupplier clock, FailureReporter reporter) {
        public Context {
            Objects.requireNonNull(inventory); Objects.requireNonNull(account); Objects.requireNonNull(repository);
            Objects.requireNonNull(sellerId); Objects.requireNonNull(sellerName); Objects.requireNonNull(ids);
            Objects.requireNonNull(clock); Objects.requireNonNull(reporter);
        }
    }
    public interface Inventory {
        int slotCount(); Object copySlot(int slot); Object unitTemplate(Object stack);
        ItemStackSnapshotResult<ItemStackSnapshot> capture(Object template);
        long countMatching(Object template);
        /** Failure or exception must leave inventory exactly as it was before this call. */
        RemovalResult removeMatching(Object template, int quantity);
    }
    public interface Removal { boolean rollback(); }
    public record RemovalResult(boolean succeeded, boolean failureRestored, Removal rollback) {
        public static RemovalResult success(Removal rollback) { return new RemovalResult(true, true, Objects.requireNonNull(rollback)); }
        public static RemovalResult failure(boolean restored) { return new RemovalResult(false, restored, null); }
    }
    public interface Account { boolean canDebit(int amount); BalanceMutationResult debitExact(int amount); BalanceMutationResult creditExact(int amount); }
    public interface Repository {
        boolean isFull();
        /** Returning false or throwing must never leave the supplied order in the repository. */
        boolean add(MarketOrder order);
    }
    public interface FailureReporter {
        void report(UUID tradeId, String stage, CreateSalesOrderResult result, RuntimeException cause, Compensation compensation);
        static FailureReporter noop() { return (tradeId, stage, result, cause, compensation) -> {}; }
    }
    public record Compensation(boolean taxAttempted, boolean taxRestored, boolean inventoryAttempted,
                               boolean inventoryRestored, RuntimeException taxError, RuntimeException inventoryError) {
        public static Compensation notRequired() { return new Compensation(false, true, false, true, null, null); }
        public boolean complete() { return taxRestored && inventoryRestored; }
    }
}
