package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.platform.EconomyServices;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211ConfirmDemandOrderHandler {
  private NeoForge1211ConfirmDemandOrderHandler() {}

  public static void handle(ConfirmDemandOrderMessage m, IPayloadContext c) {
    c.enqueueWork(() -> execute(m, c));
  }

  private static void execute(ConfirmDemandOrderMessage m, IPayloadContext c) {
    if (!(c.player() instanceof ServerPlayer actor)) return;
    ConfirmDemandOrderOutcome o;
    try {
      MarketSavedData data = MarketSavedData.getInstance(actor.serverLevel());
      NeoForge1211TransactionalInventoryAdapter materializer =
          new NeoForge1211TransactionalInventoryAdapter(actor);
      o =
          ConfirmDemandOrderService.execute(
              m,
              new ConfirmDemandOrderService.Context(
                  actor.getUUID(),
                  actor.hasPermissions(2),
                  materializer,
                  id -> resolve(actor, id),
                  new Repo(data),
                  ConfirmDemandOrderService.FailureReporter.noop()));
    } catch (RuntimeException e) {
      EconomySystem.LOGGER.error(
          "Demand confirmation infrastructure failed actor={} tradeId={}",
          actor.getUUID(),
          m.tradeId(),
          e);
      sendGenericFailure(actor, m.tradeId());
      return;
    }
    boolean notifyOwner =
        o.result() == ConfirmDemandOrderResult.SUCCESS
            && o.confirmedOrder().isPresent()
            && !actor.getUUID().equals(o.confirmedOrder().get().sellerId());
    IsolatedPostActions.runAll(
        MarketActionPostPlan.build(
            o.mutationState(),
            o.result() == ConfirmDemandOrderResult.SUCCESS,
            notifyOwner,
            () -> MarketInvalidationBroadcaster.broadcast(actor),
            () -> feedback(actor, o),
            () -> notice(actor, o.confirmedOrder().orElseThrow())),
        (stage, exception) ->
            EconomySystem.LOGGER.error(
                "Demand confirmation post-commit failure stage={} tradeId={}",
                stage,
                m.tradeId(),
                exception));
  }

  private static Optional<TransactionalInventory> resolve(ServerPlayer actor, UUID id) {
    ServerPlayer owner = actor.server.getPlayerList().getPlayer(id);
    return owner == null
        ? Optional.empty()
        : Optional.of(new NeoForge1211TransactionalInventoryAdapter(owner));
  }

  private static Object name(ServerPlayer p, MarketOrder o) {
    try {
      return EconomyServices.platform()
          .itemStacks()
          .restoreSnapshot(o.item(), p.registryAccess())
          .orElseThrow()
          .getHoverName();
    } catch (RuntimeException e) {
      EconomySystem.LOGGER.error(
          "Demand confirmation display restore failed tradeId={}", o.tradeId(), e);
      return o.item().itemId();
    }
  }

  private static void feedback(ServerPlayer actor, ConfirmDemandOrderOutcome o) {
    if (o.result() != ConfirmDemandOrderResult.SUCCESS)
      actor.sendSystemMessage(Component.translatable(ConfirmDemandOrderFeedback.key(o.result())));
    else {
      MarketOrder order = o.confirmedOrder().orElseThrow();
      actor.sendSystemMessage(
          Component.translatable(
              ConfirmDemandOrderFeedback.key(o.result()), name(actor, order), order.quantity()));
    }
  }

  private static void notice(ServerPlayer actor, MarketOrder o) {
    ServerPlayer owner = actor.server.getPlayerList().getPlayer(o.sellerId());
    if (owner != null)
      owner.sendSystemMessage(
          Component.translatable(
              "message.market.confirm_demand.operator_notice",
              actor.getName(),
              name(actor, o),
              o.quantity()));
  }

  private static void sendGenericFailure(ServerPlayer actor, UUID tradeId) {
    try {
      actor.sendSystemMessage(Component.translatable("message.market.confirm_demand.failed"));
    } catch (RuntimeException exception) {
      EconomySystem.LOGGER.error(
          "Demand confirmation infrastructure feedback failed tradeId={}", tradeId, exception);
    }
  }

  private record Repo(MarketSavedData d) implements ConfirmDemandOrderService.Repository {
    public MarketOrder find(UUID id) {
      return d.getOrder(id);
    }

    public DeliveredDemandRemovalResult removeDeliveredDemandTransactional(UUID id) {
      return d.removeDeliveredDemandTransactional(id);
    }
  }
}
