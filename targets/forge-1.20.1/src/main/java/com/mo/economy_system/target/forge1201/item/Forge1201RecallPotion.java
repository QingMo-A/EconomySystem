package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.common.territory.RecallPotionUseService;
import com.mo.economy_system.common.territory.RecallPotionUseService.Arrival;
import com.mo.economy_system.common.territory.RecallPotionUseService.Lookup;
import com.mo.economy_system.common.territory.RecallPotionUseService.Result;
import com.mo.economy_system.common.territory.RecallPotionUseService.Target;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/** Forge API adapter for the common return-to-spawn recall transaction. */
public final class Forge1201RecallPotion extends Item {
  private static final Logger LOGGER = LogUtils.getLogger();

  public Forge1201RecallPotion(Properties properties) {
    super(properties.food(new FoodProperties.Builder()
        .alwaysEat()
        .nutrition(0)
        .saturationMod(0.0F)
        .build()));
  }

  @Override
  public UseAnim getUseAnimation(ItemStack stack) {
    return UseAnim.DRINK;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    player.startUsingItem(hand);
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
    if (level.isClientSide || !(entity instanceof ServerPlayer player)
        || !(level instanceof ServerLevel current)) {
      return super.finishUsingItem(stack, level, entity);
    }

    Result result = RecallPotionUseService.execute(
        new Port(player, current),
        (stage, primary, secondary) -> LOGGER.warn(
            "Forge recall potion issue stage={} player={}", stage, player.getUUID(), primary));
    switch (result) {
      case DIMENSION_NOT_FOUND -> player.sendSystemMessage(Component.translatable(
          "message.recall_potion.error_dimension_not_found"));
      case TELEPORT_FAILED -> player.sendSystemMessage(Component.translatable(
          "message.teleport.failed"));
      case TELEPORT_STATE_UNKNOWN -> player.sendSystemMessage(Component.translatable(
          "message.teleport.state_unknown"));
      case SUCCESS -> {
      }
    }
    return result.consumesItem() ? super.finishUsingItem(stack, level, entity) : stack;
  }

  private record Port(ServerPlayer player, ServerLevel source)
      implements RecallPotionUseService.Port<ServerLevel> {
    @Override
    public Lookup<ServerLevel> respawnTarget() {
      BlockPos position = player.getRespawnPosition();
      if (position == null) return Lookup.notConfigured();
      ServerLevel destination = source.getServer().getLevel(player.getRespawnDimension());
      return destination == null
          ? Lookup.dimensionNotFound()
          : Lookup.found(target(destination, position));
    }

    @Override
    public Lookup<ServerLevel> defaultTarget() {
      ServerLevel destination = source.getServer().overworld();
      return destination == null
          ? Lookup.dimensionNotFound()
          : Lookup.found(target(destination, destination.getSharedSpawnPos()));
    }

    @Override
    public void prepare(Target<ServerLevel> target) {
      BlockPos position = block(target.position());
      if (!target.dimension().isLoaded(position)) {
        target.dimension().getChunkSource().addRegionTicket(
            TicketType.POST_TELEPORT, new ChunkPos(position), 1, player.getId());
      }
    }

    @Override
    public void sourceEffect() {
      source.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(), player.getZ(),
          50, 1, 1, 1, 0.1D);
    }

    @Override
    public void teleport(Target<ServerLevel> target) {
      Position position = target.position();
      player.teleportTo(target.dimension(), position.x() + 0.5D,
          position.y() + 1.0D, position.z() + 0.5D, player.getYRot(), player.getXRot());
    }

    @Override
    public Arrival arrival(Target<ServerLevel> target) {
      Position position = target.position();
      double x = position.x() + 0.5D;
      double y = position.y() + 1.0D;
      double z = position.z() + 0.5D;
      return player.serverLevel() == target.dimension() && player.distanceToSqr(x, y, z) <= 0.25D
          ? Arrival.ARRIVED
          : Arrival.NOT_ARRIVED;
    }

    @Override
    public void destinationEffects(Target<ServerLevel> target) {
      Position position = target.position();
      target.dimension().sendParticles(ParticleTypes.PORTAL,
          position.x(), position.y(), position.z(), 50, 1, 1, 1, 0.1D);
      target.dimension().playSound(null, block(position), SoundEvents.ENDERMAN_TELEPORT,
          SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static Target<ServerLevel> target(ServerLevel dimension, BlockPos position) {
      return new Target<>(dimension, new Position(position.getX(), position.getY(), position.getZ()));
    }

    private static BlockPos block(Position position) {
      return new BlockPos(position.x(), position.y(), position.z());
    }
  }
}
