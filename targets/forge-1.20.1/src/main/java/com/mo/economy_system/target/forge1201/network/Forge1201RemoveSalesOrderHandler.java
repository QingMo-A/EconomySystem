package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.Forge1201Platform;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201RemoveSalesOrderHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  static void handle(RemoveSalesOrderMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context network = supplier.get();
    ServerPlayer actor = network.getSender();
    if (actor != null) execute(message, actor);
    network.setPacketHandled(true);
  }

  private static void execute(RemoveSalesOrderMessage message, ServerPlayer actor) {
    RemoveSalesOrderOutcome outcome;
    try {
      MarketSavedData data = MarketSavedData.getInstance(actor.serverLevel());
      Forge1201TransactionalInventoryAdapter materializer =
          new Forge1201TransactionalInventoryAdapter(actor);
      outcome =
          RemoveSalesOrderService.execute(
              message,
              new RemoveSalesOrderService.Context(
                  actor.getUUID(),
                  actor.hasPermissions(2),
                  materializer,
                  id -> resolve(actor, id),
                  new Repository(data),
                  reporter()));
    } catch (RuntimeException exception) {
      LOGGER.error(
          "Sales removal infrastructure failed actor={} tradeId={}",
          actor.getUUID(),
          message.tradeId(),
          exception);
      sendFailure(actor, message.tradeId());
      return;
    }
    boolean notifyOwner =
        outcome.result() == RemoveSalesOrderResult.SUCCESS
            && outcome.removedOrder().isPresent()
            && !actor.getUUID().equals(outcome.removedOrder().get().sellerId());
    IsolatedPostActions.runAll(
        MarketActionPostPlan.build(
            outcome.mutationState(),
            outcome.result() == RemoveSalesOrderResult.SUCCESS,
            notifyOwner,
            () -> Forge1201MarketInvalidation.broadcast(actor),
            () -> sendFeedback(actor, outcome),
            () -> notifyOwner(actor, outcome.removedOrder().orElseThrow())),
        (stage, exception) ->
            LOGGER.error(
                "Sales removal post-commit failure stage={} tradeId={}",
                stage,
                message.tradeId(),
                exception));
  }

  private static Optional<TransactionalInventory> resolve(ServerPlayer actor, UUID id) {
    ServerPlayer owner = actor.server.getPlayerList().getPlayer(id);
    return owner == null
        ? Optional.empty()
        : Optional.of(new Forge1201TransactionalInventoryAdapter(owner));
  }

  private static void sendFeedback(ServerPlayer actor, RemoveSalesOrderOutcome outcome) {
    if (outcome.result() != RemoveSalesOrderResult.SUCCESS) {
      actor.sendSystemMessage(
          Component.translatable(RemoveSalesOrderFeedback.key(outcome.result())));
      return;
    }
    MarketOrder order = outcome.removedOrder().orElseThrow();
    actor.sendSystemMessage(
        Component.translatable(
            RemoveSalesOrderFeedback.key(outcome.result()),
            displayName(actor, order),
            order.quantity()));
  }

  private static Object displayName(ServerPlayer player, MarketOrder order) {
    try {
      return Forge1201Platform.nativeItemStacks()
          .restoreSnapshot(order.item(), player.serverLevel().registryAccess())
          .orElseThrow()
          .getHoverName();
    } catch (RuntimeException exception) {
      LOGGER.error("Removal display item restore failed tradeId={}", order.tradeId(), exception);
      return order.item().itemId();
    }
  }

  private static void notifyOwner(ServerPlayer actor, MarketOrder order) {
    ServerPlayer owner = actor.server.getPlayerList().getPlayer(order.sellerId());
    if (owner != null)
      owner.sendSystemMessage(
          Component.translatable(
              "message.market.remove_sales.operator_notice",
              actor.getName(),
              displayName(actor, order),
              order.quantity()));
  }

  private static void sendFailure(ServerPlayer actor, UUID tradeId) {
    try {
      actor.sendSystemMessage(Component.translatable("message.market.remove_sales.failed"));
    } catch (RuntimeException exception) {
      LOGGER.error("Sales removal infrastructure feedback failed tradeId={}", tradeId, exception);
    }
  }

  private record Repository(MarketSavedData data) implements RemoveSalesOrderService.Repository {
    public MarketOrder find(UUID id) {
      return data.getOrder(id);
    }

    public SalesOrderRemovalResult removeSalesTransactional(UUID id) {
      return data.removeSalesTransactional(id);
    }
  }

  private static RemoveSalesOrderService.FailureReporter reporter() {
    return (t, a, o, p, s, r, i, d, e) ->
        LOGGER.error(
            "Sales removal failure tradeId={} actor={} owner={} operator={} stage={} result={}"
                + " inventoryRestore={} orderRestore={}",
            t,
            a,
            o,
            p,
            s,
            r,
            i,
            d,
            e);
  }
}
