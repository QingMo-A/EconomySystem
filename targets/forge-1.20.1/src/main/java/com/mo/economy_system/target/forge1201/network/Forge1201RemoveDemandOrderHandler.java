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
  private static final Logger LOGGER=LogUtils.getLogger();
  static void handle(RemoveDemandOrderMessage m,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();ServerPlayer p=c.getSender();if(p!=null)execute(m,p);c.setPacketHandled(true);}
  private static void execute(RemoveDemandOrderMessage m,ServerPlayer p){try{EconomySavedData a=EconomySavedData.getInstance(p.serverLevel());MarketSavedData d=MarketSavedData.getInstance(p.serverLevel());CancelDemandOrderOutcome o=CancelDemandOrderService.execute(m,new CancelDemandOrderService.Context(p.getUUID(),Util_Player.isOP(p),new Account(a),new Repo(d),Forge1201RemoveDemandOrderHandler::report));IsolatedPostActions.runAll(MarketActionPostPlan.build(o.mutationState(),true,false,()->Forge1201MarketInvalidation.broadcast(p),()->p.sendSystemMessage(Component.translatable(CancelDemandOrderFeedback.messageKey(o.result()))),()->{}),(stage,e)->LOGGER.error("Demand cancellation post action failed stage={}",stage,e));}catch(RuntimeException e){LOGGER.error("Demand cancellation infrastructure failed",e);try{p.sendSystemMessage(Component.translatable("message.request.cancel_failed"));}catch(RuntimeException ignored){}}}
  private static void report(CancelDemandOrderFailure f){LOGGER.error("Demand cancellation failed tradeId={} actorId={} requesterId={} operator={} stage={} result={} marketState={} removal={} refund={} restore={}",f.tradeId(),f.actorId(),f.requesterId(),f.operator(),f.stage(),f.result(),f.mutationState(),f.removalStatus(),f.refundResult(),f.restoreResult(),f.primaryError());}
  private record Account(EconomySavedData d) implements CancelDemandOrderService.Account{public BalanceMutationResult previewCreditExact(UUID id,int n){return d.previewCreditExact(id,n);}public BalanceMutationResult creditExact(UUID id,int n){return d.creditExact(id,n,"市场","取消求购单退款");}}
  private record Repo(MarketSavedData d) implements CancelDemandOrderService.Repository{public MarketOrder find(UUID id){return d.getOrder(id);}public DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(UUID id,MarketOrder e){return d.removeUndeliveredDemandIfUnchanged(id,e);}}
}
