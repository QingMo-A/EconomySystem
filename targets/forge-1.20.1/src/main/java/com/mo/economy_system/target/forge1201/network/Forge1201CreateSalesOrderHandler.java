package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.CreateSalesOrderService;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

final class Forge1201CreateSalesOrderHandler {
    private Forge1201CreateSalesOrderHandler() {}
    static void handle(CreateSalesOrderMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context network = supplier.get();
        ServerPlayer player = network.getSender();
        if (player != null) {
            Inventory inventory = player.getInventory();
            EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
            MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
            CreateSalesOrderService.execute(message, new CreateSalesOrderService.Context(
                    new InventoryAdapter(inventory, player), new AccountAdapter(accounts, player), new RepositoryAdapter(market),
                    player.getUUID(), player.getName().getString(), UUID::randomUUID, System::currentTimeMillis));
        }
        network.setPacketHandled(true);
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
            return EconomyServices.platform().itemStacks().captureSnapshot((ItemStack) value, player.serverLevel().registryAccess());
        }
        public long countMatching(Object value) { ItemStack template = (ItemStack) value; long total = 0;
            for (ItemStack stack : inventory.items) if (!stack.isEmpty() && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) total += stack.getCount(); return total; }
        public CreateSalesOrderService.Removal removeMatching(Object value, int quantity) {
            ItemStack template = (ItemStack) value; List<ItemStack> before = inventory.items.stream().map(ItemStack::copy).toList(); int remaining = quantity;
            for (ItemStack stack : inventory.items) if (remaining > 0 && !stack.isEmpty() && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
                int removed = Math.min(remaining, stack.getCount()); stack.shrink(removed); remaining -= removed; }
            if (remaining != 0) return null; inventory.setChanged();
            return () -> { for (int i = 0; i < before.size(); i++) inventory.setItem(i, before.get(i).copy()); inventory.setChanged(); return true; };
        }
    }
}
