package com.mo.economy_system.common.market;

public enum MarketOrderType {
    SALES("sales_order"),
    DEMAND("demand_order");

    private final String id;

    MarketOrderType(String id) { this.id = id; }
    public String id() { return id; }

    public static MarketOrderType fromPersistentId(String id) {
        return switch (id) {
            case "sales_order", "com.mo.economy_system.core.economy_system.market.SalesOrder" -> SALES;
            case "demand_order", "com.mo.economy_system.core.economy_system.market.DemandOrder" -> DEMAND;
            default -> throw new IllegalArgumentException("Unknown market order type: " + id);
        };
    }
}
