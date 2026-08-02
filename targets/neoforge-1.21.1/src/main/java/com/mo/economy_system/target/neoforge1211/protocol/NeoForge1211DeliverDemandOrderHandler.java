package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.core.economy_system.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class NeoForge1211DeliverDemandOrderHandler {
  static void handle(DeliverDemandOrderMessage m, IPayloadContext c) {
    c.enqueueWork(() -> execute(m, c));
  }

  private static void execute(DeliverDemandOrderMessage m, IPayloadContext c) {
    if (!(c.player() instanceof ServerPlayer p)) return;
    DemandOrderDeliveryOutcome o;
    EconomySavedData a;
    try {
      MarketSavedData d = MarketSavedData.getInstance(p.serverLevel());
      a = EconomySavedData.getInstance(p.serverLevel());
      NeoForge1211TransactionalInventoryAdapter i =
          new NeoForge1211TransactionalInventoryAdapter(p);
      o =
          DemandOrderDeliveryService.execute(
              m,
              new DemandOrderDeliveryService.Context(
                  p.getUUID(),
                  i,
                  i,
                  new Account(a, p),
                  new Repo(d),
                  NeoForge1211DeliverDemandOrderHandler::reportFailure));
    } catch (RuntimeException e) {
      EconomySystem.LOGGER.error("Demand delivery infrastructure failed", e);
      try {
        p.sendSystemMessage(Component.translatable("message.market.deliver_demand.failed"));
      } catch (RuntimeException feedbackError) {
        EconomySystem.LOGGER.error("Demand delivery infrastructure feedback failed", feedbackError);
      }
      return;
    }
    IsolatedPostActions.runAll(
        MarketActionPostPlan.build(
            o.mutationState(),
            o.result() == DemandOrderDeliveryResult.SUCCESS,
            o.result() == DemandOrderDeliveryResult.SUCCESS,
            () -> MarketInvalidationBroadcaster.broadcast(p),
            () -> feedback(p, o),
            () -> notify(p, a, o.deliveredOrder().orElseThrow())),
        (s, e) -> EconomySystem.LOGGER.error("Demand delivery post action failed stage={}", s, e));
  }

  private static void feedback(ServerPlayer p, DemandOrderDeliveryOutcome outcome) {
    if (outcome.result() == DemandOrderDeliveryResult.SUCCESS) {
      MarketOrder order = outcome.deliveredOrder().orElseThrow();
      p.sendSystemMessage(
          Component.translatable(
              DemandOrderDeliveryFeedback.translationKey(outcome.result()),
              displayName(p, order),
              order.quantity(),
              order.totalPrice()));
      return;
    }
    p.sendSystemMessage(
        Component.translatable(DemandOrderDeliveryFeedback.translationKey(outcome.result())));
  }

  private static void notify(ServerPlayer p, EconomySavedData a, MarketOrder o) {
    Component m =
        Component.translatable(
            "message.market.deliver_demand.requester_notice",
            p.getName(),
            displayName(p, o),
            o.quantity());
    ServerPlayer r = p.server.getPlayerList().getPlayer(o.sellerId());
    if (r != null) r.sendSystemMessage(m);
    else a.storeOfflineMessage(o.sellerId(), m.getString());
  }

  private static Object displayName(ServerPlayer player, MarketOrder order) {
    try {
      return ((net.minecraft.world.item.ItemStack)
              new NeoForge1211TransactionalInventoryAdapter(player).restore(order))
          .getHoverName();
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error("Demand delivery display name restore failed tradeId={}", order.tradeId(), error);
      return order.item().itemId();
    }
  }

  private static void reportFailure(DemandOrderDeliveryFailure failure) {
    EconomySystem.LOGGER.error(
        "Demand delivery failed tradeId={} supplierId={} requesterId={} stage={} result={} marketState={} transitionStatus={} inventoryRemovalFailureRestored={} paymentReverted={} inventoryRestored={}",
        failure.tradeId(), failure.supplierId(), failure.requesterId(), failure.stage(), failure.result(),
        failure.mutationState(), failure.transitionStatus(),
        failure.inventoryRemovalFailureRestored(), failure.paymentReversalSucceeded(),
        failure.inventoryRollbackSucceeded());
    logError("primary", failure.primaryError());
    logError("inventory", failure.inventoryError());
    logError("payment", failure.paymentError());
    logError("repository", failure.repositoryError());
  }

  private static void logError(String kind, RuntimeException error) {
    if (error != null) EconomySystem.LOGGER.error("Demand delivery {} failure", kind, error);
  }

  private record Repo(MarketSavedData d) implements DemandOrderDeliveryService.Repository {
    public MarketOrder find(java.util.UUID id) {
      return d.getOrder(id);
    }

    public DemandDeliveryTransition markDemandDeliveredIfUnchanged(
        java.util.UUID id, MarketOrder e) {
      return d.markDemandDeliveredIfUnchanged(id, e);
    }
  }

  private record Account(EconomySavedData d, ServerPlayer p)
      implements DemandOrderDeliveryService.Account {
    public BalanceMutationResult previewCreditExact(int n) {
      return d.previewCreditExact(p.getUUID(), n);
    }

    public BalanceMutationResult creditExact(int n) {
      return d.creditExact(p.getUUID(), n, "市场", "交付求购单");
    }

    public BalanceMutationResult debitExact(int n) {
      return d.debitExact(p.getUUID(), n, "市场", "交付求购单回滚");
    }
  }
}
