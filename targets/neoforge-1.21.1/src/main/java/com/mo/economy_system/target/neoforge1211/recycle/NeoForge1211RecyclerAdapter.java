package com.mo.economy_system.target.neoforge1211.recycle;

import com.mo.economy_system.common.recycle.RecycleConfig;
import com.mo.economy_system.common.recycle.RecycleResult;
import com.mo.economy_system.common.recycle.RecycleService;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.target.neoforge1211.NeoForge1211Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** NeoForge adapter for the loader-neutral recycling transaction service. */
public final class NeoForge1211RecyclerAdapter {
  private static final RecycleService SERVICE = new RecycleService(
      RecycleConfig.defaults(), new InventoryPort(), new EconomyPort());

  private NeoForge1211RecyclerAdapter() {}

  public static RecycleResult recycleHeld(ServerPlayer player, int amount) {
    ItemStack held = player.getMainHandItem();
    if (held.isEmpty()) return new RecycleResult(RecycleResult.Status.UNKNOWN_ITEM, 0, 0, 0, 0);
    ItemStack one = held.copy();
    one.setCount(1);
    ItemStackSnapshot snapshot = NeoForge1211Platform.nativeItemStacks()
        .captureSnapshot(one, player.serverLevel().registryAccess()).orElseThrow();
    return SERVICE.recycle(player.getUUID(), snapshot, amount, UUID.randomUUID(), player.server.getTickCount() * 50L);
  }

  public static RecycleConfig config() { return SERVICE.config(); }

  private static ServerPlayer find(UUID id) {
    MinecraftServer server = NeoForge1211Platform.activeServer();
    return server == null ? null : server.getPlayerList().getPlayer(id);
  }

  private static ItemStack nativeStack(ServerPlayer player, ItemStackSnapshot snapshot) {
    return NeoForge1211Platform.nativeItemStacks().restoreSnapshot(snapshot, player.serverLevel().registryAccess()).orElseThrow();
  }

  private static final class InventoryPort implements RecycleService.InventoryPort {
    public int count(UUID playerId, ItemStackSnapshot item) {
      ServerPlayer player = find(playerId);
      if (player == null) return 0;
      ItemStack template = nativeStack(player, item);
      int count = 0;
      for (ItemStack stack : player.getInventory().items) {
        if (NeoForge1211Platform.nativeItemStacks().sameItemAndData(stack, template)) count += stack.getCount();
      }
      return count;
    }

    public boolean remove(UUID playerId, ItemStackSnapshot item, int amount) {
      ServerPlayer player = find(playerId);
      if (player == null || count(playerId, item) < amount) return false;
      ItemStack template = nativeStack(player, item);
      int remaining = amount;
      for (ItemStack stack : player.getInventory().items) {
        if (remaining == 0) break;
        if (!NeoForge1211Platform.nativeItemStacks().sameItemAndData(stack, template)) continue;
        int removed = Math.min(remaining, stack.getCount());
        stack.shrink(removed);
        remaining -= removed;
      }
      return remaining == 0;
    }

    public void restore(UUID playerId, ItemStackSnapshot item, int amount) {
      ServerPlayer player = find(playerId);
      if (player == null) throw new IllegalStateException("player left during recycle transaction");
      ItemStack restored = nativeStack(player, item);
      restored.setCount(amount);
      if (!player.getInventory().add(restored)) throw new IllegalStateException("could not restore recycled item");
    }
  }

  private static final class EconomyPort implements RecycleService.EconomyPort {
    public BalanceMutationResult creditExact(UUID playerId, int amount, String category, String reason) {
      ServerPlayer player = find(playerId);
      if (player == null) return BalanceMutationResult.PERSIST_FAILED;
      return EconomySavedData.getInstance(player.serverLevel()).creditExact(playerId, amount, category, reason);
    }
  }
}
