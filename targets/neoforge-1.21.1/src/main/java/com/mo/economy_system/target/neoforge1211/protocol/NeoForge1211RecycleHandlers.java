package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientRecycleState;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.common.recycle.RecycleResult;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.recycle.NeoForge1211RecyclerAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211RecycleHandlers {
  private NeoForge1211RecycleHandlers() {}
  public static void request(RecycleDataRequestMessage m, IPayloadContext c){c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p)sendData(p,m.requestId());});}
  public static void submit(RecycleSubmitMessage m, IPayloadContext c){c.enqueueWork(()->{if(!(c.player() instanceof ServerPlayer p))return;RecycleResult r;try{r=NeoForge1211RecyclerAdapter.recycle(p,m.itemId(),m.amount(),m.submissionId());}catch(RuntimeException e){r=new RecycleResult(RecycleResult.Status.PERSIST_FAILED,0,0,0,0);}EconomySystem_NetworkManager.sendToClient(p,new RecycleActionResponseMessage(m.requestId(),status(r.status()),r.acceptedAmount(),r.payout(),r.highQuotaRemaining(),r.status().name()));sendData(p,m.requestId());});}
  public static void data(RecycleDataResponseMessage m,IPayloadContext c){c.enqueueWork(()->ClientRecycleState.apply(m));}
  public static void action(RecycleActionResponseMessage m,IPayloadContext c){ }
  private static void sendData(ServerPlayer p,long id){long now=Math.max(1,System.currentTimeMillis());try{EconomySystem_NetworkManager.sendToClient(p,RecycleDataResponseMessage.data(id,now,NeoForge1211RecyclerAdapter.cycleEndsAt(p,now),NeoForge1211RecyclerAdapter.offers(p)));}catch(RuntimeException e){EconomySystem_NetworkManager.sendToClient(p,RecycleDataResponseMessage.error(id,now,"screen.recycle.sync_failed"));}}
  private static RecycleActionStatus status(RecycleResult.Status s){return switch(s){case SUCCESS->RecycleActionStatus.SUCCESS;case UNKNOWN_ITEM->RecycleActionStatus.UNKNOWN_ITEM;case INVALID_AMOUNT->RecycleActionStatus.INVALID_AMOUNT;case INSUFFICIENT_ITEMS->RecycleActionStatus.INSUFFICIENT_ITEMS;case HIGH_QUOTA_EXHAUSTED->RecycleActionStatus.HIGH_QUOTA_EXHAUSTED;case BALANCE_LIMIT->RecycleActionStatus.BALANCE_LIMIT;case PERSIST_FAILED->RecycleActionStatus.PERSIST_FAILED;case DUPLICATE_SUBMISSION->RecycleActionStatus.DUPLICATE_SUBMISSION;};}
}
