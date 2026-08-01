package com.mo.economy_system.common.market;

public final class CancelDemandOrderFeedback {
    private CancelDemandOrderFeedback() {}
    public static String messageKey(CancelDemandOrderResult result) {
        return switch (result) {
            case SUCCESS -> "message.request.cancel_success";
            case NOT_FOUND, WRONG_ORDER_TYPE -> "message.request.cancel_not_found";
            case NOT_OWNER -> "message.request.cancel_not_owner";
            case ALREADY_DELIVERED -> "message.request.cancel_delivered";
            case OWNER_BALANCE_LIMIT -> "message.request.cancel_balance_limit";
            case ROLLBACK_FAILED -> "message.request.cancel_rollback_failed";
            case ORDER_CHANGED -> "message.request.cancel_failed";
            default -> "message.request.cancel_failed";
        };
    }
}
