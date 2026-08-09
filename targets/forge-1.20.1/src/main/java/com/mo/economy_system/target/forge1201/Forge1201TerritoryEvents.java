package com.mo.economy_system.target.forge1201;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.TerritoryPresenceService;
import com.mo.economy_system.common.territory.TerritoryRuntimePolicy;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritorySnapshotStore;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Forge 1.20.1 API adapter for the common territory runtime. */
@Mod.EventBusSubscriber(modid = EconomySystemForge1201.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201TerritoryEvents {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<MinecraftServer, TerritoryPresenceService> PRESENCE_BY_SERVER =
      new IdentityHashMap<>();

  private Forge1201TerritoryEvents() {}

  @SubscribeEvent
  public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
    MinecraftServer server = player.getServer();
    if (server == null || !server.isSameThread()) return;

    BlockPos position = player.blockPosition();
    TerritoryPresenceService.TickOutcome outcome = presence(server).tick(
        player.getUUID(),
        server.getTickCount(),
        new TerritoryPresenceService.Location(
            player.serverLevel().dimension().location().toString(),
            position.getX(),
            position.getZ()),
        location -> territoryAt(player, location.x(), location.z()));
    if (outcome.lookupFailed()) {
      LOGGER.warn("territory presence lookup failed player={}", player.getUUID());
    }
    outcome.exited().ifPresent(value -> notifyLeave(player, value));
    outcome.entered().ifPresent(value -> {
      notifyEnter(player, value);
      showBoundary(
          player.serverLevel(),
          value.summary().pos1(),
          value.summary().pos2(),
          player.blockPosition(),
          TerritoryRuntimePolicy.TERRITORY_BOUNDARY_Y_OFFSET);
    });
    if (outcome.applyBuffs()) applyBuffs(player, outcome.current().orElseThrow());
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
    if (event.getEntity() instanceof ServerPlayer player
        && !hasPermission(player, event.getPos(), RuleAction.PLACE_BLOCK)) {
      deny(player, RuleAction.PLACE_BLOCK);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onBlockBreak(BlockEvent.BreakEvent event) {
    Player player = event.getPlayer();
    if (player instanceof ServerPlayer serverPlayer
        && !hasPermission(serverPlayer, event.getPos(), RuleAction.BREAK_BLOCK)) {
      deny(serverPlayer, RuleAction.BREAK_BLOCK);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
    if (event.getEntity() instanceof ServerPlayer player
        && !hasPermission(player, player.blockPosition(), RuleAction.USE_ITEM)) {
      deny(player, RuleAction.USE_ITEM);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
    RuleAction action = isContainerBlock(player, event.getPos())
        ? RuleAction.OPEN_CONTAINER
        : RuleAction.INTERACT_BLOCK;
    if (!hasPermission(player, event.getPos(), action)) {
      deny(player, action);
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
    synchronized (PRESENCE_BY_SERVER) {
      TerritoryPresenceService service = PRESENCE_BY_SERVER.get(player.getServer());
      if (service != null) service.clear(player.getUUID());
    }
  }

  @SubscribeEvent
  public static void onServerStopped(ServerStoppedEvent event) {
    synchronized (PRESENCE_BY_SERVER) {
      TerritoryPresenceService service = PRESENCE_BY_SERVER.remove(event.getServer());
      if (service != null) service.clearAll();
    }
  }

  public static void showSelectionBoundary(ServerPlayer player, Position first, Position second) {
    showBoundary(
        player.serverLevel(),
        first,
        second,
        player.blockPosition(),
        TerritoryRuntimePolicy.SELECTION_BOUNDARY_Y_OFFSET);
  }

  private static boolean hasPermission(ServerPlayer player, BlockPos position, RuleAction action) {
    Optional<Owned> territory = territoryAt(player, position.getX(), position.getZ());
    return territory.isEmpty()
        || TerritoryRuntimePolicy.allows(
            territory.orElseThrow(), action, player.getUUID(), player.hasPermissions(2));
  }

  private static Optional<Owned> territoryAt(ServerPlayer player, int x, int z) {
    return Forge1201TerritorySnapshotStore.get(player.serverLevel()).at(
        player.serverLevel().dimension().location().toString(), x, z);
  }

  private static boolean isContainerBlock(ServerPlayer player, BlockPos position) {
    Block block = player.serverLevel().getBlockState(position).getBlock();
    return block instanceof BaseEntityBlock || player.serverLevel().getBlockEntity(position) != null;
  }

  private static void deny(ServerPlayer player, RuleAction action) {
    player.sendSystemMessage(Component.translatable(TerritoryRuntimePolicy.denialMessageKey(action)));
  }

  private static void notifyEnter(ServerPlayer player, Owned territory) {
    player.sendSystemMessage(
        Component.translatable("message.territory.runtime.enter", territory.summary().name())
            .withStyle(ChatFormatting.GREEN));
    player.connection.send(new ClientboundSetTitleTextPacket(
        Component.translatable("message.territory.runtime.welcome", territory.summary().name())
            .withStyle(ChatFormatting.AQUA)));
    player.connection.send(new ClientboundSetSubtitleTextPacket(
        Component.translatable("message.territory.runtime.owner", territory.summary().ownerName())
            .withStyle(ChatFormatting.GOLD)));
    player.connection.send(new ClientboundSetTitlesAnimationPacket(
        TerritoryRuntimePolicy.TITLE_FADE_IN_TICKS,
        TerritoryRuntimePolicy.TITLE_STAY_TICKS,
        TerritoryRuntimePolicy.TITLE_FADE_OUT_TICKS));
  }

  private static void notifyLeave(ServerPlayer player, Owned territory) {
    player.sendSystemMessage(
        Component.translatable("message.territory.runtime.leave", territory.summary().name())
            .withStyle(ChatFormatting.RED));
  }

  private static void applyBuffs(ServerPlayer player, Owned territory) {
    for (TerritoryRuntimePolicy.EffectApplication application
        : TerritoryRuntimePolicy.activeEffects(territory)) {
      ResourceLocation effectId = ResourceLocation.tryParse(application.effectId());
      if (effectId == null) {
        LOGGER.warn("invalid territory buff effect id={}", application.effectId());
        continue;
      }
      BuiltInRegistries.MOB_EFFECT.getOptional(effectId).ifPresentOrElse(
          effect -> player.addEffect(new MobEffectInstance(
              effect,
              application.durationTicks(),
              application.amplifier(),
              false,
              true)),
          () -> LOGGER.warn("unknown territory buff effect id={}", application.effectId()));
    }
  }

  private static void showBoundary(
      ServerLevel level,
      Position first,
      Position second,
      BlockPos playerPosition,
      double yOffset) {
    for (TerritoryRuntimePolicy.BoundaryColumn column
        : TerritoryRuntimePolicy.nearestBoundaryColumns(
            first,
            second,
            playerPosition.getX(),
            playerPosition.getZ(),
            TerritoryRuntimePolicy.BOUNDARY_PARTICLE_RADIUS)) {
      level.sendParticles(
          ParticleTypes.END_ROD,
          column.x() + 0.5D,
          column.minY() + yOffset,
          column.z() + 0.5D,
          1,
          0,
          0,
          0,
          0);
      if (column.maxY() != column.minY()) {
        level.sendParticles(
            ParticleTypes.END_ROD,
            column.x() + 0.5D,
            column.maxY() + yOffset,
            column.z() + 0.5D,
            1,
            0,
            0,
            0,
            0);
      }
    }
  }

  private static TerritoryPresenceService presence(MinecraftServer server) {
    synchronized (PRESENCE_BY_SERVER) {
      return PRESENCE_BY_SERVER.computeIfAbsent(server, ignored -> new TerritoryPresenceService());
    }
  }
}
