package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;

import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Validates protocol 9 and atomically freezes funds before adding a demand order. */
public final class CreateDemandOrderService {
    private CreateDemandOrderService() {}

    public static CreateDemandOrderResult execute(CreateDemandOrderMessage message, Context context) {
        if (message == null || context == null) return CreateDemandOrderResult.INVALID_CONTEXT;
        if (message.itemId() == null || message.itemId().isBlank()
                || message.itemId().length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH)
            return CreateDemandOrderResult.INVALID_ITEM_ID;
        if (message.quantity() <= 0) return CreateDemandOrderResult.INVALID_QUANTITY;
        if (message.totalPrice() <= 0) return CreateDemandOrderResult.INVALID_PRICE;

        DemandItemResolveResult resolved;
        try { resolved = context.items().resolve(message.itemId()); }
        catch (RuntimeException exception) {
            report(context, null, "item-resolve", CreateDemandOrderResult.SNAPSHOT_REJECTED, exception, false);
            return CreateDemandOrderResult.SNAPSHOT_REJECTED;
        }
        if (resolved == null || !resolved.isSuccess()) return mapResolveFailure(resolved);
        ResolvedDemandItem item = resolved.value();
        if (item.template().count() != 1) return CreateDemandOrderResult.SNAPSHOT_REJECTED;
        if (message.quantity() > item.maxQuantity()) return CreateDemandOrderResult.QUANTITY_EXCEEDS_LIMIT;
        try {
            if (context.repository().isFull()) return CreateDemandOrderResult.REPOSITORY_FULL;
            if (!context.account().canDebit(message.totalPrice())) return CreateDemandOrderResult.INSUFFICIENT_FUNDS;
        } catch (RuntimeException exception) {
            report(context, null, "precondition", CreateDemandOrderResult.PAYMENT_FAILED, exception, false);
            return CreateDemandOrderResult.PAYMENT_FAILED;
        }

        long now = context.clock().getAsLong();
        long expiration;
        try { expiration = Math.addExact(now, MarketOrder.EXPIRATION_DURATION_MILLIS); }
        catch (ArithmeticException exception) { return CreateDemandOrderResult.TIME_OVERFLOW; }
        UUID tradeId = context.ids().get();
        if (tradeId == null) return CreateDemandOrderResult.ID_GENERATION_FAILED;
        MarketOrder order = new MarketOrder(MarketOrderType.DEMAND, tradeId, item.template(), message.quantity(),
                message.totalPrice(), context.buyerName(), context.buyerId(), now, expiration, false);

        boolean debited;
        RuntimeException debitError = null;
        try { debited = context.account().debit(message.totalPrice()); }
        catch (RuntimeException exception) { debited = false; debitError = exception; }
        if (!debited) {
            report(context, tradeId, "payment-debit", CreateDemandOrderResult.PAYMENT_FAILED, debitError, false);
            return CreateDemandOrderResult.PAYMENT_FAILED;
        }

        boolean added;
        RuntimeException repositoryError = null;
        try { added = context.repository().add(order); }
        catch (RuntimeException exception) { added = false; repositoryError = exception; }
        if (added) return CreateDemandOrderResult.SUCCESS;

        boolean refunded;
        RuntimeException refundError = null;
        try { refunded = context.account().credit(message.totalPrice()); }
        catch (RuntimeException exception) { refunded = false; refundError = exception; }
        CreateDemandOrderResult result = refunded ? CreateDemandOrderResult.ORDER_PERSIST_FAILED
                : CreateDemandOrderResult.REFUND_FAILED;
        report(context, tradeId, "repository-add", result, repositoryError, refunded);
        if (refundError != null) report(context, tradeId, "payment-refund", result, refundError, false);
        return result;
    }

    private static CreateDemandOrderResult mapResolveFailure(DemandItemResolveResult result) {
        if (result == null || result.error() == null) return CreateDemandOrderResult.SNAPSHOT_REJECTED;
        return switch (result.error()) {
            case INVALID_ITEM_ID -> CreateDemandOrderResult.INVALID_ITEM_ID;
            case ITEM_NOT_FOUND -> CreateDemandOrderResult.ITEM_NOT_FOUND;
            case SNAPSHOT_REJECTED -> CreateDemandOrderResult.SNAPSHOT_REJECTED;
        };
    }

    private static void report(Context context, UUID tradeId, String stage, CreateDemandOrderResult result,
                               RuntimeException cause, boolean refunded) {
        try { context.reporter().report(tradeId, stage, result, cause, refunded); }
        catch (RuntimeException ignored) { }
    }

    public record Context(ItemResolver items, Account account, Repository repository, UUID buyerId,
                          String buyerName, Supplier<UUID> ids, LongSupplier clock, FailureReporter reporter) {
        public Context {
            Objects.requireNonNull(items); Objects.requireNonNull(account); Objects.requireNonNull(repository);
            Objects.requireNonNull(buyerId); Objects.requireNonNull(buyerName); Objects.requireNonNull(ids);
            Objects.requireNonNull(clock); Objects.requireNonNull(reporter);
        }
    }
    public interface ItemResolver { DemandItemResolveResult resolve(String itemId); }
    public interface Account { boolean canDebit(int amount); boolean debit(int amount); boolean credit(int amount); }
    public interface Repository {
        boolean isFull();
        /** Returning false or throwing must not leave the supplied order in the repository. */
        boolean add(MarketOrder order);
    }
    public interface FailureReporter {
        void report(UUID tradeId, String stage, CreateDemandOrderResult result, RuntimeException cause, boolean refunded);
        static FailureReporter noop() { return (tradeId, stage, result, cause, refunded) -> {}; }
    }
}
