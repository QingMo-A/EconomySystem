package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.market.CreateSalesOrderResult;
import com.mo.economy_system.common.market.CreateSalesOrderService;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataResponse;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NeoForge1211CreateSalesOrderHandler {
    private NeoForge1211CreateSalesOrderHandler() {}

    public static void handle(CreateSalesOrderMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Inventory inventory = player.getInventory();
            EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
            MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
            CreateSalesOrderResult result = CreateSalesOrderService.execute(message,
                    new CreateSalesOrderService.Context(new InventoryAdapter(inventory, player), new AccountAdapter(accounts, player),
                            new RepositoryAdapter(market), player.getUUID(), player.getName().getString(), UUID::randomUUID,
                            System::currentTimeMillis));
            player.sendSystemMessage(messageFor(result, message.totalPrice()));
            if (result == CreateSalesOrderResult.SUCCESS) {
                EconomySystem_NetworkManager.sendToClient(player, new Packet_MarketDataResponse(MarketManager.getMarketItems()));
            }
        });
    }

    private static Component messageFor(CreateSalesOrderResult result, int totalPrice) {
        return switch (result) {
            case SUCCESS -> Component.translatable(Util_MessageKeys.LIST_SUCCESSFULLY_MESSAGE_KEY);
            case INVALID_PRICE, INVALID_QUANTITY, TAX_OVERFLOW -> Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY);
            case INVALID_SLOT -> Component.translatable(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY);
            case EMPTY_SLOT -> Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY);
            case INSUFFICIENT_ITEMS -> Component.translatable(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY);
            case INSUFFICIENT_FUNDS -> Component.translatable(Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY,
                    Math.max(1L, ((long) totalPrice + 9L) / 10L));
            default -> Component.literal("创建销售订单失败: " + result.name());
        };
    }

    private record RepositoryAdapter(MarketSavedData data) implements CreateSalesOrderService.Repository {
        public boolean isFull() { return data.isFull(); }
        public boolean add(com.mo.economy_system.common.market.MarketOrder order) { return data.addOrder(order); }
    }
    private record AccountAdapter(EconomySavedData data, ServerPlayer player) implements CreateSalesOrderService.Account {
        public boolean canDebit(int amount) { return data.hasEnoughBalance(player.getUUID(), amount); }
        public boolean debit(int amount) { return data.minBalance(player.getUUID(), amount, "税费", "上架商品税"); }
        public boolean credit(int amount) { return data.addBalance(player.getUUID(), amount, "税费", "上架失败退税"); }
    }
    private record InventoryAdapter(Inventory inventory, ServerPlayer player) implements CreateSalesOrderService.Inventory {
        public int slotCount() { return inventory.items.size(); }
        public Object copySlot(int slot) { ItemStack stack = inventory.items.get(slot); return stack.isEmpty() ? null : stack.copy(); }
        public Object unitTemplate(Object value) { ItemStack stack = ((ItemStack) value).copy(); stack.setCount(1); return stack; }
        public ItemStackSnapshotResult<ItemStackSnapshot> capture(Object value) {
            return EconomyServices.platform().itemStacks().captureSnapshot((ItemStack) value, player.registryAccess());
        }
        public long countMatching(Object value) {
            ItemStack template = (ItemStack) value; long count = 0;
            for (ItemStack stack : inventory.items) if (!stack.isEmpty() && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) count += stack.getCount();
            return count;
        }
        public CreateSalesOrderService.Removal removeMatching(Object value, int quantity) {
            ItemStack template = (ItemStack) value;
            List<ItemStack> before = inventory.items.stream().map(ItemStack::copy).toList();
            int remaining = quantity;
            for (ItemStack stack : inventory.items) if (remaining > 0 && !stack.isEmpty() && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
                int removed = Math.min(remaining, stack.getCount()); stack.shrink(removed); remaining -= removed;
            }
            if (remaining != 0) return null;
            inventory.setChanged();
            return () -> { for (int i = 0; i < before.size(); i++) inventory.setItem(i, before.get(i).copy()); inventory.setChanged(); return true; };
        }
    }
}
