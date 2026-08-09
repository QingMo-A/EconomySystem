package com.mo.economy_system.common.market;

/** Stable user-facing translation mapping shared by both loaders. */
public final class CreateSalesOrderFeedback {
    public static final String SUCCESS = "message.list.list_successfully";
    public static final String INVALID_SLOT = "message.list.list_unmatched_item";
    public static final String EMPTY_SLOT = "message.list.no_item_in_hand";
    public static final String INVALID_QUANTITY = "message.list.invalid_quantity";
    public static final String INVALID_PRICE = "message.list.invalid_price";
    public static final String INSUFFICIENT_ITEMS = "message.list.list_insufficient_item";
    public static final String UNSUPPORTED_ITEM = "message.list.unsupported_item_data";
    public static final String INSUFFICIENT_FUNDS = "message.list.list_item_tax_payment_failed";
    public static final String MARKET_FULL = "message.list.market_full";
    public static final String FAILED = "message.list.create_failed";
    public static final String ROLLBACK_FAILED = "message.list.rollback_failed";

    private CreateSalesOrderFeedback() {}

    public static String messageKey(CreateSalesOrderResult result) {
        return switch (result) {
            case SUCCESS -> SUCCESS;
            case INVALID_SLOT -> INVALID_SLOT;
            case EMPTY_SLOT -> EMPTY_SLOT;
            case INVALID_QUANTITY -> INVALID_QUANTITY;
            case INVALID_PRICE, TAX_OVERFLOW -> INVALID_PRICE;
            case INSUFFICIENT_ITEMS -> INSUFFICIENT_ITEMS;
            case SNAPSHOT_REJECTED -> UNSUPPORTED_ITEM;
            case INSUFFICIENT_FUNDS -> INSUFFICIENT_FUNDS;
            case REPOSITORY_FULL -> MARKET_FULL;
            case ROLLBACK_FAILED, STATE_UNKNOWN -> ROLLBACK_FAILED;
            default -> FAILED;
        };
    }

    public static boolean internalFailure(CreateSalesOrderResult result) {
        return switch (result) {
            case INVENTORY_MUTATION_FAILED, TAX_MUTATION_FAILED, ORDER_PERSIST_FAILED, ROLLBACK_FAILED,
                    STATE_UNKNOWN, INVALID_CONTEXT -> true;
            default -> false;
        };
    }
}
