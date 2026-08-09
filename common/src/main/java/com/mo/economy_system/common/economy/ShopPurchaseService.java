package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.market.InventoryInsertionResult;
import com.mo.economy_system.common.market.TransactionalInventory;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral, server-authoritative system-shop purchase transaction. */
public final class ShopPurchaseService {
    private static final String INVALID_COUNT = "message.shop_buy.invalid_count";
    private static final String INVALID_ITEM = "message.shop.invalid_item";
    private static final String PURCHASE_FAILED = "message.shop.buy_failed";
    private static final String INVENTORY_FULL = "message.shop.buy_failed_inventory_full";
    private static final String PURCHASE_ERROR = "message.shop.buy_error";
    private static final String PURCHASE_SUCCESS = "message.shop.buy_successfully";

    private ShopPurchaseService() {
    }

    public static ShopPurchaseResult execute(ShopBuyItemMessage message, Context context) {
        if (message == null || context == null) {
            return ShopPurchaseResult.INVALID_ITEM;
        }
        UUID playerId = context.playerId();
        if (message.quantity() <= 0 || message.quantity() > ShopPurchaseRules.MAX_QUANTITY) {
            send(context, playerId, INVALID_COUNT);
            return ShopPurchaseResult.INVALID_QUANTITY;
        }

        ShopItemSnapshot shopItem;
        try {
            shopItem = context.catalog().find(message.shopItemId());
        } catch (RuntimeException failure) {
            report(context, "catalog-lookup", failure);
            send(context, playerId, INVALID_ITEM);
            return ShopPurchaseResult.INVALID_ITEM;
        }
        if (shopItem == null) {
            send(context, playerId, INVALID_ITEM);
            return ShopPurchaseResult.INVALID_ITEM;
        }

        int totalPrice = ShopPurchaseRules.checkedTotalPrice(
                shopItem.currentPrice(), message.quantity());
        if (totalPrice < 0) {
            send(context, playerId, PURCHASE_FAILED);
            return ShopPurchaseResult.INVALID_PRICE;
        }

        MaterializedItem materialized;
        try {
            materialized = context.catalog().materialize(shopItem);
        } catch (RuntimeException failure) {
            report(context, "item-materialize", failure);
            materialized = null;
        }
        if (materialized == null || materialized.value() == null) {
            send(context, playerId, INVALID_ITEM);
            return ShopPurchaseResult.INVALID_ITEM;
        }

        TransactionalInventory inventory = context.inventory();
        if (!inventory.ownerId().equals(playerId)) {
            report(context, "inventory-owner-mismatch", null);
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.DELIVERY_FAILED;
        }
        try {
            if (!inventory.canAccept(materialized.value(), message.quantity())) {
                send(context, playerId, INVENTORY_FULL);
                return ShopPurchaseResult.INVENTORY_FULL;
            }
        } catch (RuntimeException failure) {
            report(context, "inventory-capacity", failure);
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.DELIVERY_FAILED;
        }

        BalanceMutationResult debit;
        try {
            debit = context.accounts().debit(
                    playerId,
                    totalPrice,
                    "市场",
                    "系统商店购买 " + materialized.displayName() + " x" + message.quantity());
        } catch (RuntimeException failure) {
            report(context, "payment", failure);
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.PAYMENT_FAILED;
        }
        if (debit == BalanceMutationResult.INSUFFICIENT_FUNDS) {
            send(context, playerId, PURCHASE_FAILED);
            return ShopPurchaseResult.INSUFFICIENT_FUNDS;
        }
        if (debit != BalanceMutationResult.SUCCESS) {
            report(context, "payment-result",
                    new IllegalStateException("shop debit result: " + debit));
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.PAYMENT_FAILED;
        }

        InventoryInsertionResult insertion;
        try {
            insertion = inventory.insert(materialized.value(), message.quantity());
        } catch (RuntimeException failure) {
            report(context, "inventory-insert-state-unknown", failure);
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.STATE_UNKNOWN;
        }
        if (insertion == null) {
            report(context, "inventory-insert-state-unknown",
                    new IllegalStateException("inventory returned null insertion result"));
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.STATE_UNKNOWN;
        }
        if (!insertion.succeeded()) {
            if (!insertion.failureRestored()) {
                report(context, "inventory-rollback",
                        new IllegalStateException("shop inventory rollback was not proven"));
                send(context, playerId, PURCHASE_ERROR);
                return ShopPurchaseResult.ROLLBACK_FAILED;
            }
            BalanceMutationResult refund;
            try {
                refund = context.accounts().credit(
                        playerId, totalPrice, "系统", "系统商店购买失败退款");
            } catch (RuntimeException failure) {
                report(context, "refund", failure);
                send(context, playerId, PURCHASE_ERROR);
                return ShopPurchaseResult.REFUND_FAILED;
            }
            if (refund != BalanceMutationResult.SUCCESS) {
                report(context, "refund-result",
                        new IllegalStateException("shop refund result: " + refund));
                send(context, playerId, PURCHASE_ERROR);
                return ShopPurchaseResult.REFUND_FAILED;
            }
            report(context, "delivery-failed", null);
            send(context, playerId, PURCHASE_ERROR);
            return ShopPurchaseResult.DELIVERY_FAILED;
        }

        // Purchase statistics are best effort after delivery; delivered items must not be refunded
        // merely because a pricing-config write failed.
        try {
            if (!context.catalog().recordPurchase(shopItem.shopItemId(), message.quantity())) {
                report(context, "purchase-statistics", null);
            }
        } catch (RuntimeException failure) {
            report(context, "purchase-statistics", failure);
        }
        send(
                context,
                playerId,
                PURCHASE_SUCCESS,
                totalPrice,
                message.quantity(),
                materialized.displayName());
        return ShopPurchaseResult.SUCCESS;
    }

    private static void send(
            Context context, UUID playerId, String translationKey, Object... arguments) {
        try {
            context.feedback().send(playerId, translationKey, arguments);
        } catch (RuntimeException failure) {
            report(context, "feedback", failure);
        }
    }

    private static void report(Context context, String stage, RuntimeException failure) {
        try {
            context.feedback().report(stage, failure);
        } catch (RuntimeException ignored) {
            // Diagnostics must not change the authoritative transaction result.
        }
    }

    public record Context(
            UUID playerId,
            Catalog catalog,
            TransactionalInventory inventory,
            Accounts accounts,
            Feedback feedback) {
        public Context {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(catalog, "catalog");
            Objects.requireNonNull(inventory, "inventory");
            Objects.requireNonNull(accounts, "accounts");
            Objects.requireNonNull(feedback, "feedback");
        }
    }

    public interface Catalog {
        ShopItemSnapshot find(String shopItemId);

        MaterializedItem materialize(ShopItemSnapshot item);

        boolean recordPurchase(String shopItemId, int quantity);
    }

    public interface Accounts {
        BalanceMutationResult debit(UUID playerId, int amount, String category, String reason);

        BalanceMutationResult credit(UUID playerId, int amount, String category, String reason);
    }

    public interface Feedback {
        void send(UUID playerId, String translationKey, Object... arguments);

        default void report(String stage, RuntimeException failure) {
        }
    }

    public record MaterializedItem(Object value, String displayName) {
        public MaterializedItem {
            Objects.requireNonNull(displayName, "displayName");
        }
    }
}
