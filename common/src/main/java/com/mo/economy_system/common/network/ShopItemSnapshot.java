package com.mo.economy_system.common.network;

import java.util.Objects;

/**
 * Loader-neutral wire representation of one system-shop entry.
 *
 * <p>The item payload remains an opaque string here. Each Minecraft target is
 * responsible for interpreting its own item-component/NBT representation.</p>
 */
public record ShopItemSnapshot(
        String shopItemId,
        String itemId,
        int basePrice,
        int currentPrice,
        int lastPrice,
        String description,
        double fluctuationFactor,
        String nbt,
        String itemData,
        int recentDemand,
        int virtualStock,
        int maxVirtualStock
) {
    public ShopItemSnapshot {
        shopItemId = Objects.requireNonNull(shopItemId, "shopItemId");
        itemId = Objects.requireNonNull(itemId, "itemId");
        description = Objects.requireNonNullElse(description, "");
        nbt = Objects.requireNonNullElse(nbt, "");
        itemData = Objects.requireNonNullElse(itemData, "");
    }
}
