package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.territory.*;
import com.mo.economy_system.common.territory.RecallPotionReservation;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.item.EconomySystem_Items;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211TerritoryTeleportHandler {
  private static final TerritoryTeleportRateLimiter LIMITER = new TerritoryTeleportRateLimiter();
  private NeoForge1211TerritoryTeleportHandler() {}

  public static void handle(TeleportToTerritoryMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      try {
        Adapter adapter = new Adapter(player);
        TerritoryTeleportService<ServerLevel> service = new TerritoryTeleportService<>(
            id -> Optional.ofNullable(TerritoryManager.getTerritoryByID(id)).map(Adapter::target),
            adapter, adapter, LIMITER,
            (stage, playerId, territoryId, slot, primary, secondary) ->
                EconomySystem.LOGGER.warn("Territory teleport issue stage={} result={} player={} territory={} slot={}",
                    stage, stage.equals("rollback") ? "ROLLBACK_FAILED" : "TELEPORT_FAILED",
                    playerId, territoryId, slot, primary));
        TerritoryTeleportOutcome outcome = service.execute(player.getUUID(), message.territoryId(), player.server.getTickCount());
        player.sendSystemMessage(message(outcome));
      } catch (Exception error) {
        EconomySystem.LOGGER.error("Territory teleport request failed stage=handler player={} territory={}",
            player.getUUID(), message.territoryId(), error);
        player.sendSystemMessage(Component.translatable("message.teleport.failed"));
      }
    });
  }

  static Component message(TerritoryTeleportOutcome outcome) {
    return switch (outcome.result()) {
      case SUCCESS -> Component.translatable("message.teleport.success", outcome.territoryName());
      case TERRITORY_NOT_FOUND -> Component.translatable("message.teleport.target_not_found");
      case NO_PERMISSION -> Component.translatable("message.teleport.no_permission");
      case NO_BACKPOINT -> Component.translatable("message.teleport.no_backpoint");
      case DIMENSION_NOT_FOUND -> Component.translatable("message.teleport.dimension_not_found");
      case UNSAFE_DESTINATION -> Component.translatable("message.teleport.unsafe_destination");
      case NO_RECALL_POTION -> Component.translatable("message.teleport.no_potion");
      case COOLDOWN -> Component.translatable("message.teleport.cooldown");
      case ROLLBACK_FAILED -> Component.translatable("message.teleport.rollback_failed");
      case TELEPORT_FAILED -> Component.translatable("message.teleport.failed");
    };
  }

  private static final class Adapter implements TerritoryTeleportService.DestinationAdapter<ServerLevel>, TerritoryTeleportService.Inventory {
    private final ServerPlayer player;
    Adapter(ServerPlayer player) { this.player = player; }
    static TerritoryTeleportTarget target(Territory territory) {
      Set<UUID> authorized = territory.getAuthorizedPlayers().stream().map(p -> p.getUuid()).collect(Collectors.toUnmodifiableSet());
      BlockPos point = territory.getBackpoint();
      return new TerritoryTeleportTarget(territory.getTerritoryID(), territory.getName(), territory.getOwnerUUID(), authorized,
          territory.getDimension().location().toString(), point == null ? Optional.empty() : Optional.of(new Position(point.getX(), point.getY(), point.getZ())));
    }
    public Optional<ServerLevel> resolve(String id) {
      for (ServerLevel level : player.server.getAllLevels()) {
        if (level.dimension().location().toString().equals(id)) return Optional.of(level);
      }
      return Optional.empty();
    }
    public boolean prepareAndValidate(ServerLevel level, Position backpoint) {
      BlockPos feet = pos(backpoint); BlockPos head = feet.above(); BlockPos below = feet.below();
      if (feet.getY() < level.getMinBuildHeight() || head.getY() >= level.getMaxBuildHeight()
          || !level.getWorldBorder().isWithinBounds(feet)) return false;
      ChunkPos chunk = new ChunkPos(feet);
      level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunk, 1, player.getId());
      if (level.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true) == null) return false;
      BlockState footState = level.getBlockState(feet), headState = level.getBlockState(head), support = level.getBlockState(below);
      if (!safeSpace(level, feet, footState) || !safeSpace(level, head, headState)
          || !support.isFaceSturdy(level, below, Direction.UP)
          || support.is(Blocks.MAGMA_BLOCK) || support.is(Blocks.CAMPFIRE)
          || support.is(Blocks.SOUL_CAMPFIRE) || support.is(Blocks.CACTUS)) return false;
      double x = feet.getX() + .5, y = feet.getY(), z = feet.getZ() + .5;
      AABB box = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
      return withinBorder(level, box) && level.noCollision(player, box);
    }
    private boolean safeSpace(ServerLevel level, BlockPos pos, BlockState state) {
      return state.getCollisionShape(level, pos).isEmpty() && !state.is(BlockTags.FIRE)
          && !state.getFluidState().is(FluidTags.LAVA);
    }
    private boolean withinBorder(ServerLevel level, AABB box) {
      var border=level.getWorldBorder();
      return box.minX>=border.getMinX() && box.maxX<=border.getMaxX()
          && box.minZ>=border.getMinZ() && box.maxZ<=border.getMaxZ();
    }
    public void teleport(ServerLevel level, Position backpoint) {
      BlockPos p = pos(backpoint); player.teleportTo(level, p.getX() + .5, p.getY(), p.getZ() + .5, player.getYRot(), player.getXRot());
    }
    public boolean arrived(ServerLevel level, Position backpoint) {
      BlockPos p = pos(backpoint); double x=p.getX()+.5,y=p.getY(),z=p.getZ()+.5;
      return player.serverLevel() == level && player.distanceToSqr(x,y,z) <= .25;
    }
    public void particles(ServerLevel level, Position backpoint) {
      BlockPos p=pos(backpoint); level.sendParticles(ParticleTypes.PORTAL,p.getX()+.5,p.getY()+1,p.getZ()+.5,32,.4,.8,.4,.1);
    }
    public void sound(ServerLevel level, Position backpoint) {
      BlockPos p=pos(backpoint); level.playSound(null,p,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
    }
    public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion() {
      for (int slot=0; slot<player.getInventory().items.size(); slot++) {
        ItemStack stack=player.getInventory().items.get(slot);
        if (stack.is(EconomySystem_Items.RECALL_POTION.get())) {
          ItemStack removed=stack.copyWithCount(1); stack.shrink(1);
          ItemStack expected=stack.copy(); changed();
          return Optional.of(new RecallPotionReservation<>(slot, removed, expected, slots()));
        }
      }
      return Optional.empty();
    }
    private RecallPotionReservation.Slots<ItemStack> slots() {
      return new RecallPotionReservation.Slots<>() {
        public int size(){return player.getInventory().items.size();}
        public ItemStack get(int slot){return player.getInventory().items.get(slot);}
        public void set(int slot,ItemStack value){player.getInventory().items.set(slot,value);}
        public ItemStack copy(ItemStack value){return value.copy();}
        public boolean equivalent(ItemStack a,ItemStack b){return a.getCount()==b.getCount() && ItemStack.isSameItemSameComponents(a,b);}
        public boolean empty(ItemStack value){return value.isEmpty();}
        public boolean canMerge(ItemStack a,ItemStack b){return !a.isEmpty() && ItemStack.isSameItemSameComponents(a,b);}
        public int count(ItemStack value){return value.getCount();}
        public int maximum(ItemStack value){return value.getMaxStackSize();}
        public ItemStack withAddedOne(ItemStack existing,ItemStack one){ItemStack result=existing.isEmpty()?one.copy():existing.copy();if(!existing.isEmpty())result.grow(1);return result;}
        public void changed(){Adapter.this.changed();}
      };
    }
    private void changed(){player.getInventory().setChanged();player.containerMenu.broadcastChanges();}
    private static BlockPos pos(Position p) { return new BlockPos(p.x(), p.y()+1, p.z()); }
  }
}
