package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.target.neoforge1211.player.NeoForge1211PlayerLookup;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class NeoForge1211RemoveDemandOrderHandler {
  static void handle(RemoveDemandOrderMessage message, IPayloadContext context) {
    context.enqueueWork(() -> execute(message, context));
  }

  private static void execute(RemoveDemandOrderMessage message, IPayloadContext context) {
    if (!(context.player() instanceof ServerPlayer player)) return;
    try {
      EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
      MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
      CancelDemandOrderOutcome outcome = CancelDemandOrderService.execute(
          message,
          new CancelDemandOrderService.Context(
              player.getUUID(), NeoForge1211PlayerLookup.isOperator(player), new Account(accounts),
              new Repository(market), NeoForge1211RemoveDemandOrderHandler::report));
      boolean success = outcome.result() == CancelDemandOrderResult.SUCCESS;
      IsolatedPostActions.runAll(
          MarketActionPostPlan.build(
              outcome.mutationState(), success, false,
              () -> MarketInvalidationBroadcaster.broadcast(player),
              () -> player.sendSystemMessage(
                  Component.translatable(CancelDemandOrderFeedback.messageKey(outcome.result()))),
              () -> {}),
          (stage, error) -> EconomySystem.LOGGER.error(
              "Demand cancellation post action failed stage={}", stage, error));
    } catch (RuntimeException error) {
      EconomySystem.LOGGER.error("Demand cancellation infrastructure failed", error);
      try { player.sendSystemMessage(Component.translatable("message.request.cancel_failed")); }
      catch (RuntimeException ignored) { /* Feedback failure must not escape the network task. */ }
    }
  }

  private static void report(CancelDemandOrderFailure failure) {
    String message = "Demand cancellation failed tradeId={} actorId={} requesterId={} operator={} stage={} result={} marketState={} removal={} refund={} restore={}";
    RuntimeException error = failure.combinedError();
    if (error == null)
      EconomySystem.LOGGER.error(message, failure.tradeId(), failure.actorId(),
          failure.requesterId(), failure.operator(), failure.stage(), failure.result(),
          failure.mutationState(), failure.removalStatus(), failure.refundResult(),
          failure.restoreResult());
    else
      EconomySystem.LOGGER.error(message, failure.tradeId(), failure.actorId(),
          failure.requesterId(), failure.operator(), failure.stage(), failure.result(),
          failure.mutationState(), failure.removalStatus(), failure.refundResult(),
          failure.restoreResult(), error);
  }

  private record Account(EconomySavedData data) implements CancelDemandOrderService.Account {
    public BalanceMutationResult previewCreditExact(UUID ownerId, int amount) {
      return data.previewCreditExact(ownerId, amount);
    }
    public BalanceMutationResult creditExact(UUID ownerId, int amount) {
      return data.creditExact(ownerId, amount, "市场", "取消求购单退款");
    }
  }

  private record Repository(MarketSavedData data) implements CancelDemandOrderService.Repository {
    public MarketOrder find(UUID tradeId) { return data.getOrder(tradeId); }
    public DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(
        UUID tradeId, MarketOrder expectedOrder) {
      return data.removeUndeliveredDemandIfUnchanged(tradeId, expectedOrder);
    }
  }
}
