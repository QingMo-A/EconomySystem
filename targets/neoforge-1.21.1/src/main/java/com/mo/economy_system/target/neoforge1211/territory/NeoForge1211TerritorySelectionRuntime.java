package com.mo.economy_system.target.neoforge1211.territory;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.TerritoryGeometry;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.common.territory.TerritorySelectionService.Mode;
import com.mo.economy_system.common.territory.TerritorySelectionService.Point;
import com.mo.economy_system.common.territory.TerritorySelectionService.Result;
import com.mo.economy_system.common.territory.TerritorySelectionService.SelectionOutcome;
import com.mo.economy_system.common.territory.TerritorySelectionService.Session;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.events.territory_system.EventHandler_Player;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** NeoForge shell for the common claim-wand selection state machine. */
public final class NeoForge1211TerritorySelectionRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<MinecraftServer, TerritorySelectionService> BY_SERVER = new WeakHashMap<>();

  private NeoForge1211TerritorySelectionRuntime() {}

  public static boolean hasResize(ServerPlayer player) {
    MinecraftServer server = player == null ? null : player.getServer();
    return server != null
        && state(server).has(player.getUUID(), Mode.RESIZE, server.overworld().getGameTime());
  }

  public static boolean startResize(ServerPlayer player, UUID territoryId) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(territoryId, "territoryId");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) throw new IllegalStateException("selection must run on server thread");
    state(server).startResize(
        player.getUUID(), territoryId, dimension(player), server.overworld().getGameTime());
    player.sendSystemMessage(Component.translatable("message.claim_wand.enter_resize_mode"));
    player.sendSystemMessage(Component.translatable("message.claim_wand.resize_instruction"));
    return true;
  }

  public static void cancelResize(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    MinecraftServer server = player.getServer();
    if (server != null) state(server).clear(player.getUUID());
    player.sendSystemMessage(Component.translatable("message.claim_wand.exit_resize_mode"));
  }

  public static boolean select(ServerPlayer player, BlockPos clicked) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(clicked, "clicked");
    MinecraftServer server = player.getServer();
    if (server == null || !server.isSameThread()) return false;
    TerritorySelectionService service = state(server);
    long tick = server.overworld().getGameTime();
    String dimension = dimension(player);
    SelectionOutcome outcome = service.has(player.getUUID(), Mode.RESIZE, tick)
        ? service.selectResize(
            player.getUUID(),
            service.session(player.getUUID(), Mode.RESIZE, tick).orElseThrow().territoryId(),
            dimension,
            point(clicked),
            tick,
            (first, second, excluded) -> overlaps(player, first, second, excluded))
        : service.selectClaim(
            player.getUUID(),
            dimension,
            point(clicked),
            tick,
            (first, second, excluded) -> overlaps(player, first, second, excluded));
    notifySelection(player, outcome);
    return outcome.result() == Result.FIRST_SELECTED
        || outcome.result() == Result.SECOND_SELECTED
        || outcome.result() == Result.CANCELLED;
  }

  public static Optional<Session> claimSession(ServerPlayer player) {
    if (player == null || player.getServer() == null) return Optional.empty();
    MinecraftServer server = player.getServer();
    return state(server).session(player.getUUID(), Mode.CLAIM, server.overworld().getGameTime());
  }

  public static Optional<Session> resizeSession(ServerPlayer player) {
    if (player == null || player.getServer() == null) return Optional.empty();
    MinecraftServer server = player.getServer();
    return state(server).session(player.getUUID(), Mode.RESIZE, server.overworld().getGameTime());
  }

  public static boolean clear(MinecraftServer server, UUID playerId) {
    return server != null && state(server).clear(playerId);
  }

  public static int clearForTerritory(MinecraftServer server, UUID territoryId) {
    return server == null ? 0 : state(server).clearForTerritory(territoryId);
  }

  public static List<UUID> clearForTerritoryAndList(MinecraftServer server, UUID territoryId) {
    if (server == null) return List.of();
    return state(server).clearForTerritorySessions(territoryId).stream()
        .map(Session::playerId)
        .toList();
  }

  public static void clear(UUID playerId) {
    if (playerId == null) return;
    synchronized (BY_SERVER) {
      BY_SERVER.values().forEach(service -> service.clear(playerId));
    }
  }

  public static Optional<Session> find(UUID playerId, Mode mode) {
    if (playerId == null || mode == null) return Optional.empty();
    synchronized (BY_SERVER) {
      for (Map.Entry<MinecraftServer, TerritorySelectionService> entry : BY_SERVER.entrySet()) {
        Optional<Session> value = entry.getValue().session(
            playerId, mode, entry.getKey().overworld().getGameTime());
        if (value.isPresent()) return value;
      }
    }
    return Optional.empty();
  }

  public static void expire(MinecraftServer server) {
    if (server == null || !server.isSameThread()) return;
    for (Session expired : state(server).expire(server.overworld().getGameTime())) {
      ServerPlayer player = server.getPlayerList().getPlayer(expired.playerId());
      if (player != null) {
        player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
      }
    }
  }

  public static void clearAll() {
    synchronized (BY_SERVER) {
      BY_SERVER.values().forEach(TerritorySelectionService::clearAll);
      BY_SERVER.clear();
    }
  }

  private static void notifySelection(ServerPlayer player, SelectionOutcome outcome) {
    Session session = outcome.session();
    switch (outcome.result()) {
      case FIRST_SELECTED -> {
        Point first = session.first().orElseThrow();
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.first_position_set", first.x(), first.y(), first.z()));
      }
      case SECOND_SELECTED -> {
        Point first = session.first().orElseThrow();
        Point second = session.second().orElseThrow();
        EventHandler_Player.showSelectionBoundary(player, first, second);
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.second_position_set", second.x(), second.y(), second.z()));
        if (session.mode() == Mode.RESIZE) {
          Territory territory = TerritoryManager.getTerritoryByID(session.territoryId());
          if (territory == null) {
            player.sendSystemMessage(Component.translatable("message.claim.resize.not_found"));
            return;
          }
          long oldArea = TerritoryGeometry.area(
              territory.getPos1().getX(), territory.getPos1().getZ(),
              territory.getPos2().getX(), territory.getPos2().getZ());
          long newArea = area(first, second);
          long charge = com.mo.economy_system.common.territory.TerritoryPricing.saturatingPriceForArea(
              Math.max(0L, newArea - oldArea),
              com.mo.economy_system.common.territory.TerritoryPricing.pricePerCell());
          if (newArea > oldArea) {
            player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_expand"));
            player.sendSystemMessage(Component.translatable(
                "message.claim_wand.resize_cost_details", oldArea, newArea, charge));
          } else if (newArea < oldArea) {
            player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_shrink"));
            player.sendSystemMessage(Component.translatable(
                "message.claim_wand.volume_change", oldArea, newArea));
          } else {
            player.sendSystemMessage(Component.translatable("message.claim.resize.unchanged_preview"));
          }
          player.sendSystemMessage(Component.translatable("message.claim.resize.confirm_command"));
        } else {
          long area = area(first, second);
          long price = valid(first) && valid(second)
              ? com.mo.economy_system.common.territory.TerritoryPricing.saturatingPriceForArea(
                  area, com.mo.economy_system.common.territory.TerritoryPricing.pricePerCell())
              : 0L;
          player.sendSystemMessage(Component.translatable("message.claim_wand.volume", area));
          player.sendSystemMessage(Component.translatable("message.claim_wand.price", price));
          player.sendSystemMessage(Component.translatable("message.claim_wand.instruction"));
        }
      }
      case CANCELLED -> player.sendSystemMessage(Component.translatable("message.claim_wand.cancel"));
      case DIMENSION_MISMATCH -> player.sendSystemMessage(Component.translatable("message.claim.resize.wrong_dimension"));
      case Y_MISMATCH -> player.sendSystemMessage(Component.translatable("message.claim_wand.y_mismatch_error"));
      case OVERLAP -> player.sendSystemMessage(Component.translatable("message.claim_wand.overlap_error"));
      case EXPIRED -> player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
      case NO_SESSION -> player.sendSystemMessage(Component.translatable("message.claim.resize.no_session"));
      case STATE_UNKNOWN -> player.sendSystemMessage(Component.translatable("message.claim.resize.state_unknown"));
    }
  }

  private static boolean overlaps(ServerPlayer player, Point first, Point second, UUID excluded) {
    if (!valid(first) || !valid(second)) {
      return false;
    }
    TerritoryGeometry.Rectangle candidate = rectangle(first, second);
    for (Territory territory : TerritoryManager.getAllTerritories()) {
      if (excluded != null && excluded.equals(territory.getTerritoryID())) continue;
      if (!territory.getDimension().equals(player.serverLevel().dimension())) continue;
      if (candidate.intersects(
          TerritoryGeometry.rectangle(
              territory.getPos1().getX(), territory.getPos1().getZ(),
              territory.getPos2().getX(), territory.getPos2().getZ()))) {
        return true;
      }
    }
    return false;
  }

  private static TerritorySelectionService state(MinecraftServer server) {
    synchronized (BY_SERVER) {
      return BY_SERVER.computeIfAbsent(server, ignored -> new TerritorySelectionService());
    }
  }

  private static String dimension(ServerPlayer player) {
    return player.serverLevel().dimension().location().toString();
  }

  private static Point point(BlockPos position) {
    return new Point(position.getX(), position.getY(), position.getZ());
  }

  private static boolean valid(Point point) {
    return TerritoryGeometry.validCoordinate(point.x(), point.z());
  }

  private static long area(Point first, Point second) {
    return TerritoryGeometry.area(first.x(), first.z(), second.x(), second.z());
  }

  private static TerritoryGeometry.Rectangle rectangle(Point first, Point second) {
    return TerritoryGeometry.rectangle(first.x(), first.z(), second.x(), second.z());
  }

}
