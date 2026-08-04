package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.territory.*;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class NeoForge1211TerritoryInviteHandler {
  private static final TerritoryInviteStoreRegistry<MinecraftServer> STORES=new TerritoryInviteStoreRegistry<>();
  private static final Logger LOGGER=LogUtils.getLogger();
  private static final Map<MinecraftServer,TerritoryInviteRateLimiter> LIMITERS=Collections.synchronizedMap(new WeakHashMap<>());
  private NeoForge1211TerritoryInviteHandler(){}
  public static void handle(InvitePlayerMessage message,IPayloadContext context){context.enqueueWork(()->{if(context.player() instanceof ServerPlayer sender)request(sender,message.territoryId(),message.targetPlayerId());});}
  public static void request(ServerPlayer sender,UUID territoryId,UUID targetId){MinecraftServer server=sender.getServer();TerritoryInviteStore store=STORES.get(server);TerritoryInviteRequestService service=new TerritoryInviteRequestService(id->{Territory t=TerritoryManager.getTerritoryByID(id);return t==null?Optional.empty():Optional.of(new TerritoryInviteRequestService.Territory(id,t.getOwnerUUID(),t.getName(),t.getAuthorizedPlayers().stream().map(p->p.getUuid()).collect(Collectors.toSet())));},id->{ServerPlayer p=server.getPlayerList().getPlayer(id);return p==null?Optional.empty():Optional.of(new TerritoryInviteRequestService.Player(id,p.getGameProfile().getName()));},store,LIMITERS.computeIfAbsent(server,k->new TerritoryInviteRateLimiter()),UUID::randomUUID,(stage,inviter,territory,target,error)->LOGGER.warn("invite stage={} inviter={} territory={} target={}",stage,inviter,territory,target,error),1200);long tick=server.overworld().getGameTime();var outcome=service.create(sender.getUUID(),sender.getGameProfile().getName(),territoryId,targetId,tick);try{if(outcome.result()==TerritoryInviteResult.SUCCESS){TerritoryInvite i=outcome.invite();ServerPlayer target=server.getPlayerList().getPlayer(i.targetPlayerId());sender.sendSystemMessage(Component.translatable("message.invite.sent",i.targetPlayerName(),i.territoryName()));if(target!=null)target.sendSystemMessage(received(i));}else sender.sendSystemMessage(Component.translatable(key(outcome.result())));}catch(RuntimeException error){LOGGER.warn("invite notification failed inviter={} territory={} target={}",sender.getUUID(),territoryId,targetId,error);}}
  private static Component received(TerritoryInvite i){String id=i.inviteId().toString();MutableComponent accept=Component.translatable("message.invite.accept").withStyle(s->s.withColor(0x55ff55).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/accept_invite "+id)));MutableComponent decline=Component.translatable("message.invite.decline").withStyle(s->s.withColor(0xff5555).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/decline_invite "+id)));return Component.translatable("message.invite.received",i.inviterName(),i.territoryName()).append(" ").append(accept).append(" ").append(decline);}
  private static String key(TerritoryInviteResult r){return switch(r){case SUCCESS->"message.invite.sent";case TERRITORY_NOT_FOUND->"message.invite.territory_not_found";case NO_PERMISSION->"message.invite.no_permission";case TARGET_OFFLINE->"message.invite.target_offline";case CANNOT_INVITE_OWNER->"message.invite.cannot_invite_owner";case CANNOT_INVITE_SELF->"message.invite.cannot_invite_self";case ALREADY_MEMBER->"message.invite.already_member";case ALREADY_PENDING->"message.invite.already_pending";case RATE_LIMITED->"message.invite.rate_limited";case STORE_FULL->"message.invite.store_full";case CREATE_FAILED->"message.invite.create_failed";};}
  public static TerritoryInviteStore store(MinecraftServer server){return STORES.get(server);}
  public static TerritoryInviteDecisionService decisions(MinecraftServer server){return new TerritoryInviteDecisionService(STORES.get(server),TerritoryManager::authorizeInvitedPlayer,(stage,invite,error)->LOGGER.error("invite decision stage={} invite={} territory={}",stage,invite.inviteId(),invite.territoryId(),error));}
}
