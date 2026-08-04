package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211TerritoryInviteHandler;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class Command_Territory {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setbackpoint")
                        .executes(Command_Territory::setBackPoint)
        );
        dispatcher.register(
                Commands.literal("accept_invite")
                        .executes(context -> handleAccept(context.getSource(), null))
                        .then(Commands.argument("inviteId", StringArgumentType.word()).executes(context -> handleAccept(context.getSource(), StringArgumentType.getString(context, "inviteId"))))
        );

        dispatcher.register(
                Commands.literal("decline_invite")
                        .executes(context -> handleDecline(context.getSource(), null))
                        .then(Commands.argument("inviteId", StringArgumentType.word()).executes(context -> handleDecline(context.getSource(), StringArgumentType.getString(context, "inviteId"))))
        );
        dispatcher.register(
                Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer sender = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");

                                    Vec3 senderPos = sender.position();
                                    int x = (int) Math.floor(senderPos.x);
                                    int z = (int) Math.floor(senderPos.z);

                                    Territory territory = TerritoryManager.getTerritoryAtIgnoreY(sender.serverLevel().dimension(), x, z);
                                    if (territory == null || !territory.isOwner(sender.getUUID())) {
                                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NOT_IN_TERRITORY));
                                        return 0;
                                    }

                                    NeoForge1211TerritoryInviteHandler.request(sender, territory.getTerritoryID(), target.getUUID());
                                    // The shared service already sends inviteId-bearing actions.
                                    if (sender != null) return 1;

                                    Component acceptButton = Component.translatable(Util_MessageKeys.INVITE_ACCEPT_BUTTON)
                                            .withStyle(style -> style
                                                    .withColor(0x55FF55)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept_invite"))
                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§e点击同意!"))));

                                    Component declinedButton = Component.translatable(Util_MessageKeys.INVITE_DECLINE_BUTTON)
                                            .withStyle(style -> style
                                                    .withColor(0xFF5555)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/decline_invite"))
                                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§e点击拒绝!"))));

                                    sender.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_SENT_TO_PLAYER, target.getName().getString()));
                                    target.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_RECEIVED_PLAYER, sender.getName().getString(), territory.getName())
                                            .append(" ")
                                            .append(acceptButton)
                                            .append(" ")
                                            .append(declinedButton));
                                    return 1;
                                }))
        );

    }

    private static int setBackPoint(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));
            return 0;
        }

        // 获取玩家位置
        Vec3 playerPos = player.position();
        int x = (int) Math.floor(playerPos.x);
        int y = (int) Math.floor(playerPos.y);
        int z = (int) Math.floor(playerPos.z);

        // 检查玩家是否在自己的领地
        Territory territory = TerritoryManager.getTerritoryAtIgnoreY(player.serverLevel().dimension(), x, z);
        if (territory == null || !territory.isOwner(player.getUUID())) {
            source.sendFailure(Component.translatable(Util_MessageKeys.TERRITORY_SETBACKPOINT_NO_PERMISSION));
            return 0;
        }

        // 设置回城点
        territory.setBackpoint(new BlockPos(x, y, z));
        TerritoryManager.markDirty(); // 如果有保存机制，标记数据需要保存

        source.sendSuccess(() -> Component.translatable(Util_MessageKeys.TERRITORY_SETBACKPOINT_SUCCESS, x, y, z), true);
        return 1;
    }

    private static int handleAccept(CommandSourceStack source, String rawId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));
            return 0;
        }

        long tick=source.getServer().overworld().getGameTime();
        TerritoryInviteDecisionService service=new TerritoryInviteDecisionService(NeoForge1211TerritoryInviteHandler.store(source.getServer()),TerritoryManager::authorizeInvitedPlayer);
        UUID id=resolveInviteId(service,player.getUUID(),rawId,tick,source);if(id==null)return 0;
        TerritoryInviteDecisionService.Result result=service.accept(id,player.getUUID(),player.getGameProfile().getName(),tick);
        if(result==TerritoryInviteDecisionService.Result.ACCEPTED){source.sendSuccess(()->Component.translatable("message.invite.accepted"),false);return 1;}
        source.sendFailure(Component.translatable("message.invite."+result.name().toLowerCase(java.util.Locale.ROOT)));return 0;
    }

    private static int handleDecline(CommandSourceStack source, String rawId) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));
            return 0;
        }

        long tick=source.getServer().overworld().getGameTime();TerritoryInviteDecisionService service=new TerritoryInviteDecisionService(NeoForge1211TerritoryInviteHandler.store(source.getServer()),TerritoryManager::authorizeInvitedPlayer);UUID id=resolveInviteId(service,player.getUUID(),rawId,tick,source);if(id==null)return 0;TerritoryInviteDecisionService.Result result=service.decline(id,player.getUUID(),tick);if(result==TerritoryInviteDecisionService.Result.DECLINED){source.sendSuccess(()->Component.translatable("message.invite.declined"),false);return 1;}source.sendFailure(Component.translatable("message.invite."+result.name().toLowerCase(java.util.Locale.ROOT)));return 0;
    }

    private static UUID resolveInviteId(TerritoryInviteDecisionService service,UUID player,String raw,long tick,CommandSourceStack source){if(raw!=null){try{return UUID.fromString(raw);}catch(IllegalArgumentException e){source.sendFailure(Component.translatable("message.invite.not_found"));return null;}}int count=service.pending(player,tick);if(count==0){source.sendFailure(Component.translatable("message.invite.not_found"));return null;}if(count>1){source.sendFailure(Component.translatable("message.invite.multiple_pending"));return null;}return service.sole(player,tick).orElse(null);}
}

