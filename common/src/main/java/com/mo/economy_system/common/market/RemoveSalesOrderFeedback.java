package com.mo.economy_system.common.market;

public final class RemoveSalesOrderFeedback {
    private RemoveSalesOrderFeedback() {}
    public static String key(RemoveSalesOrderResult result) {
        return "message.market.remove_sales." + result.name().toLowerCase(java.util.Locale.ROOT);
    }
}
