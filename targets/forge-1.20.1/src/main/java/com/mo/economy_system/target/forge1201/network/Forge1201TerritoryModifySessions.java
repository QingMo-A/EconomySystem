package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.common.territory.TerritorySelectionService.Mode;
import com.mo.economy_system.common.territory.TerritorySelectionService.Point;
import com.mo.economy_system.common.territory.TerritorySelectionService.Result;
import com.mo.economy_system.common.territory.TerritorySelectionService.Session;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.target.forge1201.Forge1201TerritoryEvents;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Forge shell for common resize selection state and transaction policy. */
public final class Forge1201TerritoryModifySessions {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201TerritoryModifySessions() {}

  public static boolean hasSession(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    MinecraftServer server = player.getServer();
    return server != null
        && Forge1201TerritorySelectionRuntime.state(server)
            .has(player.getUUID(), Mode.RESIZE, Forge1201TerritorySelectionRuntime.tick(server));
  }

  public static boolean start(ServerPlayer player, UUID territoryId) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(territoryId, "territoryId");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) {
      throw new IllegalStateException("resize session must start on server thread");
    }
    Forge1201TerritorySelectionRuntime.state(server).startResize(
        player.getUUID(),
        territoryId,
        dimension(player),
        Forge1201TerritorySelectionRuntime.tick(server));
    player.sendSystemMessage(Component.translatable("message.claim_wand.enter_resize_mode"));
    player.sendSystemMessage(Component.translatable("message.claim_wand.resize_instruction"));
    return true;
  }

  public static boolean select(ServerPlayer player, BlockPos clicked) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(clicked, "clicked");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) {
      throw new IllegalStateException("resize selection must run on server thread");
    }
    Session current = Forge1201TerritorySelectionRuntime.session(
        server, player.getUUID(), Mode.RESIZE).orElse(null);
    if (current == null) {
      player.sendSystemMessage(Component.translatable("message.claim.resize.no_session"));
      return false;
    }

    TerritorySelectionService.SelectionOutcome outcome =
        Forge1201TerritorySelectionRuntime.state(server).selectResize(
            player.getUUID(),
            current.territoryId(),
            dimension(player),
            point(clicked),
            Forge1201TerritorySelectionRuntime.tick(server),
            (first, second, excluded) -> {
              Forge1201TerritorySnapshotStore.ResizePrepareResult result =
                  prepare(player, current, first, second).result();
              return result == Forge1201TerritorySnapshotStore.ResizePrepareResult.OVERLAP;
            });
    return notifySelection(player, outcome);
  }

  public static int confirm(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    if (!server.isSameThread()) {
      throw new IllegalStateException("resize confirmation must run on server thread");
    }
    Session session = Forge1201TerritorySelectionRuntime.session(
        server, player.getUUID(), Mode.RESIZE).orElse(null);
    if (session == null || session.first().isEmpty() || session.second().isEmpty()) {
      player.sendSystemMessage(Component.translatable("message.claim.resize_failed"));
      return 0;
    }
    Forge1201TerritoryResizeTransaction.Outcome outcome =
        Forge1201TerritoryResizeTransaction.execute(
            EconomySavedData.getInstance(player.serverLevel()),
            Forge1201TerritorySnapshotStore.get(player.serverLevel()),
            player.getUUID(),
            session.territoryId(),
            session.dimensionId(),
            position(session.first().orElseThrow()),
            position(session.second().orElseThrow()),
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
      Forge1201TerritorySelectionRuntime.clear(server, player.getUUID());
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
    for (Session session : Forge1201TerritorySelectionRuntime.state(server)
        .clearForTerritorySessions(territoryId)) {
      cleared++;
      ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
      if (player != null) {
        try {
          player.sendSystemMessage(Component.translatable(
              "message.territory.remove.resize_cancelled", territoryName));
        } catch (RuntimeException failure) {
          LOGGER.warn(
              "resize deletion cleanup notification failed player={}",
              session.playerId(),
              failure);
        }
      }
    }
    return cleared;
  }

  private static boolean notifySelection(
      ServerPlayer player, TerritorySelectionService.SelectionOutcome outcome) {
    switch (outcome.result()) {
      case FIRST_SELECTED -> {
        Point first = outcome.session().first().orElseThrow();
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.first_position_set", first.x(), first.y(), first.z()));
        return true;
      }
      case SECOND_SELECTED -> {
        Session session = outcome.session();
        Forge1201TerritorySnapshotStore.ResizePrepareOutcome prepared =
            prepare(
                player,
                session,
                session.first().orElseThrow(),
                session.second().orElseThrow());
        if (prepared.result() != Forge1201TerritorySnapshotStore.ResizePrepareResult.READY
            && prepared.result() != Forge1201TerritorySnapshotStore.ResizePrepareResult.UNCHANGED) {
          Forge1201TerritorySelectionRuntime.clear(player.getServer(), player.getUUID());
          player.sendSystemMessage(Component.translatable(selectionFailureKey(prepared.result())));
          return false;
        }
        Point second = session.second().orElseThrow();
        Forge1201TerritoryEvents.showSelectionBoundary(
            player, position(session.first().orElseThrow()), position(second));
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.second_position_set", second.x(), second.y(), second.z()));
        if (prepared.plan() == null) {
          player.sendSystemMessage(Component.translatable("message.claim.resize.unchanged_preview"));
        } else {
          Forge1201TerritorySnapshotStore.ResizePlan plan = prepared.plan();
          if (plan.newArea() > plan.oldArea()) {
            player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_expand"));
            player.sendSystemMessage(Component.translatable(
                "message.claim_wand.resize_cost_details",
                plan.oldArea(),
                plan.newArea(),
                plan.charge()));
          } else {
            player.sendSystemMessage(Component.translatable("message.claim_wand.confirm_shrink"));
            player.sendSystemMessage(Component.translatable(
                "message.claim_wand.volume_change", plan.oldArea(), plan.newArea()));
          }
        }
        player.sendSystemMessage(Component.translatable("message.claim.resize.confirm_command"));
        return true;
      }
      case CANCELLED -> {
        player.sendSystemMessage(Component.translatable("message.claim_wand.cancel"));
        player.sendSystemMessage(Component.translatable("message.claim_wand.exit_resize_mode"));
        return true;
      }
      case DIMENSION_MISMATCH ->
          player.sendSystemMessage(Component.translatable("message.claim.resize.wrong_dimension"));
      case Y_MISMATCH ->
          player.sendSystemMessage(Component.translatable("message.claim_wand.y_mismatch_error"));
      case OVERLAP ->
          player.sendSystemMessage(Component.translatable("message.claim.resize.overlap"));
      case EXPIRED ->
          player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
      case NO_SESSION ->
          player.sendSystemMessage(Component.translatable("message.claim.resize.no_session"));
      case STATE_UNKNOWN ->
          player.sendSystemMessage(Component.translatable("message.claim.resize.state_unknown"));
    }
    return false;
  }

  private static Forge1201TerritorySnapshotStore.ResizePrepareOutcome prepare(
      ServerPlayer player, Session session, Point first, Point second) {
    return Forge1201TerritorySnapshotStore.get(player.serverLevel()).prepareResize(
        session.territoryId(),
        player.getUUID(),
        session.dimensionId(),
        position(first),
        position(second));
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

  private static Point point(BlockPos value) {
    return new Point(value.getX(), value.getY(), value.getZ());
  }

  private static Position position(Point value) {
    return new Position(value.x(), value.y(), value.z());
  }

  private static String dimension(ServerPlayer player) {
    return player.serverLevel().dimension().location().toString();
  }
}
