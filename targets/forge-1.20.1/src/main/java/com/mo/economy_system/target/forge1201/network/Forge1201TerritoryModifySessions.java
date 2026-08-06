package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Server-scoped, tick-expiring Forge selection sessions for canonical protocol 36. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201TerritoryModifySessions {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final long TIMEOUT_TICKS = 20L * 60L;
  private static final Map<MinecraftServer, Map<UUID, Session>> BY_SERVER = new WeakHashMap<>();

  private Forge1201TerritoryModifySessions() {}

  public static boolean start(ServerPlayer player, UUID territoryId) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(territoryId, "territoryId");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) throw new IllegalStateException("resize session must start on server thread");
    long tick = tick(server);
    synchronized (BY_SERVER) {
      sessions(server).put(
          player.getUUID(),
          new Session(
              territoryId,
              player.serverLevel().dimension().location().toString(),
              tick + TIMEOUT_TICKS));
    }
    player.sendSystemMessage(Component.translatable("message.claim_wand.enter_resize_mode"));
    player.sendSystemMessage(Component.translatable("message.claim_wand.resize_instruction"));
    return true;
  }

  /** Handles first point, second point, and third-click cancellation. */
  public static boolean select(ServerPlayer player, BlockPos clicked) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(clicked, "clicked");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) throw new IllegalStateException("resize selection must run on server thread");
    Session session;
    synchronized (BY_SERVER) {
      session = sessions(server).get(player.getUUID());
      if (session != null && session.expiresAt < tick(server)) {
        sessions(server).remove(player.getUUID());
        session = null;
      }
    }
    if (session == null) {
      player.sendSystemMessage(Component.translatable("message.claim.resize.no_session"));
      return false;
    }
    String dimension = player.serverLevel().dimension().location().toString();
    if (!session.dimensionId.equals(dimension)) {
      cancel(player, "message.claim.resize.wrong_dimension");
      return false;
    }
    if (session.first == null) {
      session.first = clicked.immutable();
      session.second = null;
      player.sendSystemMessage(
          Component.translatable(
              "message.claim_wand.first_position_set",
              clicked.getX(),
              clicked.getY(),
              clicked.getZ()));
      return true;
    }
    if (session.second != null) {
      cancel(player, "message.claim_wand.cancel");
      return true;
    }
    if (session.first.getY() != clicked.getY()) {
      resetPoints(session);
      player.sendSystemMessage(Component.translatable("message.claim_wand.y_mismatch_error"));
      return false;
    }

    Position first = position(session.first);
    Position second = position(clicked);
    Forge1201TerritorySnapshotStore.ResizePrepareOutcome prepared =
        Forge1201TerritorySnapshotStore.get(player.serverLevel())
            .prepareResize(
                session.territoryId, player.getUUID(), session.dimensionId, first, second);
    if (prepared.result() != Forge1201TerritorySnapshotStore.ResizePrepareResult.READY
        && prepared.result() != Forge1201TerritorySnapshotStore.ResizePrepareResult.UNCHANGED) {
      resetPoints(session);
      player.sendSystemMessage(Component.translatable(selectionFailureKey(prepared.result())));
      return false;
    }
    session.second = clicked.immutable();
    player.sendSystemMessage(
        Component.translatable(
            "message.claim_wand.second_position_set",
            clicked.getX(),
            clicked.getY(),
            clicked.getZ()));
    if (prepared.plan() == null) {
      player.sendSystemMessage(Component.translatable("message.claim.resize.unchanged_preview"));
    } else {
      Forge1201TerritorySnapshotStore.ResizePlan plan = prepared.plan();
      if (plan.newArea() > plan.oldArea()) {
        player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_expand"));
        player.sendSystemMessage(
            Component.translatable(
                "message.claim_wand.resize_cost_details",
                plan.oldArea(),
                plan.newArea(),
                plan.charge()));
      } else {
        player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_shrink"));
        player.sendSystemMessage(
            Component.translatable(
                "message.claim_wand.volume_change", plan.oldArea(), plan.newArea()));
      }
    }
    player.sendSystemMessage(Component.translatable("message.claim.resize.confirm_command"));
    return true;
  }

  public static int confirm(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) throw new IllegalStateException("resize confirmation must run on server thread");
    Session session;
    synchronized (BY_SERVER) {
      session = sessions(server).get(player.getUUID());
    }
    if (session == null || session.first == null || session.second == null) {
      player.sendSystemMessage(Component.translatable("message.claim.resize_failed"));
      return 0;
    }
    String dimension = player.serverLevel().dimension().location().toString();
    if (!session.dimensionId.equals(dimension)) {
      cancel(player, "message.claim.resize.wrong_dimension");
      return 0;
    }
    Forge1201TerritoryResizeTransaction.Outcome outcome =
        Forge1201TerritoryResizeTransaction.execute(
            EconomySavedData.getInstance(player.serverLevel()),
            Forge1201TerritorySnapshotStore.get(player.serverLevel()),
            player.getUUID(),
            session.territoryId,
            session.dimensionId,
            position(session.first),
            position(session.second),
            (stage, owner, territory, failure) ->
                LOGGER.warn(
                    "territory resize stage={} player={} territory={}",
                    stage,
                    owner,
                    territory,
                    failure));
    player.sendSystemMessage(Component.translatable(resultKey(outcome.result())));
    if (outcome.result() == Forge1201TerritoryResizeTransaction.Result.SUCCESS
        || outcome.result() == Forge1201TerritoryResizeTransaction.Result.UNCHANGED
        || outcome.result() == Forge1201TerritoryResizeTransaction.Result.STATE_UNKNOWN) {
      synchronized (BY_SERVER) {
        sessions(server).remove(player.getUUID());
      }
    }
    return outcome.result() == Forge1201TerritoryResizeTransaction.Result.SUCCESS
            || outcome.result() == Forge1201TerritoryResizeTransaction.Result.UNCHANGED
        ? 1
        : 0;
  }

  public static int cancelForTerritory(
      MinecraftServer server, UUID territoryId, String territoryName) {
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(territoryId, "territoryId");
    int cleared = 0;
    synchronized (BY_SERVER) {
      Iterator<Map.Entry<UUID, Session>> iterator = sessions(server).entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<UUID, Session> entry = iterator.next();
        if (!territoryId.equals(entry.getValue().territoryId)) continue;
        iterator.remove();
        cleared++;
        ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
        if (player != null) {
          try {
            player.sendSystemMessage(
                Component.translatable(
                    "message.territory.remove.resize_cancelled", territoryName));
          } catch (RuntimeException failure) {
            LOGGER.warn("resize deletion cleanup notification failed player={}", entry.getKey(), failure);
          }
        }
      }
    }
    return cleared;
  }

  private static void cancel(ServerPlayer player, String key) {
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    synchronized (BY_SERVER) {
      sessions(server).remove(player.getUUID());
    }
    player.sendSystemMessage(Component.translatable(key));
    player.sendSystemMessage(Component.translatable("message.claim_wand.exit_resize_mode"));
  }

  private static void resetPoints(Session session) {
    session.first = null;
    session.second = null;
  }

  private static Position position(BlockPos value) {
    return new Position(value.getX(), value.getY(), value.getZ());
  }

  private static String selectionFailureKey(
      Forge1201TerritorySnapshotStore.ResizePrepareResult result) {
    return switch (result) {
      case OVERLAP -> "message.claim.resize.overlap";
      case WRONG_DIMENSION -> "message.claim.resize.wrong_dimension";
      case NOT_FOUND -> "message.claim.resize.not_found";
      case NOT_OWNER -> "message.claim.resize.no_permission";
      case INVALID_BOUNDS -> "message.claim.resize.invalid_bounds";
      case PRICE_OVERFLOW -> "message.claim.resize.price_overflow";
      case STATE_UNKNOWN -> "message.claim.resize.state_unknown";
      case READY, UNCHANGED -> throw new AssertionError();
    };
  }

  private static String resultKey(Forge1201TerritoryResizeTransaction.Result result) {
    return switch (result) {
      case SUCCESS -> "message.claim.resize_success";
      case UNCHANGED -> "message.claim.resize.unchanged";
      case INSUFFICIENT_FUNDS -> "message.claim.resize_insufficient_balance";
      case NOT_FOUND -> "message.claim.resize.not_found";
      case NOT_OWNER -> "message.claim.resize.no_permission";
      case WRONG_DIMENSION -> "message.claim.resize.wrong_dimension";
      case INVALID_BOUNDS -> "message.claim.resize.invalid_bounds";
      case OVERLAP -> "message.claim.resize.overlap";
      case CHANGED -> "message.claim.resize.changed";
      case PERSIST_FAILED -> "message.claim.resize.persist_failed";
      case STATE_UNKNOWN -> "message.claim.resize.state_unknown";
      case REFUND_FAILED -> "message.claim.resize.refund_failed";
      case PAYMENT_FAILED -> "message.claim.resize.payment_failed";
    };
  }

  private static long tick(MinecraftServer server) {
    return server.overworld().getGameTime();
  }

  private static Map<UUID, Session> sessions(MinecraftServer server) {
    return BY_SERVER.computeIfAbsent(server, ignored -> new HashMap<>());
  }

  @SubscribeEvent
  public static void tick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    MinecraftServer server = event.getServer();
    long tick = tick(server);
    synchronized (BY_SERVER) {
      Iterator<Map.Entry<UUID, Session>> iterator = sessions(server).entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<UUID, Session> entry = iterator.next();
        if (entry.getValue().expiresAt >= tick) continue;
        iterator.remove();
        ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
        if (player != null) {
          player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
        }
      }
    }
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
    synchronized (BY_SERVER) {
      sessions(player.getServer()).remove(player.getUUID());
    }
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    synchronized (BY_SERVER) {
      BY_SERVER.remove(event.getServer());
    }
  }

  private static final class Session {
    private final UUID territoryId;
    private final String dimensionId;
    private final long expiresAt;
    private BlockPos first;
    private BlockPos second;

    private Session(UUID territoryId, String dimensionId, long expiresAt) {
      this.territoryId = territoryId;
      this.dimensionId = dimensionId;
      this.expiresAt = expiresAt;
    }
  }
}
