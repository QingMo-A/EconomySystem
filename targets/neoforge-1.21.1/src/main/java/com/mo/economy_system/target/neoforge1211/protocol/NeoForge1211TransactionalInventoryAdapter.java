package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.platform.EconomyServices;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class NeoForge1211TransactionalInventoryAdapter
    implements MarketItemMaterializer, TransactionalInventory, TransactionalInventoryRemoval {
  private final ServerPlayer player;

  NeoForge1211TransactionalInventoryAdapter(ServerPlayer player) {
    this.player = Objects.requireNonNull(player);
  }

  public UUID ownerId() {
    return player.getUUID();
  }

  public Object restore(MarketOrder order) {
    return EconomyServices.platform()
        .itemStacks()
        .restoreSnapshot(order.item(), player.registryAccess())
        .orElseThrow();
  }

  public boolean canAccept(Object value, int quantity) {
    return capacity((ItemStack) value) >= quantity;
  }

  private int capacity(ItemStack template) {
    long result = 0;
    for (ItemStack stack : player.getInventory().items) {
      if (stack.isEmpty()) result += template.getMaxStackSize();
      else if (EconomyServices.platform().itemStacks().sameItemAndData(stack, template))
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
            && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
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
        EconomySystem.LOGGER.error(
            "Inventory rollback slot failed owner={} slot={}", player.getUUID(), index, exception);
      }
    try {
      player.getInventory().setChanged();
    } catch (RuntimeException exception) {
      restored = false;
      EconomySystem.LOGGER.error(
          "Inventory rollback setChanged failed owner={}", player.getUUID(), exception);
    }
    return restored;
  }

  public long countMatching(Object value) {
    ItemStack template = (ItemStack) value;
    long count = 0;
    for (ItemStack stack : player.getInventory().items)
      if (!stack.isEmpty()
          && EconomyServices.platform().itemStacks().sameItemAndData(stack, template))
        count += stack.getCount();
    return count;
  }

  public InventoryRemovalResult removeMatching(Object value, int quantity) {
    ItemStack template = (ItemStack) value;
    List<ItemStack> before = player.getInventory().items.stream().map(ItemStack::copy).toList();
    try {
      int remaining = quantity;
      for (ItemStack stack : player.getInventory().items)
        if (remaining > 0
            && !stack.isEmpty()
            && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
          int removed = Math.min(remaining, stack.getCount());
          stack.shrink(removed);
          remaining -= removed;
        }
      if (remaining != 0) return InventoryRemovalResult.failure(restoreSlots(before));
      player.getInventory().setChanged();
      return InventoryRemovalResult.success(() -> restoreSlots(before));
    } catch (RuntimeException exception) {
      return InventoryRemovalResult.failure(restoreSlots(before));
    }
  }
}
