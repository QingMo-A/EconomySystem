package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

final class Forge1201CreateSalesOrderHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Forge1201CreateSalesOrderHandler() {}

    static void handle(CreateSalesOrderMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context network = supplier.get();
        ServerPlayer player = network.getSender();
        if (player != null) {
            Inventory inventory = player.getInventory();
            EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
            MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
            CreateSalesOrderResult result = CreateSalesOrderService.execute(message, new CreateSalesOrderService.Context(
                    new InventoryAdapter(inventory, player), new AccountAdapter(accounts, player), new RepositoryAdapter(market),
                    player.getUUID(), player.getName().getString(), UUID::randomUUID, System::currentTimeMillis, reporter(player)));
            player.sendSystemMessage(messageFor(result, derivedTotalPrice(message)));
            if (result == CreateSalesOrderResult.SUCCESS) Forge1201MarketInvalidation.broadcast(player);
            if (CreateSalesOrderFeedback.internalFailure(result)) {
                LOGGER.error("Sales order creation failed player={} name={} result={}",
                        player.getUUID(), player.getName().getString(), result);
            }
        }
        network.setPacketHandled(true);
    }

    static Component messageFor(CreateSalesOrderResult result, int totalPrice) {
        String key = CreateSalesOrderFeedback.messageKey(result);
        return result == CreateSalesOrderResult.INSUFFICIENT_FUNDS
                ? Component.translatable(key, CreateSalesOrderService.taxFor(totalPrice)) : Component.translatable(key);
    }

    private static int derivedTotalPrice(CreateSalesOrderMessage message) {
        try { return Math.multiplyExact(message.quantity(), message.unitPrice()); }
        catch (ArithmeticException ignored) { return 0; }
    }

    private static CreateSalesOrderService.FailureReporter reporter(ServerPlayer player) {
        return (tradeId, stage, result, cause, compensation) -> {
            LOGGER.error("Sales order transaction failure player={} name={} tradeId={} stage={} result={} taxRestored={} inventoryRestored={}",
                    player.getUUID(), player.getName().getString(), tradeId, stage, result,
                    compensation.taxRestored(), compensation.inventoryRestored(), cause);
            if (compensation.taxError() != null) LOGGER.error("Sales order tax compensation threw tradeId={}", tradeId, compensation.taxError());
            if (compensation.inventoryError() != null) LOGGER.error("Sales order inventory compensation threw tradeId={}", tradeId, compensation.inventoryError());
        };
    }
    private record RepositoryAdapter(MarketSavedData data) implements CreateSalesOrderService.Repository {
        public boolean isFull() { return data.isFull(); }
        public boolean add(MarketOrder order) { return data.addOrder(order); }
    }
    private record AccountAdapter(EconomySavedData data, ServerPlayer player) implements CreateSalesOrderService.Account {
        public boolean canDebit(int amount) { return data.hasEnoughBalance(player.getUUID(), amount); }
        public BalanceMutationResult debitExact(int amount) { return data.debitExact(player.getUUID(), amount, "税费", "上架商品税"); }
        public BalanceMutationResult creditExact(int amount) { return data.creditExact(player.getUUID(), amount, "税费", "上架失败退税"); }
    }
    private record InventoryAdapter(Inventory inventory, ServerPlayer player) implements CreateSalesOrderService.Inventory {
        public int slotCount() { return inventory.items.size(); }
        public Object copySlot(int slot) { ItemStack stack = inventory.items.get(slot); return stack.isEmpty() ? null : stack.copy(); }
        public Object unitTemplate(Object value) { ItemStack stack = ((ItemStack) value).copy(); stack.setCount(1); return stack; }
        public ItemStackSnapshotResult<ItemStackSnapshot> capture(Object value) {
            return Forge1201Platform.nativeItemStacks().captureSnapshot((ItemStack) value, player.serverLevel().registryAccess());
        }
        public long countMatching(Object value) { ItemStack template = (ItemStack) value; long total = 0;
            for (ItemStack stack : inventory.items) if (!stack.isEmpty() && Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) total += stack.getCount(); return total; }
        public CreateSalesOrderService.RemovalResult removeMatching(Object value, int quantity) {
            ItemStack template = (ItemStack) value; List<ItemStack> before = inventory.items.stream().map(ItemStack::copy).toList();
            try {
                int remaining = quantity;
                for (ItemStack stack : inventory.items) if (remaining > 0 && !stack.isEmpty() && Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) {
                    int removed = Math.min(remaining, stack.getCount()); stack.shrink(removed); remaining -= removed;
                }
                if (remaining != 0) return CreateSalesOrderService.RemovalResult.failure(restore(before));
                inventory.setChanged();
                return CreateSalesOrderService.RemovalResult.success(() -> restore(before));
            } catch (RuntimeException exception) {
                boolean restored = restore(before);
                LOGGER.error("Inventory removal failed and compensation was attempted player={} restored={}", player.getUUID(), restored, exception);
                return CreateSalesOrderService.RemovalResult.failure(restored);
            }
        }
        private boolean restore(List<ItemStack> before) {
            boolean restored = true;
            for (int i = 0; i < before.size(); i++) try { inventory.setItem(i, before.get(i).copy()); }
            catch (RuntimeException exception) { restored = false; LOGGER.error("Failed to restore inventory slot {} for {}", i, player.getUUID(), exception); }
            inventory.setChanged(); return restored;
        }
    }
}
