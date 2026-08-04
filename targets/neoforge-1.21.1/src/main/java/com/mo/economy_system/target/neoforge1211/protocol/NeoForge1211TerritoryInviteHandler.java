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

public final class NeoForge1211TerritoryInviteHandler {
  private static final TerritoryInviteStoreRegistry<MinecraftServer> STORES=new TerritoryInviteStoreRegistry<>();
  private static final Map<MinecraftServer,TerritoryInviteRateLimiter> LIMITERS=Collections.synchronizedMap(new WeakHashMap<>());
  private NeoForge1211TerritoryInviteHandler(){}
  public static void handle(InvitePlayerMessage message,IPayloadContext context){context.enqueueWork(()->{if(context.player() instanceof ServerPlayer sender)request(sender,message.territoryId(),message.targetPlayerId());});}
  public static void request(ServerPlayer sender,UUID territoryId,UUID targetId){MinecraftServer server=sender.getServer();TerritoryInviteStore store=STORES.get(server);TerritoryInviteRequestService service=new TerritoryInviteRequestService(id->{Territory t=TerritoryManager.getTerritoryByID(id);return t==null?Optional.empty():Optional.of(new TerritoryInviteRequestService.Territory(id,t.getOwnerUUID(),t.getName(),t.getAuthorizedPlayers().stream().map(p->p.getUuid()).collect(Collectors.toSet())));},id->{ServerPlayer p=server.getPlayerList().getPlayer(id);return p==null?Optional.empty():Optional.of(new TerritoryInviteRequestService.Player(id,p.getGameProfile().getName()));},store,LIMITERS.computeIfAbsent(server,k->new TerritoryInviteRateLimiter()),UUID::randomUUID);long tick=server.overworld().getGameTime();var outcome=service.create(sender.getUUID(),sender.getGameProfile().getName(),territoryId,targetId,tick);if(outcome.result()==TerritoryInviteResult.SUCCESS){TerritoryInvite i=outcome.invite();ServerPlayer target=server.getPlayerList().getPlayer(i.targetPlayerId());sender.sendSystemMessage(Component.translatable("message.invite.sent",i.targetPlayerName(),i.territoryName()));if(target!=null)target.sendSystemMessage(received(i));}else sender.sendSystemMessage(Component.translatable(key(outcome.result())));}
  private static Component received(TerritoryInvite i){String id=i.inviteId().toString();MutableComponent accept=Component.translatable("message.invite.accept").withStyle(s->s.withColor(0x55ff55).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/accept_invite "+id)));MutableComponent decline=Component.translatable("message.invite.decline").withStyle(s->s.withColor(0xff5555).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/decline_invite "+id)));return Component.translatable("message.invite.received",i.inviterName(),i.territoryName()).append(" ").append(accept).append(" ").append(decline);}
  private static String key(TerritoryInviteResult r){return "message.invite."+r.name().toLowerCase(Locale.ROOT);}
  public static TerritoryInviteStore store(MinecraftServer server){return STORES.get(server);}
}
