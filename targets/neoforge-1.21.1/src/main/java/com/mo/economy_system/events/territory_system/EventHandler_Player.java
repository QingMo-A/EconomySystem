package com.mo.economy_system.events.territory_system;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.territory.TerritoryPresenceService;
import com.mo.economy_system.common.territory.TerritoryRuntimePolicy;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/** NeoForge event adapter for common territory runtime semantics. */
@EventBusSubscriber(modid = EconomySystem.MODID)
public final class EventHandler_Player {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<MinecraftServer, TerritoryPresenceService> PRESENCE_BY_SERVER =
      new IdentityHashMap<>();

  private EventHandler_Player() {}

  @SubscribeEvent
  public static void onPlayerTick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) return;
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
    outcome.exited().ifPresent(value -> postLeave(player, value));
    outcome.entered().ifPresent(value -> {
      postEnter(player, value);
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

  public static void showSelectionBoundary(
      ServerPlayer player,
      com.mo.economy_system.common.territory.TerritorySelectionService.Point first,
      com.mo.economy_system.common.territory.TerritorySelectionService.Point second) {
    showBoundary(
        player.serverLevel(),
        new Position(first.x(), first.y(), first.z()),
        new Position(second.x(), second.y(), second.z()),
        player.blockPosition(),
        TerritoryRuntimePolicy.SELECTION_BOUNDARY_Y_OFFSET);
  }

  public static void clear(MinecraftServer server, UUID playerId) {
    if (server == null || playerId == null) return;
    synchronized (PRESENCE_BY_SERVER) {
      TerritoryPresenceService service = PRESENCE_BY_SERVER.get(server);
      if (service != null) service.clear(playerId);
    }
  }

  public static void stop(MinecraftServer server) {
    if (server == null) return;
    synchronized (PRESENCE_BY_SERVER) {
      TerritoryPresenceService service = PRESENCE_BY_SERVER.remove(server);
      if (service != null) service.clearAll();
    }
  }

  private static boolean hasPermission(ServerPlayer player, BlockPos position, RuleAction action) {
    Optional<Owned> territory = territoryAt(player, position.getX(), position.getZ());
    return territory.isEmpty()
        || TerritoryRuntimePolicy.allows(
            territory.orElseThrow(), action, player.getUUID(), player.hasPermissions(2));
  }

  private static Optional<Owned> territoryAt(ServerPlayer player, int x, int z) {
    Territory territory = TerritoryManager.getTerritoryAtIgnoreY(
        player.serverLevel().dimension(), x, z);
    return territory == null
        ? Optional.empty()
        : Optional.of(TerritoryNetworkSnapshots.owned(territory));
  }

  private static boolean isContainerBlock(ServerPlayer player, BlockPos position) {
    Block block = player.serverLevel().getBlockState(position).getBlock();
    return block instanceof BaseEntityBlock || player.serverLevel().getBlockEntity(position) != null;
  }

  private static void deny(ServerPlayer player, RuleAction action) {
    player.sendSystemMessage(Component.translatable(TerritoryRuntimePolicy.denialMessageKey(action)));
  }

  private static void postEnter(ServerPlayer player, Owned snapshot) {
    NeoForge.EVENT_BUS.post(new Event_PlayerEnterTerritory(player, nativeTerritory(snapshot)));
  }

  private static void postLeave(ServerPlayer player, Owned snapshot) {
    NeoForge.EVENT_BUS.post(new Event_PlayerLeaveTerritory(player, nativeTerritory(snapshot)));
  }

  private static Territory nativeTerritory(Owned snapshot) {
    Territory territory = TerritoryManager.getTerritoryByID(snapshot.summary().territoryId());
    return territory == null ? TerritoryNetworkSnapshots.restoreOwned(snapshot) : territory;
  }

  private static void applyBuffs(ServerPlayer player, Owned territory) {
    for (TerritoryRuntimePolicy.EffectApplication application
        : TerritoryRuntimePolicy.activeEffects(territory)) {
      ResourceLocation effectId = ResourceLocation.tryParse(application.effectId());
      if (effectId == null) {
        LOGGER.warn("invalid territory buff effect id={}", application.effectId());
        continue;
      }
      var effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
      if (effect == null) {
        LOGGER.warn("unknown territory buff effect id={}", application.effectId());
        continue;
      }
      player.addEffect(new MobEffectInstance(
          effect,
          application.durationTicks(),
          application.amplifier(),
          false,
          true));
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
