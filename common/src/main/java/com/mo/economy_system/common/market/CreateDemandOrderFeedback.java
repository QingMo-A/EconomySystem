package com.mo.economy_system.common.market;

/** Stable user-facing result mapping shared by both loaders. */
public final class CreateDemandOrderFeedback {
    public static final String SUCCESS = "message.request.create_success";
    public static final String INVALID_ITEM_ID = "message.request.invalid_item_id";
    public static final String ITEM_NOT_FOUND = "message.request.item_not_found";
    public static final String INVALID_QUANTITY = "message.request.invalid_quantity";
    public static final String QUANTITY_EXCEEDS_LIMIT = "message.request.quantity_exceeds_limit";
    public static final String INVALID_PRICE = "message.request.invalid_price";
    public static final String INSUFFICIENT_FUNDS = "message.request.insufficient_funds";
    public static final String MARKET_FULL = "message.request.market_full";
    public static final String UNSUPPORTED_ITEM = "message.request.unsupported_item";
    public static final String FAILED = "message.request.create_failed";
    public static final String REFUND_FAILED = "message.request.refund_failed";

    private CreateDemandOrderFeedback() {}

    public static String messageKey(CreateDemandOrderResult result) {
        return switch (result) {
            case SUCCESS -> SUCCESS;
            case INVALID_ITEM_ID -> INVALID_ITEM_ID;
            case ITEM_NOT_FOUND -> ITEM_NOT_FOUND;
            case INVALID_QUANTITY -> INVALID_QUANTITY;
            case QUANTITY_EXCEEDS_LIMIT -> QUANTITY_EXCEEDS_LIMIT;
            case INVALID_PRICE -> INVALID_PRICE;
            case INSUFFICIENT_FUNDS -> INSUFFICIENT_FUNDS;
            case REPOSITORY_FULL -> MARKET_FULL;
            case SNAPSHOT_REJECTED -> UNSUPPORTED_ITEM;
            case REFUND_FAILED, STATE_UNKNOWN -> REFUND_FAILED;
            default -> FAILED;
        };
    }

    public static boolean internalFailure(CreateDemandOrderResult result) {
        return switch (result) {
            case INVALID_CONTEXT, ID_GENERATION_FAILED, TIME_OVERFLOW, PAYMENT_FAILED,
                    ORDER_PERSIST_FAILED, REFUND_FAILED, STATE_UNKNOWN -> true;
            default -> false;
        };
    }
}
