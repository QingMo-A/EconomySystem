package com.mo.economy_system.target.neoforge1211.shop;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.platform.shop.EconomyShopCatalogBridge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Exposes the authoritative NeoForge 1.21.1 shop model to common code. */
public final class NeoForge1211ShopCatalogBridge implements EconomyShopCatalogBridge {
    @Override
    public List<ShopItemSnapshot> snapshot() {
        return EconomySystem.SHOP_MANAGER.getItems().stream()
                .map(ShopItem::toBridgeSnapshot)
                .toList();
    }

    @Override
    public ShopItemSnapshot findByShopItemId(String shopItemId) {
        ShopItem item = EconomySystem.SHOP_MANAGER.findByShopItemId(shopItemId);
        return item == null ? null : item.toBridgeSnapshot();
    }

    public ItemStack createItemStack(ShopItemSnapshot item, RegistryAccess registryAccess) {
        return ShopItem.fromBridgeSnapshot(item).getItemStack(registryAccess);
    }

    public ShopItemSnapshot addItemFromStack(
            ItemStack stack, int basePrice, String description, RegistryAccess registryAccess) {
        if (stack == null || stack.isEmpty() || basePrice <= 0) {
            throw new IllegalArgumentException("invalid shop item or base price");
        }
        return EconomySystem.SHOP_MANAGER
                .addItemFromStack(stack, basePrice, description == null ? "" : description, registryAccess)
                .toBridgeSnapshot();
    }

    @Override
    public boolean recordPurchase(String shopItemId, int quantity) {
        ShopItem item = EconomySystem.SHOP_MANAGER.findByShopItemId(shopItemId);
        if (item == null || quantity <= 0) {
            return false;
        }
        EconomySystem.SHOP_MANAGER.recordPurchase(item, quantity);
        return true;
    }

    @Override
    public boolean refreshPrices() {
        EconomySystem.SHOP_MANAGER.adjustPrices();
        return true;
    }
}
