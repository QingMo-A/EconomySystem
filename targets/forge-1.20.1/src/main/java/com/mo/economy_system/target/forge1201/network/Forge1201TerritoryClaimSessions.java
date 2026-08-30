package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.TerritoryClaimService;
import com.mo.economy_system.common.territory.TerritoryGeometry;
import com.mo.economy_system.common.territory.TerritoryPricing;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.common.territory.TerritorySelectionService.Mode;
import com.mo.economy_system.common.territory.TerritorySelectionService.Point;
import com.mo.economy_system.common.territory.TerritorySelectionService.Result;
import com.mo.economy_system.common.territory.TerritorySelectionService.Session;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import com.mo.economy_system.target.forge1201.Forge1201TerritoryEvents;
import java.util.UUID;
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

/** Forge shell for common claim selection and confirmation policy. */
@Mod.EventBusSubscriber(modid = EconomySystemForge1201.MODID)
public final class Forge1201TerritoryClaimSessions {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201TerritoryClaimSessions() {}

  public static boolean hasSession(ServerPlayer player) {
    MinecraftServer server = player.getServer();
    return server != null
        && Forge1201TerritorySelectionRuntime.state(server)
            .has(player.getUUID(), Mode.CLAIM, Forge1201TerritorySelectionRuntime.tick(server));
  }

  public static boolean select(ServerPlayer player, BlockPos clicked) {
    MinecraftServer server = player.getServer();
    if (server == null || !server.isSameThread()) return false;
    TerritorySelectionService.SelectionOutcome outcome =
        Forge1201TerritorySelectionRuntime.state(server).selectClaim(
            player.getUUID(),
            dimension(player),
            point(clicked),
            Forge1201TerritorySelectionRuntime.tick(server),
            (first, second, excluded) ->
                Forge1201TerritorySnapshotStore.get(player.serverLevel()).overlaps(
                    new TerritoryClaimService.Request(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        "preview",
                        dimension(player),
                        position(first),
                        position(second))));
    notifySelection(player, outcome);
    return outcome.result() == Result.FIRST_SELECTED
        || outcome.result() == Result.SECOND_SELECTED
        || outcome.result() == Result.CANCELLED;
  }

  public static int confirm(ServerPlayer player, String name) {
    MinecraftServer server = player.getServer();
    if (server == null || !server.isSameThread()) return 0;
    Session selection = Forge1201TerritorySelectionRuntime.session(
        server, player.getUUID(), Mode.CLAIM).orElse(null);
    if (selection == null || selection.first().isEmpty() || selection.second().isEmpty()) {
      player.sendSystemMessage(Component.translatable("message.claim_wand.select_points"));
      return 0;
    }
    try {
      TerritoryClaimService.Request request = new TerritoryClaimService.Request(
          player.getUUID(),
          player.getGameProfile().getName(),
          name,
          selection.dimensionId(),
          position(selection.first().orElseThrow()),
          position(selection.second().orElseThrow()));
      EconomySavedData economy = EconomySavedData.getInstance(player.serverLevel());
      TerritoryClaimService.Outcome outcome = TerritoryClaimService.execute(
          request,
          new TerritoryClaimService.Balance() {
            public com.mo.economy_system.core.economy_system.BalanceMutationResult debitExact(
                UUID ownerId, int amount) {
              return economy.debitExact(ownerId, amount, "territory", "territory claim");
            }

            public com.mo.economy_system.core.economy_system.BalanceMutationResult creditExact(
                UUID ownerId, int amount) {
              return economy.creditExact(
                  ownerId, amount, "territory", "territory claim refund");
            }
          },
          new TerritoryClaimService.Repository() {
            public boolean overlaps(TerritoryClaimService.Request value) {
              return Forge1201TerritorySnapshotStore.get(player.serverLevel()).overlaps(value);
            }

            public TerritoryClaimService.RepositoryResult create(
                TerritoryClaimService.Request value, long area, int price) {
              return Forge1201TerritorySnapshotStore.get(player.serverLevel())
                  .create(value, area, price);
            }
          },
          (stage, owner, failure) -> LOGGER.warn(
              "territory claim stage={} player={}", stage, owner, failure));
      String key = switch (outcome.result()) {
        case SUCCESS -> "message.claim.success";
        case OVERLAP -> "message.claim_wand.overlap_error";
        case INSUFFICIENT_FUNDS -> "message.claim.insufficient_balance";
        case INVALID_INPUT, PRICE_OVERFLOW, PAYMENT_FAILED -> "message.claim.resize_failed";
        case PERSIST_FAILED -> "message.claim.resize.persist_failed";
        case REFUND_FAILED, STATE_UNKNOWN -> "message.claim.resize.state_unknown";
      };
      if (outcome.result() == TerritoryClaimService.Result.SUCCESS) {
        player.sendSystemMessage(Component.translatable(key, name, outcome.price()));
      } else if (outcome.result() == TerritoryClaimService.Result.INSUFFICIENT_FUNDS) {
        player.sendSystemMessage(Component.translatable(key, outcome.price()));
      } else {
        player.sendSystemMessage(Component.translatable(key));
      }
      if (outcome.result() == TerritoryClaimService.Result.SUCCESS
          || outcome.result() == TerritoryClaimService.Result.STATE_UNKNOWN
          || outcome.result() == TerritoryClaimService.Result.REFUND_FAILED) {
        Forge1201TerritorySelectionRuntime.clear(server, player.getUUID());
      }
      return outcome.result() == TerritoryClaimService.Result.SUCCESS ? 1 : 0;
    } catch (RuntimeException failure) {
      LOGGER.warn("territory claim confirmation failed player={}", player.getUUID(), failure);
      player.sendSystemMessage(Component.translatable("message.claim.resize.state_unknown"));
      return 0;
    }
  }

