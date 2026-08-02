package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.utils.Util_Player;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201RemoveDemandOrderHandler {
  private static final Logger LOGGER = LogUtils.getLogger();

  static void handle(
      RemoveDemandOrderMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) execute(message, player);
    context.setPacketHandled(true);
  }

  private static void execute(RemoveDemandOrderMessage message, ServerPlayer player) {
    try {
      EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
      MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
      CancelDemandOrderOutcome outcome = CancelDemandOrderService.execute(
          message,
          new CancelDemandOrderService.Context(
              player.getUUID(), Util_Player.isOP(player), new Account(accounts),
              new Repository(market), Forge1201RemoveDemandOrderHandler::report));
      boolean success = outcome.result() == CancelDemandOrderResult.SUCCESS;
      IsolatedPostActions.runAll(
          MarketActionPostPlan.build(
              outcome.mutationState(), success, false,
              () -> Forge1201MarketInvalidation.broadcast(player),
              () -> player.sendSystemMessage(
                  Component.translatable(CancelDemandOrderFeedback.messageKey(outcome.result()))),
              () -> {}),
          (stage, error) -> LOGGER.error(
              "Demand cancellation post action failed stage={}", stage, error));
    } catch (RuntimeException error) {
      LOGGER.error("Demand cancellation infrastructure failed", error);
      try { player.sendSystemMessage(Component.translatable("message.request.cancel_failed")); }
      catch (RuntimeException ignored) { /* Feedback failure must not escape the network task. */ }
    }
  }

  private static void report(CancelDemandOrderFailure failure) {
    String message = "Demand cancellation failed tradeId={} actorId={} requesterId={} operator={} stage={} result={} marketState={} removal={} refund={} restore={}";
    RuntimeException error = failure.combinedError();
    if (error == null)
      LOGGER.error(message, failure.tradeId(), failure.actorId(), failure.requesterId(),
          failure.operator(), failure.stage(), failure.result(), failure.mutationState(),
          failure.removalStatus(), failure.refundResult(), failure.restoreResult());
    else
      LOGGER.error(message, failure.tradeId(), failure.actorId(), failure.requesterId(),
          failure.operator(), failure.stage(), failure.result(), failure.mutationState(),
          failure.removalStatus(), failure.refundResult(), failure.restoreResult(), error);
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
