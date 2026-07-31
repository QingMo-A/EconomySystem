package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.territory_system.InviteManager;
import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_InvitePlayer implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_InvitePlayer> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_invite_player"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_InvitePlayer> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_InvitePlayer.encode(packet, buf), Packet_InvitePlayer::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final UUID territoryID;
    private final String playerName;

    public Packet_InvitePlayer(UUID territoryID, String playerName) {
        this.territoryID = territoryID;
        this.playerName = playerName;
    }

    public static void encode(Packet_InvitePlayer msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUtf(msg.playerName);
    }

    public static Packet_InvitePlayer decode(FriendlyByteBuf buf) {
        return new Packet_InvitePlayer(buf.readUUID(), buf.readUtf(256));
    }

    public static void handle(Packet_InvitePlayer msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer inviter = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (inviter == null) return;

            // 获取领地信息
            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null || !territory.isOwner(inviter.getUUID())) {
                inviter.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NO_PERMISSION));
                return;
            }

            // 获取目标玩家
            ServerPlayer target = inviter.server.getPlayerList().getPlayerByName(msg.playerName);
            if (target == null) {
                inviter.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_PLAYER_OFFLINE, msg.playerName));
                return;
            }

            if (!(target.getUUID().equals(territory.getOwnerUUID()))){
                System.out.println(target.getUUID() + "\n" + territory.getOwnerUUID());
                for (PlayerInfo playerInfo : territory.getAuthorizedPlayers()) {
                    if (playerInfo.getUuid().equals(target.getUUID())) {
                        inviter.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_ALREADY_MEMBER, msg.playerName));
                        return;
                    }
                }
                // 添加邀请到管理器
                InviteManager.sendInvite(inviter.getUUID(), target.getUUID(), msg.territoryID);

                // 发送邀请消息
                inviter.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_SENT, msg.playerName));

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

                target.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_RECEIVED, inviter.getName().getString(), territory.getName())
                        .append(" ")
                        .append(acceptButton)
                        .append(" ")
                        .append(declinedButton));
                target.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_INSTRUCTIONS));
            } else {
                inviter.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_SELF_ERROR));
            }

        });
    }
}
