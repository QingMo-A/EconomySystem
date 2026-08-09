package com.mo.economy_system.platform.shop;

import com.mo.economy_system.common.network.ShopItemSnapshot;

import java.util.List;

/** Supplies the loader-specific shop model as stable common snapshots. */
public interface EconomyShopCatalogBridge {
    List<ShopItemSnapshot> snapshot();

    default ShopItemSnapshot findByShopItemId(String shopItemId) {
        if (shopItemId == null || shopItemId.isBlank()) {
            return null;
        }
        return snapshot().stream()
                .filter(item -> shopItemId.equals(item.shopItemId()))
                .findFirst()
                .orElse(null);
    }

    /** Returns whether demand/stock statistics were persisted successfully. */
    boolean recordPurchase(String shopItemId, int quantity);

    /** Applies and persists one common dynamic-pricing cycle. */
    boolean refreshPrices();
}
