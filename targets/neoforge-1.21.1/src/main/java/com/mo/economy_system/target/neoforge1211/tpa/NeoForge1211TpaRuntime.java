package com.mo.economy_system.target.neoforge1211.tpa;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.RecallPotionReservation;
import com.mo.economy_system.common.territory.TerritoryTeleportService;
import com.mo.economy_system.common.tpa.TpaPort;
import com.mo.economy_system.common.tpa.TpaRequest;
import com.mo.economy_system.common.tpa.TpaRequestStore;
import com.mo.economy_system.common.tpa.TpaReservationException;
import com.mo.economy_system.common.tpa.TpaService;
import com.mo.economy_system.common.tpa.TpaServiceRegistry;
import com.mo.economy_system.item.EconomySystem_Items;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.slf4j.Logger;

/** NeoForge player, inventory, and teleport API adapter for common TPA semantics. */
public final class NeoForge1211TpaRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TpaServiceRegistry<MinecraftServer> SERVICES = new TpaServiceRegistry<>();

  private NeoForge1211TpaRuntime() {}

  public static TpaService service(MinecraftServer server) {
    return SERVICES.get(server, value -> new TpaService(
        new TpaRequestStore(),
        new Adapter(value),
        (stage, request, slot, primary, secondary) ->
            LOGGER.warn(
                "TPA issue stage={} request={} slot={}", stage, request, slot, primary)));
  }

  public static List<TpaRequest> expire(MinecraftServer server) {
    return service(server).expire(server.getTickCount());
  }

  public static void shutdown(MinecraftServer server) {
    SERVICES.remove(server);
  }

  private static final class Adapter implements TpaPort {
    private final MinecraftServer server;

    private Adapter(MinecraftServer server) {
      this.server = server;
    }

    @Override
    public boolean isOnline(UUID playerId) {
      return player(playerId) != null;
    }

    @Override
    public boolean hasWormholePotion(UUID playerId) {
      ServerPlayer player = player(playerId);
      return player != null && player.getInventory().items.stream()
          .anyMatch(stack -> stack.is(EconomySystem_Items.WORMHOLE_POTION.get()));
    }

    @Override
    public Optional<PotionReservation> reserveWormholePotion(UUID playerId) throws Exception {
      ServerPlayer player = player(playerId);
      if (player == null) return Optional.empty();
      Inventory inventory = new Inventory(player);
      for (int slot = 0; slot < inventory.size(); slot++) {
        if (!inventory.get(slot).is(EconomySystem_Items.WORMHOLE_POTION.get())) continue;
        try {
          TerritoryTeleportService.Reservation reservation =
              RecallPotionReservation.reserve(slot, inventory.get(slot), inventory);
          return Optional.of(wrap(reservation));
        } catch (com.mo.economy_system.common.territory.RecallPotionReserveException error) {
          throw new TpaReservationException(slot, error.rollbackFailed(), error);
        }
      }
      return Optional.empty();
    }

    @Override
    public void teleport(UUID senderId, UUID targetId) {
      ServerPlayer sender = requirePlayer(senderId);
      ServerPlayer target = requirePlayer(targetId);
      ServerLevel level = target.serverLevel();
      BlockPos position = target.blockPosition();
      ChunkPos chunk = new ChunkPos(position);
      level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunk, 1, sender.getId());
      if (level.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true) == null) {
        throw new IllegalStateException("target chunk unavailable");
      }
      sender.teleportTo(
          level,
          target.getX(),
          target.getY(),
          target.getZ(),
          sender.getYRot(),
          sender.getXRot());
    }

    @Override
    public TeleportArrival arrival(UUID senderId, UUID targetId) {
      ServerPlayer sender = player(senderId);
      ServerPlayer target = player(targetId);
      if (sender == null || target == null) return TeleportArrival.UNKNOWN;
      return sender.serverLevel() == target.serverLevel()
              && sender.distanceToSqr(target.getX(), target.getY(), target.getZ()) <= 4.0D
          ? TeleportArrival.ARRIVED
          : TeleportArrival.NOT_ARRIVED;
    }

    @Override
    public void effects(UUID senderId, UUID targetId) {
      ServerPlayer sender = requirePlayer(senderId);
      ServerLevel level = sender.serverLevel();
      level.sendParticles(
          ParticleTypes.PORTAL,
          sender.getX(),
          sender.getY() + 1.0D,
          sender.getZ(),
          50,
          1.0D,
          1.0D,
          1.0D,
          0.1D);
      level.playSound(
          null,
          sender.blockPosition(),
          SoundEvents.ENDERMAN_TELEPORT,
          SoundSource.PLAYERS,
          1.0F,
          1.0F);
    }

    private ServerPlayer player(UUID id) {
      return server.getPlayerList().getPlayer(id);
    }

    private ServerPlayer requirePlayer(UUID id) {
      ServerPlayer player = player(id);
      if (player == null) throw new IllegalStateException("player went offline: " + id);
      return player;
    }

    private static PotionReservation wrap(TerritoryTeleportService.Reservation reservation) {
      return new PotionReservation() {
        @Override
        public int slot() {
          return reservation.slot();
        }

        @Override
        public void commit() {
          reservation.commit();
        }

        @Override
        public void rollback() throws Exception {
          reservation.rollback();
        }
      };
    }
  }

  private static final class Inventory implements RecallPotionReservation.Slots<ItemStack> {
    private final ServerPlayer player;

    private Inventory(ServerPlayer player) {
      this.player = player;
    }

    public int size() { return player.getInventory().items.size(); }
    public ItemStack get(int slot) { return player.getInventory().items.get(slot); }
    public void set(int slot, ItemStack value) { player.getInventory().items.set(slot, value); }
    public ItemStack copy(ItemStack value) { return value.copy(); }
    public ItemStack withCount(ItemStack value, int count) { return value.copyWithCount(count); }
    public boolean equivalent(ItemStack left, ItemStack right) {
      return ItemStack.isSameItemSameComponents(left, right);
    }
    public boolean empty(ItemStack value) { return value.isEmpty(); }
    public boolean canMerge(ItemStack existing, ItemStack removed) {
      return !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, removed);
    }
    public int count(ItemStack value) { return value.getCount(); }
    public int maximum(ItemStack value) { return value.getMaxStackSize(); }
    public ItemStack withAddedOne(ItemStack existing, ItemStack removed) {
      ItemStack result = existing.isEmpty() ? removed.copy() : existing.copy();
      if (!existing.isEmpty()) result.grow(1);
      return result;
    }
    public void markChanged() { player.getInventory().setChanged(); }
    public void synchronizeClient() { player.containerMenu.broadcastChanges(); }
    public void warning(String stage, Exception error) {
      LOGGER.warn("TPA inventory issue stage={} player={}", stage, player.getUUID(), error);
    }
  }
}
