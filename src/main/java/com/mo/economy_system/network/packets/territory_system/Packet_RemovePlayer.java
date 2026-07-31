package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_RemovePlayer implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RemovePlayer> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_remove_player"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RemovePlayer> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RemovePlayer.encode(packet, buf), Packet_RemovePlayer::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final UUID territoryID;
    private final UUID playerUUID;

    public Packet_RemovePlayer(UUID territoryID, UUID playerUUID) {
        this.territoryID = territoryID;
        this.playerUUID = playerUUID;
    }

    public static void encode(Packet_RemovePlayer msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUUID(msg.playerUUID);
    }

    public static Packet_RemovePlayer decode(FriendlyByteBuf buf) {
        return new Packet_RemovePlayer(buf.readUUID(), buf.readUUID());
    }

    public static void handle(Packet_RemovePlayer msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (sender == null) return;

            var territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NOT_EXIST));
                return;
            }

            if (!territory.isOwner(sender.getUUID())) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NO_PERMISSION));
                return;
            }

            // 获取目标玩家
            ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.playerUUID);

            if (target != null) {
                target.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_PLAYER_KICKED, territory.getName()));
            }

            territory.removeAuthorizedPlayer(msg.playerUUID);
            TerritoryManager.markDirty();

            sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_PLAYER_REMOVED));
        });
    }
}
