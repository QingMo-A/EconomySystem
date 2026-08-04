package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.common.territory.*;
import com.mo.economy_system.core.territory_system.*;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211TerritoryInviteHandler;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class Command_Territory {
  private Command_Territory() {}
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("setbackpoint").executes(Command_Territory::setBackPoint));
    dispatcher.register(Commands.literal("accept_invite").executes(c->decide(c.getSource(),null,true)).then(Commands.argument("inviteId",StringArgumentType.word()).executes(c->decide(c.getSource(),StringArgumentType.getString(c,"inviteId"),true))));
    dispatcher.register(Commands.literal("decline_invite").executes(c->decide(c.getSource(),null,false)).then(Commands.argument("inviteId",StringArgumentType.word()).executes(c->decide(c.getSource(),StringArgumentType.getString(c,"inviteId"),false))));
    dispatcher.register(Commands.literal("invite").then(Commands.argument("player",EntityArgument.player()).executes(c->{ServerPlayer sender=c.getSource().getPlayerOrException();ServerPlayer target=EntityArgument.getPlayer(c,"player");Vec3 pos=sender.position();Territory territory=TerritoryManager.getTerritoryAtIgnoreY(sender.serverLevel().dimension(),(int)Math.floor(pos.x),(int)Math.floor(pos.z));if(territory==null||!territory.isOwner(sender.getUUID())){sender.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NOT_IN_TERRITORY));return 0;}NeoForge1211TerritoryInviteHandler.request(sender,territory.getTerritoryID(),target.getUUID());return 1;})));
  }
  private static int setBackPoint(CommandContext<CommandSourceStack> context){CommandSourceStack source=context.getSource();if(!(source.getEntity() instanceof ServerPlayer player)){source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));return 0;}Vec3 p=player.position();int x=(int)Math.floor(p.x),y=(int)Math.floor(p.y),z=(int)Math.floor(p.z);Territory territory=TerritoryManager.getTerritoryAtIgnoreY(player.serverLevel().dimension(),x,z);if(territory==null||!territory.isOwner(player.getUUID())){source.sendFailure(Component.translatable(Util_MessageKeys.TERRITORY_SETBACKPOINT_NO_PERMISSION));return 0;}territory.setBackpoint(new BlockPos(x,y,z));TerritoryManager.markDirty();source.sendSuccess(()->Component.translatable(Util_MessageKeys.TERRITORY_SETBACKPOINT_SUCCESS,x,y,z),true);return 1;}
  private static int decide(CommandSourceStack source,String raw,boolean accept){if(!(source.getEntity() instanceof ServerPlayer player)){source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));return 0;}long tick=source.getServer().overworld().getGameTime();TerritoryInviteDecisionService service=NeoForge1211TerritoryInviteHandler.decisions(source.getServer());UUID id=resolve(service,player.getUUID(),raw,tick,source);if(id==null)return 0;TerritoryInviteDecisionService.DecisionOutcome outcome=accept?service.accept(id,player.getUUID(),player.getGameProfile().getName(),tick):service.decline(id,player.getUUID(),tick);if((accept&&outcome.result()==TerritoryInviteDecisionService.Result.ACCEPTED)||(!accept&&outcome.result()==TerritoryInviteDecisionService.Result.DECLINED)){String key=accept?"message.invite.accepted":"message.invite.declined";source.sendSuccess(()->Component.translatable(key,outcome.invite().territoryName()),false);notifyInviter(source,player,outcome,accept);return 1;}source.sendFailure(Component.translatable(key(outcome.result())));return 0;}
  private static UUID resolve(TerritoryInviteDecisionService service,UUID player,String raw,long tick,CommandSourceStack source){if(raw!=null){try{return UUID.fromString(raw);}catch(IllegalArgumentException e){source.sendFailure(Component.translatable("message.invite.not_found"));return null;}}TerritoryInviteStore.SoleResult sole=service.resolveSole(player,tick);if(sole.status()==TerritoryInviteStore.SoleStatus.NONE)source.sendFailure(Component.translatable("message.invite.no_pending"));else if(sole.status()==TerritoryInviteStore.SoleStatus.MULTIPLE)source.sendFailure(Component.translatable("message.invite.multiple_pending"));return sole.inviteId();}
  private static void notifyInviter(CommandSourceStack source,ServerPlayer actor,TerritoryInviteDecisionService.DecisionOutcome outcome,boolean accept){try{ServerPlayer inviter=source.getServer().getPlayerList().getPlayer(outcome.invite().inviterId());if(inviter!=null)inviter.sendSystemMessage(Component.translatable(accept?"message.invite.accepted_by":"message.invite.declined_by",actor.getGameProfile().getName(),outcome.invite().territoryName()));}catch(RuntimeException ignored){}}
  private static String key(TerritoryInviteDecisionService.Result r){return switch(r){case NOT_FOUND->"message.invite.not_found";case NOT_TARGET->"message.invite.not_target";case TERRITORY_NOT_FOUND->"message.invite.territory_not_found";case OWNER_CHANGED->"message.invite.owner_changed";case ALREADY_MEMBER->"message.invite.already_member";case PERSIST_FAILED->"message.invite.persist_failed";case STATE_UNKNOWN->"message.invite.state_unknown";case BUSY->"message.invite.busy";case MULTIPLE_PENDING->"message.invite.multiple_pending";case ACCEPTED->"message.invite.accepted";case DECLINED->"message.invite.declined";};}
}
