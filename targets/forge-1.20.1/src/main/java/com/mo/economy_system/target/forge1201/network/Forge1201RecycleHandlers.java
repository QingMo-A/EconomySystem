package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientRecycleState;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.common.recycle.RecycleResult;
import com.mo.economy_system.target.forge1201.recycle.Forge1201RecyclerAdapter;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

final class Forge1201RecycleHandlers {
  private Forge1201RecycleHandlers() {}
  static void request(RecycleDataRequestMessage m, Supplier<NetworkEvent.Context> s) {
    NetworkEvent.Context c=s.get(); ServerPlayer p=c.getSender(); if(p!=null)c.enqueueWork(()->sendData(p,m.requestId())); c.setPacketHandled(true);
  }
  static void submit(RecycleSubmitMessage m, Supplier<NetworkEvent.Context> s) {
    NetworkEvent.Context c=s.get(); ServerPlayer p=c.getSender(); if(p!=null)c.enqueueWork(()->{ RecycleResult r; try { r=Forge1201RecyclerAdapter.recycle(p,m.itemId(),m.amount(),m.submissionId()); } catch(RuntimeException e){ r=new RecycleResult(RecycleResult.Status.PERSIST_FAILED,0,0,0,0); } Forge1201NetworkChannel.sendToPlayer(p,new RecycleActionResponseMessage(m.requestId(),status(r.status()),r.acceptedAmount(),r.payout(),r.highQuotaRemaining(),r.status().name())); sendData(p,m.requestId()); }); c.setPacketHandled(true);
  }
  static void data(RecycleDataResponseMessage m,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientRecycleState.apply(m));c.setPacketHandled(true);}
  static void action(RecycleActionResponseMessage m,Supplier<NetworkEvent.Context>s){s.get().setPacketHandled(true);}
  private static void sendData(ServerPlayer p,long id){long now=Math.max(1,System.currentTimeMillis());try{Forge1201NetworkChannel.sendToPlayer(p,RecycleDataResponseMessage.data(id,now,Forge1201RecyclerAdapter.cycleEndsAt(p,now),Forge1201RecyclerAdapter.offers(p)));}catch(RuntimeException e){Forge1201NetworkChannel.sendToPlayer(p,RecycleDataResponseMessage.error(id,now,"screen.recycle.sync_failed"));}}
  private static RecycleActionStatus status(RecycleResult.Status s){return switch(s){case SUCCESS->RecycleActionStatus.SUCCESS;case UNKNOWN_ITEM->RecycleActionStatus.UNKNOWN_ITEM;case INVALID_AMOUNT->RecycleActionStatus.INVALID_AMOUNT;case INSUFFICIENT_ITEMS->RecycleActionStatus.INSUFFICIENT_ITEMS;case HIGH_QUOTA_EXHAUSTED->RecycleActionStatus.HIGH_QUOTA_EXHAUSTED;case BALANCE_LIMIT->RecycleActionStatus.BALANCE_LIMIT;case PERSIST_FAILED->RecycleActionStatus.PERSIST_FAILED;case DUPLICATE_SUBMISSION->RecycleActionStatus.DUPLICATE_SUBMISSION;};}
}