  private static void notifySelection(
      ServerPlayer player, TerritorySelectionService.SelectionOutcome outcome) {
    switch (outcome.result()) {
      case FIRST_SELECTED -> {
        Point first = outcome.session().first().orElseThrow();
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.first_position_set", first.x(), first.y(), first.z()));
      }
      case SECOND_SELECTED -> {
        Point first = outcome.session().first().orElseThrow();
        Point second = outcome.session().second().orElseThrow();
        Forge1201TerritoryEvents.showSelectionBoundary(player, position(first), position(second));
        long area = TerritoryGeometry.area(first.x(), first.z(), second.x(), second.z());
        long price = TerritoryPricing.saturatingPriceForArea(
            area, TerritoryPricing.pricePerCell());
        player.sendSystemMessage(Component.translatable(
            "message.claim_wand.second_position_set", second.x(), second.y(), second.z()));
        player.sendSystemMessage(Component.translatable("message.claim_wand.volume", area));
        player.sendSystemMessage(Component.translatable("message.claim_wand.price", price));
        player.sendSystemMessage(Component.translatable("message.claim_wand.instruction"));
      }
      case CANCELLED -> player.sendSystemMessage(Component.translatable("message.claim_wand.cancel"));
      case DIMENSION_MISMATCH -> player.sendSystemMessage(Component.translatable("message.claim.resize.wrong_dimension"));
      case Y_MISMATCH -> player.sendSystemMessage(Component.translatable("message.claim_wand.y_mismatch_error"));
      case OVERLAP -> player.sendSystemMessage(Component.translatable("message.claim_wand.overlap_error"));
      case EXPIRED -> player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
      case STATE_UNKNOWN -> {
        LOGGER.warn("territory claim preview state unknown player={}", player.getUUID());
        player.sendSystemMessage(Component.translatable("message.claim.resize.state_unknown"));
      }
      case NO_SESSION -> throw new IllegalStateException("claim selection did not create a session");
    }
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

  @SubscribeEvent
  public static void tick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    for (Session expired : Forge1201TerritorySelectionRuntime.expire(event.getServer())) {
      ServerPlayer player = event.getServer().getPlayerList().getPlayer(expired.playerId());
      if (player != null) {
        player.sendSystemMessage(Component.translatable("message.claim_wand.timeout"));
      }
    }
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
      Forge1201TerritorySelectionRuntime.clear(player.getServer(), player.getUUID());
    }
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    Forge1201TerritorySelectionRuntime.stop(event.getServer());
  }
}
