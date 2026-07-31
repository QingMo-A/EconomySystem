package com.mo.economy_system.common.economy;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

/** Server-authoritative system-shop purchase transaction. */
public final class ShopPurchaseService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String INVALID_COUNT = "message.shop_buy.invalid_count";
    private static final String INVALID_ITEM = "message.shop.invalid_item";
    private static final String PURCHASE_FAILED = "message.shop.buy_failed";
    private static final String INVENTORY_FULL = "message.shop.buy_failed_inventory_full";
    private static final String PURCHASE_ERROR = "message.shop.buy_error";
    private static final String PURCHASE_SUCCESS = "message.shop.buy_successfully";

    private ShopPurchaseService() {
    }

    public static ShopPurchaseResult execute(ServerPlayer player, ShopBuyItemMessage message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");

        if (message.quantity() <= 0 || message.quantity() > ShopPurchaseRules.MAX_QUANTITY) {
            player.sendSystemMessage(Component.translatable(INVALID_COUNT));
            return ShopPurchaseResult.INVALID_QUANTITY;
        }

        ShopItemSnapshot shopItem = EconomyServices.platform()
                .shopCatalog()
                .findByShopItemId(message.shopItemId());
        if (shopItem == null) {
            player.sendSystemMessage(Component.translatable(INVALID_ITEM));
            return ShopPurchaseResult.INVALID_ITEM;
        }

        int totalPrice = ShopPurchaseRules.checkedTotalPrice(
                shopItem.currentPrice(),
                message.quantity()
        );
        if (totalPrice < 0) {
            player.sendSystemMessage(Component.translatable(PURCHASE_FAILED));
            return ShopPurchaseResult.INVALID_PRICE;
        }

        ItemStack template = EconomyServices.platform().shopCatalog().createItemStack(
                shopItem,
                player.serverLevel().registryAccess()
        );
        if (template == null || template.isEmpty()) {
            player.sendSystemMessage(Component.translatable(INVALID_ITEM));
            return ShopPurchaseResult.INVALID_ITEM;
        }
        template = template.copy();
        template.setCount(1);

        Inventory inventory = player.getInventory();
        if (!hasCapacity(inventory, template, message.quantity())) {
            player.sendSystemMessage(Component.translatable(INVENTORY_FULL));
            return ShopPurchaseResult.INVENTORY_FULL;
        }

        EconomySavedData economyData = EconomySavedData.getInstance(player.serverLevel());
        if (!economyData.minBalance(
                player.getUUID(),
                totalPrice,
                "市场",
                "系统商店购买 " + template.getHoverName().getString() + " x" + message.quantity()
        )) {
            player.sendSystemMessage(Component.translatable(PURCHASE_FAILED));
            return ShopPurchaseResult.INSUFFICIENT_FUNDS;
        }

        List<ItemStack> inventoryBefore = inventory.items.stream()
                .map(ItemStack::copy)
                .toList();
        boolean delivered;
        try {
            delivered = addItems(inventory, template, message.quantity());
        } catch (RuntimeException exception) {
            LOGGER.error("System-shop delivery failed for {}", player.getGameProfile().getName(), exception);
            delivered = false;
        }

        if (!delivered) {
            restoreInventory(inventory, inventoryBefore);
            boolean refunded = economyData.addBalance(
                    player.getUUID(),
                    totalPrice,
                    "系统",
                    "系统商店购买失败退款"
            );
            if (!refunded) {
                LOGGER.error(
                        "Failed to refund {} coins to {} after shop delivery rollback",
                        totalPrice,
                        player.getGameProfile().getName()
                );
            }
            player.sendSystemMessage(Component.translatable(PURCHASE_ERROR));
            return ShopPurchaseResult.DELIVERY_FAILED;
        }

        // Pricing statistics are best-effort after delivery. A config write
        // failure must never refund the money while leaving delivered items.
        if (!EconomyServices.platform().shopCatalog().recordPurchase(
                shopItem.shopItemId(),
                message.quantity()
        )) {
            LOGGER.warn("Could not persist purchase statistics for shop entry {}", shopItem.shopItemId());
        }
        player.sendSystemMessage(Component.translatable(
                PURCHASE_SUCCESS,
                totalPrice,
                message.quantity(),
                template.getHoverName().getString()
        ));
        return ShopPurchaseResult.SUCCESS;
    }

    private static boolean hasCapacity(Inventory inventory, ItemStack template, int quantity) {
        long capacity = 0L;
        int maxStackSize = template.getMaxStackSize();
        for (ItemStack stack : inventory.items) {
            if (stack.isEmpty()) {
                capacity += maxStackSize;
            } else if (EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
                capacity += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
            if (capacity >= quantity) {
                return true;
            }
        }
        return false;
    }

    private static boolean addItems(Inventory inventory, ItemStack template, int quantity) {
        int remaining = quantity;
        for (ItemStack stack : inventory.items) {
            if (EconomyServices.platform().itemStacks().sameItemAndData(stack, template)
                    && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(stack.getMaxStackSize() - stack.getCount(), remaining);
                stack.grow(moved);
                remaining -= moved;
                if (remaining == 0) {
                    inventory.setChanged();
                    return true;
                }
            }
        }

        int maxStackSize = template.getMaxStackSize();
        for (int index = 0; index < inventory.items.size() && remaining > 0; index++) {
            if (!inventory.items.get(index).isEmpty()) {
                continue;
            }
            ItemStack inserted = template.copy();
            int count = Math.min(maxStackSize, remaining);
            inserted.setCount(count);
            inventory.items.set(index, inserted);
            remaining -= count;
        }
        inventory.setChanged();
        return remaining == 0;
    }

    private static void restoreInventory(Inventory inventory, List<ItemStack> snapshot) {
        for (int index = 0; index < inventory.items.size(); index++) {
            inventory.items.set(index, snapshot.get(index).copy());
        }
        inventory.setChanged();
    }
}
