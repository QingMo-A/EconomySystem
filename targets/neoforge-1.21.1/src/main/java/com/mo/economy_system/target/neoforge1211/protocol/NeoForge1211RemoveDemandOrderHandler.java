package com.mo.economy_system.target.neoforge1211.protocol;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.network.MarketInvalidationBroadcaster;
import com.mo.economy_system.utils.Util_Player;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
final class NeoForge1211RemoveDemandOrderHandler {
  static void handle(RemoveDemandOrderMessage m,IPayloadContext c){c.enqueueWork(()->execute(m,c));}
  private static void execute(RemoveDemandOrderMessage m,IPayloadContext c){if(!(c.player() instanceof ServerPlayer p))return;try{EconomySavedData a=EconomySavedData.getInstance(p.serverLevel());MarketSavedData d=MarketSavedData.getInstance(p.serverLevel());CancelDemandOrderOutcome o=CancelDemandOrderService.execute(m,new CancelDemandOrderService.Context(p.getUUID(),Util_Player.isOP(p),new Account(a),new Repo(d),NeoForge1211RemoveDemandOrderHandler::report));IsolatedPostActions.runAll(MarketActionPostPlan.build(o.mutationState(),true,false,()->MarketInvalidationBroadcaster.broadcast(p),()->p.sendSystemMessage(Component.translatable(CancelDemandOrderFeedback.messageKey(o.result()))),()->{}),(stage,e)->EconomySystem.LOGGER.error("Demand cancellation post action failed stage={}",stage,e));}catch(RuntimeException e){EconomySystem.LOGGER.error("Demand cancellation infrastructure failed",e);try{p.sendSystemMessage(Component.translatable("message.request.cancel_failed"));}catch(RuntimeException ignored){}}}
  private static void report(CancelDemandOrderFailure f){EconomySystem.LOGGER.error("Demand cancellation failed tradeId={} actorId={} requesterId={} operator={} stage={} result={} marketState={} removal={} refund={} restore={}",f.tradeId(),f.actorId(),f.requesterId(),f.operator(),f.stage(),f.result(),f.mutationState(),f.removalStatus(),f.refundResult(),f.restoreResult(),f.primaryError());}
  private record Account(EconomySavedData d) implements CancelDemandOrderService.Account{public BalanceMutationResult previewCreditExact(UUID id,int n){return d.previewCreditExact(id,n);}public BalanceMutationResult creditExact(UUID id,int n){return d.creditExact(id,n,"市场","取消求购单退款");}}
  private record Repo(MarketSavedData d) implements CancelDemandOrderService.Repository{public MarketOrder find(UUID id){return d.getOrder(id);}public DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(UUID id,MarketOrder e){return d.removeUndeliveredDemandIfUnchanged(id,e);}}
}
