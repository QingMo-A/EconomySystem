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
        if (message == null || context == null) return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVALID_CONTEXT);
        MarketOrder preview;
        try { preview = context.repository().find(message.tradeId()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), null, "lookup", PurchaseSalesOrderResult.ORDER_REMOVE_FAILED,
                    null, null, exception);
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
        }
        PurchaseSalesOrderResult validation = validate(preview, context);
        if (validation != PurchaseSalesOrderResult.SUCCESS) return PurchaseSalesOrderOutcome.validationFailure(validation);

        Object template;
        try { template = context.materializer().restore(preview); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "snapshot-restore",
                    PurchaseSalesOrderResult.ITEM_RESTORE_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);
        }
        if (template == null) return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ITEM_RESTORE_FAILED);

        BalanceTransferResult balancePreview;
        try { balancePreview = context.accounts().preview(preview.sellerId(), preview.totalPrice()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "payment-preview", PurchaseSalesOrderResult.PAYMENT_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.PAYMENT_FAILED);
        }
        PurchaseSalesOrderResult balanceFailure = mapBalanceFailure(balancePreview);
        if (balanceFailure != PurchaseSalesOrderResult.SUCCESS) return PurchaseSalesOrderOutcome.validationFailure(balanceFailure);
        boolean accepts;
        try { accepts = context.inventory().canAccept(template, preview.quantity()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "inventory-capacity", PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED);
        }
        if (!accepts) {
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.INVENTORY_FULL);
        }

        SalesOrderRemovalResult removalResult;
        try { removalResult = context.repository().removeSalesTransactional(message.tradeId()); }
        catch (RuntimeException exception) {
            report(context, message.tradeId(), preview.sellerId(), "order-remove",
                    PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, null, null, exception);
            return PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
        }
        if (removalResult == null) {
            report(context, message.tradeId(), preview.sellerId(), "order-remove-null", PurchaseSalesOrderResult.ORDER_REMOVE_FAILED, null, null, null);
            return PurchaseSalesOrderOutcome.uncertainFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED);
        }
        if (removalResult.status() != SalesOrderRemovalStatus.REMOVED) {
            return PurchaseSalesOrderOutcome.validationFailure(mapRemovalFailure(removalResult.status()));
        }
        MarketOrder authoritative = removalResult.removal().order();
        if (!preview.equals(authoritative)) {
            boolean restored = restoreOrder(removalResult.removal());
            PurchaseSalesOrderResult result = restored ? PurchaseSalesOrderResult.ORDER_CHANGED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "order-changed", result, null, restored, null);
            return restored ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
                    : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
        }

        InventoryInsertionResult insertion;
        try { insertion = context.inventory().insert(template, authoritative.quantity()); }
        catch (RuntimeException exception) {
            boolean orderRestored = restoreOrder(removalResult.removal());
            PurchaseSalesOrderResult result = orderRestored ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert", result, null, orderRestored, exception);
            return orderRestored ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
                    : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
        }
        if (insertion == null || !insertion.succeeded() || insertion.rollback() == null) {
            boolean orderRestored = restoreOrder(removalResult.removal());
            boolean inventoryRestored = insertion != null && insertion.failureRestored();
            PurchaseSalesOrderResult result = orderRestored && inventoryRestored
                    ? PurchaseSalesOrderResult.INVENTORY_MUTATION_FAILED : PurchaseSalesOrderResult.ROLLBACK_FAILED;
            report(context, message.tradeId(), authoritative.sellerId(), "inventory-insert", result,
                    inventoryRestored, orderRestored, null);
            return orderRestored ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
                    : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
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
            return orderRestored ? PurchaseSalesOrderOutcome.rolledBackFailure(result, authoritative)
                    : PurchaseSalesOrderOutcome.changedFailure(result, authoritative);
        }
        return PurchaseSalesOrderOutcome.success(authoritative);
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

    private static boolean rollbackInventory(InventoryRollback rollback) {
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

    public record Context(UUID buyerId, MarketItemMaterializer materializer, TransactionalInventory inventory, Accounts accounts, Repository repository,
                          FailureReporter reporter) {
        public Context { Objects.requireNonNull(buyerId); Objects.requireNonNull(materializer); Objects.requireNonNull(inventory); Objects.requireNonNull(accounts); Objects.requireNonNull(repository); Objects.requireNonNull(reporter); }
    }
    public interface Accounts {
        BalanceTransferResult preview(UUID sellerId, int amount);
        BalanceTransferResult transfer(UUID sellerId, int amount);
    }
    public interface Repository {
        MarketOrder find(UUID tradeId);
        SalesOrderRemovalResult removeSalesTransactional(UUID tradeId);
    }
    public interface FailureReporter {
        void report(UUID tradeId, UUID buyerId, UUID sellerId, String stage, PurchaseSalesOrderResult result,
                    Boolean inventoryRollback, Boolean orderRestore, RuntimeException exception);
        static FailureReporter noop() { return (tradeId,buyerId,sellerId,stage,result,inventoryRollback,orderRestore,exception)->{}; }
    }
}
