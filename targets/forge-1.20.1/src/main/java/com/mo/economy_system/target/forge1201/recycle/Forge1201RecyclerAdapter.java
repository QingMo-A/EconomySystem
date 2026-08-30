package com.mo.economy_system.target.forge1201.recycle;

import com.mo.economy_system.common.recycle.RecycleConfig;
import com.mo.economy_system.common.recycle.RecycleConfigLoader;
import com.mo.economy_system.common.recycle.RecycleResult;
import com.mo.economy_system.common.recycle.RecycleService;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** Forge adapter for the loader-neutral recycling transaction service. */
public final class Forge1201RecyclerAdapter {
  private static final Map<MinecraftServer, RecycleService> SERVICES = new WeakHashMap<>();
  private static final Map<MinecraftServer, RecycleConfig> CONFIGS = new WeakHashMap<>();

  private Forge1201RecyclerAdapter() {}

  public static synchronized void initialize(MinecraftServer server) {
    CONFIGS.put(server, loadConfig(server));
    SERVICES.remove(server);
  }

  public static synchronized void shutdown(MinecraftServer server) {
    SERVICES.remove(server);
    CONFIGS.remove(server);
  }

  public static synchronized boolean reload(MinecraftServer server) {
    RecycleConfig config = loadConfig(server);
    CONFIGS.put(server, config);
    SERVICES.remove(server);
    return true;
  }

  public static RecycleResult recycleHeld(ServerPlayer player, int amount) {
    ItemStack held = player.getMainHandItem();
    if (held.isEmpty()) return new RecycleResult(RecycleResult.Status.UNKNOWN_ITEM, 0, 0, 0, 0);
    ItemStack one = held.copy();
    one.setCount(1);
    ItemStackSnapshot snapshot = Forge1201Platform.nativeItemStacks()
        .captureSnapshot(one, player.serverLevel().registryAccess()).orElseThrow();
    return service(player.server).recycle(player.getUUID(), snapshot, amount, UUID.randomUUID(), System.currentTimeMillis());
  }

  public static RecycleResult recycle(ServerPlayer player, String itemId, int amount, UUID submissionId) {
    ResourceLocation id = ResourceLocation.tryParse(itemId);
    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return new RecycleResult(RecycleResult.Status.UNKNOWN_ITEM, 0, 0, 0, 0);
    ItemStack one = new ItemStack(BuiltInRegistries.ITEM.get(id));
    ItemStackSnapshot snapshot = Forge1201Platform.nativeItemStacks().captureSnapshot(one, player.serverLevel().registryAccess()).orElseThrow();
    return service(player.server).recycle(player.getUUID(), snapshot, amount, submissionId, System.currentTimeMillis());
  }

  public static List<com.mo.economy_system.common.network.RecycleOfferSnapshot> offers(ServerPlayer player) {
    long now = System.currentTimeMillis(); RecycleConfig config = service(player.server).config(); List<com.mo.economy_system.common.network.RecycleOfferSnapshot> values = new ArrayList<>();
    for (var offer : config.offers()) {
      ResourceLocation id = ResourceLocation.tryParse(offer.itemId()); if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
      ItemStackSnapshot snapshot = Forge1201Platform.nativeItemStacks().captureSnapshot(new ItemStack(BuiltInRegistries.ITEM.get(id)), player.serverLevel().registryAccess()).orElseThrow();
      int owned = new InventoryPort().count(player.getUUID(), snapshot); int quota = service(player.server).highQuotaRemaining(now).getOrDefault(offer.itemId(), 0);
      values.add(new com.mo.economy_system.common.network.RecycleOfferSnapshot(offer.itemId(), offer.baseUnitPrice(), offer.highUnitPrice(), quota, owned, BuiltInRegistries.ITEM.get(id).getMaxStackSize(), offer.fallbackToBaseWhenHighQuotaExhausted()));
    }
    return List.copyOf(values);
  }

  public static long cycleEndsAt(ServerPlayer player, long nowMillis) {
    long duration = service(player.server).config().cycle().toMillis(); return (Math.floorDiv(nowMillis, duration) + 1L) * duration;
  }

  public static RecycleConfig config() { return service(Forge1201Platform.activeServer()).config(); }

  private static ServerPlayer find(UUID id) {
    MinecraftServer server = Forge1201Platform.activeServer();
    return server == null ? null : server.getPlayerList().getPlayer(id);
  }

  private static ItemStack nativeStack(ServerPlayer player, ItemStackSnapshot snapshot) {
    return Forge1201Platform.nativeItemStacks().restoreSnapshot(snapshot, player.serverLevel().registryAccess()).orElseThrow();
  }

  private static synchronized RecycleService service(MinecraftServer server) {
    if (server == null) throw new IllegalStateException("no active Minecraft server");
    Forge1201CommissionSavedData persisted = Forge1201CommissionSavedData.get(server.overworld());
    return SERVICES.computeIfAbsent(server, value -> new RecycleService(
        CONFIGS.computeIfAbsent(value, Forge1201RecyclerAdapter::loadConfig),
        new InventoryPort(), new EconomyPort(), new RecycleService.StateRepository() {
          @Override public RecycleService.State load() { return persisted.loadRecycleState(); }
          @Override public void save(RecycleService.State state) { persisted.saveRecycleState(state); }
        }));
  }

  private static RecycleConfig loadConfig(MinecraftServer server) {
    Path path = server.getServerDirectory().toPath()
        .resolve("config").resolve("economysystem").resolve("recycle.json");
    return RecycleConfigLoader.loadOrCreate(path);
  }

  private static final class InventoryPort implements RecycleService.InventoryPort {
    public int count(UUID playerId, ItemStackSnapshot item) {
      ServerPlayer player = find(playerId);
      if (player == null) return 0;
      ItemStack template = nativeStack(player, item);
      int count = 0;
      for (ItemStack stack : player.getInventory().items) {
        if (Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) count += stack.getCount();
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
        if (!Forge1201Platform.nativeItemStacks().sameItemAndData(stack, template)) continue;
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
