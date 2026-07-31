package com.mo.economy_system.platform.shop;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

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

    ItemStack createItemStack(ShopItemSnapshot item, RegistryAccess registryAccess);

    /** Returns whether demand/stock statistics were persisted successfully. */
    boolean recordPurchase(String shopItemId, int quantity);
}
