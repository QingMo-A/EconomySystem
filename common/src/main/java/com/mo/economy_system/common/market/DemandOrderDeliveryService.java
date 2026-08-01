package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.UUID;

/** Transaction semantics used by the legacy NeoForge demand-delivery packet. */
public final class DemandOrderDeliveryService {
    private DemandOrderDeliveryService() {}

    public static DemandOrderDeliveryResult execute(UUID tradeId, Context context) {
        if (tradeId == null || context == null) return DemandOrderDeliveryResult.INVALID_CONTEXT;
        MarketOrder order = context.repository().find(tradeId);
        if (order == null) return DemandOrderDeliveryResult.NOT_FOUND;
        if (order.type() != MarketOrderType.DEMAND) return DemandOrderDeliveryResult.WRONG_ORDER_TYPE;
        if (order.delivered()) return DemandOrderDeliveryResult.ALREADY_DELIVERED;
        if (order.sellerId().equals(context.supplierId())) return DemandOrderDeliveryResult.SELF_DELIVERY;
        if (order.totalPrice() <= 0) return DemandOrderDeliveryResult.INVALID_PRICE;
        if (order.quantity() <= 0) return DemandOrderDeliveryResult.INVALID_QUANTITY;
        Object template = context.inventory().restoreTemplate(order);
        if (template == null) return DemandOrderDeliveryResult.TEMPLATE_RESTORE_FAILED;
        if (context.inventory().countMatching(template) < order.quantity()) return DemandOrderDeliveryResult.INSUFFICIENT_ITEMS;

        RemovalResult removal;
        try { removal = context.inventory().removeMatching(template, order.quantity()); }
        catch (RuntimeException exception) {
            report(context, tradeId, "inventory-remove", DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED,
                    exception, Compensation.notRequired());
            return DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED;
        }
        if (removal == null || !removal.succeeded() || removal.rollback() == null) {
            DemandOrderDeliveryResult result = removal != null && !removal.failureRestored()
                    ? DemandOrderDeliveryResult.ROLLBACK_FAILED : DemandOrderDeliveryResult.INVENTORY_MUTATION_FAILED;
            report(context, tradeId, "inventory-remove", result, null,
                    new Compensation(false, true, removal != null && !removal.failureRestored(), removal == null || removal.failureRestored(), null, null));
            return result;
        }

        boolean paid;
        RuntimeException paymentError = null;
        try { paid = context.account().credit(order.totalPrice()); }
        catch (RuntimeException exception) { paid = false; paymentError = exception; }
        if (!paid) {
            boolean restored;
            RuntimeException inventoryError = null;
            try { restored = removal.rollback().rollback(); }
            catch (RuntimeException exception) { restored = false; inventoryError = exception; }
            DemandOrderDeliveryResult result = restored ? DemandOrderDeliveryResult.PAYMENT_FAILED : DemandOrderDeliveryResult.ROLLBACK_FAILED;
            report(context, tradeId, "payment", result, paymentError,
                    new Compensation(false, true, true, restored, null, inventoryError));
            return result;
        }

        DemandDeliveryTransitionResult transition;
        RuntimeException ledgerError = null;
        try { transition = context.repository().markDelivered(tradeId); }
        catch (RuntimeException exception) { transition = DemandDeliveryTransitionResult.PERSIST_FAILED; ledgerError = exception; }
        if (transition != DemandDeliveryTransitionResult.UPDATED) {
            Compensation compensation = compensate(context, order.totalPrice(), removal.rollback());
            DemandOrderDeliveryResult result = compensation.complete()
                    ? DemandOrderDeliveryResult.LEDGER_UPDATE_FAILED : DemandOrderDeliveryResult.ROLLBACK_FAILED;
            report(context, tradeId, "ledger-transition-" + transition, result, ledgerError, compensation);
            return result;
        }
        return DemandOrderDeliveryResult.SUCCESS;
    }

    private static Compensation compensate(Context context, int amount, Removal rollback) {
        boolean paymentReverted;
        RuntimeException paymentError = null;
        try { paymentReverted = context.account().debit(amount); }
        catch (RuntimeException exception) { paymentReverted = false; paymentError = exception; }
        boolean inventoryRestored;
        RuntimeException inventoryError = null;
        try { inventoryRestored = rollback.rollback(); }
        catch (RuntimeException exception) { inventoryRestored = false; inventoryError = exception; }
        return new Compensation(true, paymentReverted, true, inventoryRestored, paymentError, inventoryError);
    }

    private static void report(Context context, UUID tradeId, String stage, DemandOrderDeliveryResult result,
                               RuntimeException cause, Compensation compensation) {
        try { context.reporter().report(tradeId, stage, result, cause, compensation); }
        catch (RuntimeException ignored) { }
    }

    public record Context(UUID supplierId, Inventory inventory, Account account, Repository repository, FailureReporter reporter) {
        public Context { Objects.requireNonNull(supplierId);Objects.requireNonNull(inventory);Objects.requireNonNull(account);Objects.requireNonNull(repository);Objects.requireNonNull(reporter); }
    }
    public interface Inventory {
        Object restoreTemplate(MarketOrder order);
        long countMatching(Object template);
        /** Failure must restore the complete pre-operation inventory before returning. */
        RemovalResult removeMatching(Object template, int quantity);
    }
    public interface Removal { boolean rollback(); }
    public record RemovalResult(boolean succeeded, boolean failureRestored, Removal rollback) {
        public static RemovalResult success(Removal rollback) { return new RemovalResult(true, true, Objects.requireNonNull(rollback)); }
        public static RemovalResult failure(boolean restored) { return new RemovalResult(false, restored, null); }
    }
    public interface Account { boolean credit(int amount); boolean debit(int amount); }
    public interface Repository { MarketOrder find(UUID tradeId); DemandDeliveryTransitionResult markDelivered(UUID tradeId); }
    public interface FailureReporter {
        void report(UUID tradeId, String stage, DemandOrderDeliveryResult result, RuntimeException cause, Compensation compensation);
        static FailureReporter noop() { return (tradeId,stage,result,cause,compensation)->{}; }
    }
    public record Compensation(boolean paymentAttempted, boolean paymentReverted, boolean inventoryAttempted,
                               boolean inventoryRestored, RuntimeException paymentError, RuntimeException inventoryError) {
        public static Compensation notRequired() { return new Compensation(false,true,false,true,null,null); }
        public boolean complete() { return paymentReverted && inventoryRestored; }
    }
}
