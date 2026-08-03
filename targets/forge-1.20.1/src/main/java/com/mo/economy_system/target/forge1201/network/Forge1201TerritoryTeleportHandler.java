package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.territory.TerritoryTeleportOutcome;
import com.mo.economy_system.common.territory.TerritoryTeleportRateLimiter;
import com.mo.economy_system.common.territory.TerritoryTeleportService;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.target.forge1201.item.Forge1201Items;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

/** Server-side Forge adapter for the common territory teleport transaction. */
final class Forge1201TerritoryTeleportHandler {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TerritoryTeleportRateLimiter LIMITER = new TerritoryTeleportRateLimiter();

  private Forge1201TerritoryTeleportHandler() {}

  static void handle(TeleportToTerritoryMessage message, java.util.function.Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> execute(player, message));
    context.setPacketHandled(true);
  }

  private static void execute(ServerPlayer player, TeleportToTerritoryMessage message) {
    try {
      Adapter adapter = new Adapter(player);
      TerritoryTeleportService<ServerLevel> service = new TerritoryTeleportService<>(
          id -> Forge1201TerritorySnapshotStore.get(player.serverLevel()).find(id),
          adapter,
          adapter,
          LIMITER,
          (stage, playerId, territoryId, slot, primary, secondary) -> LOGGER.warn(
              "Territory teleport issue stage={} player={} territory={} slot={} secondary={}",
              stage, playerId, territoryId, slot,
              secondary == null ? "none" : secondary.toString(), primary));
      long serverTick = player.serverLevel().getServer().getTickCount();
      TerritoryTeleportOutcome outcome = service.execute(
          player.getUUID(), message.territoryId(), serverTick);
      player.sendSystemMessage(message(outcome));
    } catch (Throwable error) {
      LOGGER.error("Territory teleport request failed player={} territory={}",
          player.getUUID(), message.territoryId(), error);
      player.sendSystemMessage(Component.translatable("message.teleport.failed"));
    }
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
      case TELEPORT_FAILED -> Component.translatable("message.teleport.failed");
      case ROLLBACK_FAILED -> Component.translatable("message.teleport.rollback_failed");
    };
  }

  private static final class Adapter
      implements TerritoryTeleportService.DestinationAdapter<ServerLevel>, TerritoryTeleportService.Inventory {
    private final ServerPlayer player;

    private Adapter(ServerPlayer player) {
      this.player = player;
    }

    @Override
    public Optional<ServerLevel> resolve(String dimensionId) {
      ResourceLocation location = ResourceLocation.tryParse(dimensionId);
      if (location == null || !location.toString().equals(dimensionId)) return Optional.empty();
      ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
      return Optional.ofNullable(player.serverLevel().getServer().getLevel(key));
    }

    @Override
    public boolean prepareAndValidate(ServerLevel level, Position backpoint) {
      BlockPos feet = position(backpoint);
      BlockPos head = feet.above();
      BlockPos below = feet.below();
      if (feet.getY() < level.getMinBuildHeight()
          || head.getY() >= level.getMaxBuildHeight()
          || !level.getWorldBorder().isWithinBounds(feet)) return false;

      ChunkPos chunk = new ChunkPos(feet);
      // Keep the target chunk loaded through the cross-dimension move.
      level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunk, 1, player.getId());
      level.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true);

      BlockState footState = level.getBlockState(feet);
      BlockState headState = level.getBlockState(head);
      BlockState support = level.getBlockState(below);
      if (!safeSpace(level, feet, footState) || !safeSpace(level, head, headState)
          || !support.isFaceSturdy(level, below, Direction.UP)
          || support.is(Blocks.MAGMA_BLOCK) || support.is(Blocks.CAMPFIRE)
          || support.is(Blocks.SOUL_CAMPFIRE) || support.is(Blocks.CACTUS)) return false;

      double x = feet.getX() + 0.5;
      double y = feet.getY();
      double z = feet.getZ() + 0.5;
      AABB box = player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ());
      return level.noCollision(player, box);
    }

    private static boolean safeSpace(ServerLevel level, BlockPos pos, BlockState state) {
      return state.getCollisionShape(level, pos).isEmpty()
          && !state.is(BlockTags.FIRE)
          && !state.getFluidState().is(FluidTags.LAVA);
    }

    @Override
    public void teleport(ServerLevel level, Position backpoint) {
      BlockPos feet = position(backpoint);
      player.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5,
          player.getYRot(), player.getXRot());
    }

    @Override
    public boolean arrived(ServerLevel level, Position backpoint) {
      BlockPos feet = position(backpoint);
      double x = feet.getX() + 0.5;
      double y = feet.getY();
      double z = feet.getZ() + 0.5;
      return player.serverLevel() == level && player.distanceToSqr(x, y, z) <= 0.25;
    }

    @Override
    public void particles(ServerLevel level, Position backpoint) {
      BlockPos feet = position(backpoint);
      level.sendParticles(ParticleTypes.PORTAL, feet.getX() + 0.5, feet.getY() + 1,
          feet.getZ() + 0.5, 32, 0.4, 0.8, 0.4, 0.1);
    }

    @Override
    public void sound(ServerLevel level, Position backpoint) {
      level.playSound(null, position(backpoint), SoundEvents.ENDERMAN_TELEPORT,
          SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion() {
      // Only the 36 main-inventory slots are a valid transaction source.
      int limit = player.getInventory().items.size();
      for (int slot = 0; slot < limit; slot++) {
        ItemStack stack = player.getInventory().items.get(slot);
        if (!stack.is(Forge1201Items.RECALL_POTION.get())) continue;
        ItemStack original = stack.copy();
        stack.shrink(1);
        player.getInventory().setChanged();
        int selected = slot;
        return Optional.of(new TerritoryTeleportService.Reservation() {
          @Override public int slot() { return selected; }

          @Override public void rollback() {
            player.getInventory().items.set(selected, original.copy());
            player.getInventory().setChanged();
          }
        });
      }
      return Optional.empty();
    }

    /** Backpoints are saved at the player's feet block; stand one block above them. */
    private static BlockPos position(Position backpoint) {
      return new BlockPos(backpoint.x(), backpoint.y() + 1, backpoint.z());
    }
  }
}
