package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.core.territory_system.InviteManager;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Player;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class Command_Territory {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setbackpoint")
                        .executes(Command_Territory::setBackPoint)
        );
        dispatcher.register(
                Commands.literal("accept_invite")
                        .executes(context -> handleAccept(context.getSource()))
        );

        dispatcher.register(
                Commands.literal("decline_invite")
                        .executes(context -> handleDecline(context.getSource()))
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

                                    Territory territory = TerritoryManager.getTerritoryAtIgnoreY(x, z);
                                    if (territory == null || !territory.isOwner(sender.getUUID())) {
                                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NOT_IN_TERRITORY));
                                        return 0;
                                    }

                                    InviteManager.sendInvite(sender.getUUID(), target.getUUID(), territory.getTerritoryID());

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
        Territory territory = TerritoryManager.getTerritoryAtIgnoreY(x, z);
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

    private static int handleAccept(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));
            return 0;
        }

        InviteManager.Invite invite = InviteManager.getInvite(player.getUUID());
        if (invite == null) {
            source.sendFailure(Component.translatable(Util_MessageKeys.INVITE_NO_PENDING));
            return 0;
        }

        Territory territory = TerritoryManager.getTerritoryByID(invite.getTerritoryID());
        if (territory == null) {
            source.sendFailure(Component.translatable(Util_MessageKeys.INVITE_TARGET_NOT_FOUND));
            InviteManager.removeInvite(player.getUUID());
            return 0;
        }

        territory.addAuthorizedPlayer(player.getUUID(), Util_Player.getPlayerNameByUUID(source.getServer(), player.getUUID()));
        TerritoryManager.markDirty();
        InviteManager.removeInvite(player.getUUID());

        source.sendSuccess(() -> Component.translatable(Util_MessageKeys.INVITE_ACCEPTED, territory.getName()), true);
        return 1;
    }

    private static int handleDecline(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable(Util_MessageKeys.COMMAND_PLAYER_ONLY));
            return 0;
        }

        InviteManager.Invite invite = InviteManager.getInvite(player.getUUID());
        if (invite == null) {
            source.sendFailure(Component.translatable(Util_MessageKeys.INVITE_DECLINE_NO_PENDING));
            return 0;
        }

        InviteManager.removeInvite(player.getUUID());
        source.sendSuccess(() -> Component.translatable(Util_MessageKeys.INVITE_DECLINED), true);
        return 1;
    }
}

