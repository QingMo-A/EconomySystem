package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class Forge1201TransactionalInventoryAdapter
    implements MarketItemMaterializer, TransactionalInventory, TransactionalInventoryRemoval {
  private static final Logger LOGGER = LogUtils.getLogger();
  private final ServerPlayer player;

  public Forge1201TransactionalInventoryAdapter(ServerPlayer player) {
    this.player = Objects.requireNonNull(player);
  }

  public UUID ownerId() {
    return player.getUUID();
  }

  public Object restore(MarketOrder order) {
    return Forge1201Platform.nativeItemStacks()
        .restoreSnapshot(order.item(), player.serverLevel().registryAccess())
        .orElseThrow();
  }

  public boolean canAccept(Object value, int quantity) {
    return capacity((ItemStack) value) >= quantity;
  }

  private int capacity(ItemStack template) {
    long result = 0;
    for (ItemStack stack : player.getInventory().items) {
      if (stack.isEmpty()) result += template.getMaxStackSize();
      else if (Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template))
        result += Math.max(0, stack.getMaxStackSize() - stack.getCount());
      if (result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
    }
    return (int) result;
  }

  public InventoryInsertionResult insert(Object value, int quantity) {
    ItemStack template = ((ItemStack) value).copy();
    template.setCount(1);
    Inventory inventory = player.getInventory();
    List<ItemStack> before = inventory.items.stream().map(ItemStack::copy).toList();
    try {
      int remaining = quantity;
      for (ItemStack stack : inventory.items)
        if (remaining > 0
            && !stack.isEmpty()
            && Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) {
          int add = Math.min(remaining, Math.max(0, stack.getMaxStackSize() - stack.getCount()));
          if (add > 0) {
            stack.grow(add);
            remaining -= add;
          }
        }
      for (int index = 0; index < inventory.items.size() && remaining > 0; index++)
        if (inventory.items.get(index).isEmpty()) {
          ItemStack inserted = template.copy();
          int add = Math.min(remaining, inserted.getMaxStackSize());
          inserted.setCount(add);
          inventory.setItem(index, inserted);
          remaining -= add;
        }
      if (remaining != 0) return InventoryInsertionResult.failure(restoreSlots(before));
      inventory.setChanged();
      return InventoryInsertionResult.success(() -> restoreSlots(before));
    } catch (RuntimeException exception) {
      return InventoryInsertionResult.failure(restoreSlots(before));
    }
  }

  private boolean restoreSlots(List<ItemStack> before) {
    boolean restored = true;
    for (int index = 0; index < before.size(); index++)
      try {
        player.getInventory().setItem(index, before.get(index).copy());
      } catch (RuntimeException exception) {
        restored = false;
        LOGGER.error(
            "Inventory rollback slot failed owner={} slot={}", player.getUUID(), index, exception);
      }
    try {
      player.getInventory().setChanged();
    } catch (RuntimeException exception) {
      restored = false;
      LOGGER.error("Inventory rollback setChanged failed owner={}", player.getUUID(), exception);
    }
    return restored;
  }

  public long countMatching(Object value) {
    return new SlotInventoryTransactions<>(slots()).countMatching((ItemStack) value);
  }

  public InventoryRemovalResult removeMatching(Object value, int quantity) {
    return new SlotInventoryTransactions<>(slots()).remove((ItemStack) value, quantity);
  }

  private SlotInventoryTransactions.Slots<ItemStack> slots() {
    return new SlotInventoryTransactions.Slots<>() {
      public int size() { return player.getInventory().items.size(); }
      public ItemStack get(int index) { return player.getInventory().items.get(index); }
      public void set(int index, ItemStack value) { player.getInventory().setItem(index, value); }
      public ItemStack copy(ItemStack value) { return value.copy(); }
      public boolean isEmpty(ItemStack value) { return value.isEmpty(); }
      public boolean matches(ItemStack value, ItemStack template) { return Forge1201Platform.nativeItemStacks().sameItemAndData(value, template); }
      public int count(ItemStack value) { return value.getCount(); }
      public void setCount(ItemStack value, int count) { value.setCount(count); }
      public int maxStackSize(ItemStack value) { return value.getMaxStackSize(); }
      public void setChanged() { player.getInventory().setChanged(); }
      public void rollbackError(int index, RuntimeException error) {
        LOGGER.error("Inventory rollback failed owner={} slot={}", player.getUUID(), index, error);
      }
    };
  }
}
