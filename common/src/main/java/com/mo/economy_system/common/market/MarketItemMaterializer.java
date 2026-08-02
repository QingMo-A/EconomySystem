package com.mo.economy_system.common.market;

@FunctionalInterface
public interface MarketItemMaterializer {
    Object restore(MarketOrder order);
}
