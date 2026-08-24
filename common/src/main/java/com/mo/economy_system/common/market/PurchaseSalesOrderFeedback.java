package com.mo.economy_system.common.market;

/** Stable player-facing translation keys; internal enum names are never exposed. */
public final class PurchaseSalesOrderFeedback {
    private PurchaseSalesOrderFeedback() {}

    public static String key(PurchaseSalesOrderResult result) {
        return switch (result) {
            case SUCCESS -> "message.market.purchase.success";
            case NOT_FOUND -> "message.market.purchase.not_found";
            case WRONG_ORDER_TYPE -> "message.market.purchase.wrong_type";
            case SELF_PURCHASE -> "message.market.purchase.self";
            case INSUFFICIENT_FUNDS -> "message.market.purchase.insufficient_funds";
            case SELLER_BALANCE_LIMIT -> "message.market.purchase.seller_balance_limit";
            case INVENTORY_FULL -> "message.market.purchase.inventory_full";
            case PARTIAL_FILL_UNSUPPORTED -> "message.market.purchase.whole_only";
            case ORDER_CHANGED -> "message.market.purchase.order_changed";
            case ORDER_REMOVE_FAILED -> "message.market.purchase.persist_failed";
            case ITEM_RESTORE_FAILED, INVALID_SNAPSHOT, INVENTORY_MUTATION_FAILED -> "message.market.purchase.item_failed";
            case PAYMENT_FAILED -> "message.market.purchase.payment_failed";
            case ROLLBACK_FAILED -> "message.market.purchase.rollback_failed";
            default -> "message.market.purchase.failed";
        };
    }
}
