package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative protocol 12 transaction. */
public final class PurchaseSalesOrderService {
    private PurchaseSalesOrderService() {}

    public static PurchaseSalesOrderOutcome execute(PurchaseSalesOrderMessage message, Context context) {
        if (message == null || context == null) return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.INVALID_CONTEXT);
        MarketOrder preview;
        try { preview = context.repository().find(message.tradeId()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), null, "lookup", PurchaseSalesOrderResult.ORDER_REMOVE_FAILED,
                    null, null, exception);
            return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
        }
        PurchaseSalesOrderResult validation = validate(preview, context);
        if (validation != PurchaseSalesOrderResult.SUCCESS) return PurchaseSalesOrderOutcome.failure(validation);

        Object template;
        try { template = context.inventory().restore(preview); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "snapshot-restore",
                    PurchaseSalesOrderResult.ITEM_RESTORE_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);
        }
        if (template == null) return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);

        BalanceTransferResult balancePreview = context.accounts().preview(preview.sellerId(), preview.totalPrice());
        PurchaseSalesOrderResult balanceFailure = mapBalanceFailure(balancePreview);
        if (balanceFailure != PurchaseSalesOrderResult.SUCCESS) return PurchaseSalesOrderOutcome.failure(balanceFailure);
        if (!context.inventory().canAccept(template, preview.quantity())) {
            return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.INVENTORY_FULL);
        }

        SalesOrderRemovalResult removalResult;
        try { removalResult = context.repository().removeSalesForPurchase(message.tradeId()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "order-remove",
                    PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.failure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
        }
        if (removalResult.status() != SalesOrderRemovalStatus.REMOVED || removalResult.removal() == null) {
            return PurchaseSalesOrderOutcome.failure(mapRemovalFailure(removalResult.status()));
        }
        MarketOrder authoritative = removalResult.removal().order();
        if (!preview.equals(authoritative)) {
            boolean restored = restoreOrder(removalResult.removal());
            PurchaseSalesOrderResult result = restored ? PurchaseSalesOrderResult.ORDER_CHANGED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "order-changed", result, null, restored, null);
            return new PurchaseSalesOrderOutcome(result, Optional.of(authoritative), !restored);
        }

        InsertionResult insertion;
        try { insertion = context.inventory().insert(template, authoritative.quantity()); }
        catch (RuntimeException exception) {
            boolean orderRestored = restoreOrder(removalResult.removal());
            PurchaseSalesOrderResult result = orderRestored ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert", result, null, orderRestored, exception);
            return new PurchaseSalesOrderOutcome(result, Optional.of(authoritative), !orderRestored);
        }
        if (insertion == null || !insertion.succeeded() || insertion.rollback() == null) {
            boolean orderRestored = restoreOrder(removalResult.removal());
            boolean inventoryRestored = insertion == null || insertion.failureRestored();
            PurchaseSalesOrderResult result = orderRestored && inventoryRestored
                    ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert", result,
                    inventoryRestored, orderRestored, null);
            return new PurchaseSalesOrderOutcome(result, Optional.of(authoritative), !orderRestored);
        }

        BalanceTransferResult transfer;
        RuntimeException transferException = null;
        try { transfer = context.accounts().transfer(authoritative.sellerId(), authoritative.totalPrice()); }
        catch (RuntimeException exception) { transfer = BalanceTransferResult.PERSIST_FAILED; transferException = exception; }
        if (transfer != BalanceTransferResult.SUCCESS) {
            boolean inventoryRestored = rollbackInventory(insertion.rollback());
            boolean orderRestored = restoreOrder(removalResult.removal());
            PurchaseSalesOrderResult result = inventoryRestored && orderRestored
                    ? mapCommittedTransferFailure(transfer) : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "payment", result,
                    inventoryRestored, orderRestored, transferException);
            return new PurchaseSalesOrderOutcome(result, Optional.of(authoritative), !orderRestored);
        }
        return new PurchaseSalesOrderOutcome(PurchaseSalesOrderResult.SUCCESS, Optional.of(authoritative), true);
    }

    private static PurchaseSalesOrderResult validate(MarketOrder order, Context context) {
        if (order == null) return PurchaseSalesOrderResult.NOT_FOUND;
        if (order.type() != MarketOrderType.SALES) return PurchaseSalesOrderResult.WRONG_ORDER_TYPE;
        if (context.buyerId().equals(order.sellerId())) return PurchaseSalesOrderResult.SELF_PURCHASE;
        if (order.totalPrice() <= 0) return PurchaseSalesOrderResult.INVALID_PRICE;
        if (order.quantity() <= 0) return PurchaseSalesOrderResult.INVALID_QUANTITY;
        if (order.item().count() != 1 || !ItemStackSnapshotValidator.validate(order.item()).isSuccess()) {
            return PurchaseSalesOrderResult.INVALID_SNAPSHOT;
        }
        return PurchaseSalesOrderResult.SUCCESS;
    }

    private static PurchaseSalesOrderResult mapRemovalFailure(SalesOrderRemovalStatus status) {
        return switch (status) {
            case NOT_FOUND -> PurchaseSalesOrderResult.NOT_FOUND;
            case WRONG_ORDER_TYPE -> PurchaseSalesOrderResult.WRONG_ORDER_TYPE;
            case PERSIST_FAILED -> PurchaseSalesOrderResult.ORDER_REMOVE_FAILED;
            case REMOVED -> PurchaseSalesOrderResult.ORDER_REMOVE_FAILED;
        };
    }

    private static PurchaseSalesOrderResult mapBalanceFailure(BalanceTransferResult result) {
        return switch (result) {
            case SUCCESS -> PurchaseSalesOrderResult.SUCCESS;
            case INSUFFICIENT_FUNDS -> PurchaseSalesOrderResult.INSUFFICIENT_FUNDS;
            case RECIPIENT_BALANCE_LIMIT -> PurchaseSalesOrderResult.SELLER_BALANCE_LIMIT;
            default -> PurchaseSalesOrderResult.PAYMENT_FAILED;
        };
    }

    private static PurchaseSalesOrderResult mapCommittedTransferFailure(BalanceTransferResult result) {
        PurchaseSalesOrderResult mapped = mapBalanceFailure(result);
        return mapped == PurchaseSalesOrderResult.SUCCESS ? PurchaseSalesOrderResult.PAYMENT_FAILED : mapped;
    }

    private static boolean restoreOrder(MarketOrderRemoval removal) {
        try { return removal.restore().restore() == MarketOrderRestoreResult.RESTORED; }
        catch (RuntimeException exception) { return false; }
    }

    private static boolean rollbackInventory(Insertion rollback) {
        try { return rollback.rollback(); }
        catch (RuntimeException exception) { return false; }
    }

    private static void report(Context context, UUID tradeId, UUID sellerId, String stage,
                               PurchaseSalesOrderResult result, Boolean inventoryRollback,
                               Boolean orderRestore, RuntimeException exception) {
        try { context.reporter().report(tradeId, context.buyerId(), sellerId, stage, result,
                inventoryRollback, orderRestore, exception); }
        catch (RuntimeException ignored) { }
    }

    public record Context(UUID buyerId, Inventory inventory, Accounts accounts, Repository repository,
                          FailureReporter reporter) {
        public Context { Objects.requireNonNull(buyerId); Objects.requireNonNull(inventory); Objects.requireNonNull(accounts); Objects.requireNonNull(repository); Objects.requireNonNull(reporter); }
    }
    public interface Inventory {
        Object restore(MarketOrder order);
        boolean canAccept(Object template, int quantity);
        InsertionResult insert(Object template, int quantity);
    }
    public interface Insertion { boolean rollback(); }
    public record InsertionResult(boolean succeeded, boolean failureRestored, Insertion rollback) {
        public static InsertionResult success(Insertion rollback) { return new InsertionResult(true, true, Objects.requireNonNull(rollback)); }
        public static InsertionResult failure(boolean restored) { return new InsertionResult(false, restored, null); }
    }
    public interface Accounts {
        BalanceTransferResult preview(UUID sellerId, int amount);
        BalanceTransferResult transfer(UUID sellerId, int amount);
    }
    public interface Repository {
        MarketOrder find(UUID tradeId);
        SalesOrderRemovalResult removeSalesForPurchase(UUID tradeId);
    }
    public interface FailureReporter {
        void report(UUID tradeId, UUID buyerId, UUID sellerId, String stage, PurchaseSalesOrderResult result,
                    Boolean inventoryRollback, Boolean orderRestore, RuntimeException exception);
        static FailureReporter noop() { return (tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception)->{}; }
    }
}
