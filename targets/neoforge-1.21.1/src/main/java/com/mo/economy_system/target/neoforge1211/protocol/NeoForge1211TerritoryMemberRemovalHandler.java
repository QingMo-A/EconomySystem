package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.common.territory.*;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public final class NeoForge1211TerritoryMemberRemovalHandler {
  private static final Logger LOGGER=LogUtils.getLogger();
  private static final TerritoryMemberRemovalRateLimiterRegistry<MinecraftServer> LIMITERS=new TerritoryMemberRemovalRateLimiterRegistry<>();
  private NeoForge1211TerritoryMemberRemovalHandler(){}
  public static void handle(RemoveTerritoryMemberMessage message,IPayloadContext context){context.enqueueWork(()->{if(context.player() instanceof ServerPlayer sender)remove(sender,message);});}
  static void remove(ServerPlayer sender,RemoveTerritoryMemberMessage message){MinecraftServer server=sender.getServer();var overworld=server.overworld();if(overworld==null){sender.sendSystemMessage(Component.translatable("message.territory.member_remove.state_unknown"));return;}long tick=overworld.getGameTime();TerritoryMemberRemovalService service=new TerritoryMemberRemovalService(TerritoryManager::removeTerritoryMemberAuthoritatively,LIMITERS.get(server),(target,territory,t)->NeoForge1211TerritoryInviteHandler.store(server).discardPending(target,territory,t),(stage,player,territory,error)->LOGGER.warn("member removal stage={} player={} territory={}",stage,player,territory,error));var outcome=service.remove(sender.getUUID(),message.territoryId(),message.targetPlayerId(),tick);notify(server,sender,outcome);}
  private static void notify(MinecraftServer server,ServerPlayer sender,TerritoryMemberRemovalService.Outcome outcome){String key=switch(outcome.result()){case SUCCESS->"message.territory.member_remove.success";case TERRITORY_NOT_FOUND->"message.territory.member_remove.territory_not_found";case NO_PERMISSION->"message.territory.member_remove.no_permission";case CANNOT_REMOVE_OWNER->"message.territory.member_remove.cannot_remove_owner";case TARGET_NOT_MEMBER->"message.territory.member_remove.target_not_member";case RATE_LIMITED->"message.territory.member_remove.rate_limited";case PERSIST_FAILED->"message.territory.member_remove.persist_failed";case STATE_UNKNOWN->"message.territory.member_remove.state_unknown";};try{sender.sendSystemMessage(outcome.result()==TerritoryMemberRemovalService.Result.SUCCESS?Component.translatable(key,outcome.removedMember().targetPlayerName(),outcome.removedMember().territoryName()):Component.translatable(key));}catch(RuntimeException e){LOGGER.warn("member removal owner notification failed",e);}if(outcome.result()==TerritoryMemberRemovalService.Result.SUCCESS){ServerPlayer target=server.getPlayerList().getPlayer(outcome.removedMember().targetPlayerId());if(target!=null)try{target.sendSystemMessage(Component.translatable("message.territory.member_remove.target_notice",outcome.removedMember().territoryName()));}catch(RuntimeException e){LOGGER.warn("member removal target notification failed",e);}}}
}
