package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_TransferTerritoryOwnership implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_TransferTerritoryOwnership> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_transfer_territory_ownership"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_TransferTerritoryOwnership> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_TransferTerritoryOwnership.encode(packet, buf), Packet_TransferTerritoryOwnership::decode);

    private final UUID territoryID;
    private final UUID targetUUID;
    private final String targetName;

    public Packet_TransferTerritoryOwnership(UUID territoryID, UUID targetUUID, String targetName) {
        this.territoryID = territoryID;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
    }

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_TransferTerritoryOwnership msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUUID(msg.targetUUID);
        buf.writeUtf(msg.targetName);
    }

    public static Packet_TransferTerritoryOwnership decode(FriendlyByteBuf buf) {
        return new Packet_TransferTerritoryOwnership(buf.readUUID(), buf.readUUID(), buf.readUtf(256));
    }

    public static void handle(Packet_TransferTerritoryOwnership msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (sender == null) {
                return;
            }

            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NOT_EXIST));
                return;
            }
            if (!territory.isOwner(sender.getUUID())) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION));
                return;
            }
            if (sender.getUUID().equals(msg.targetUUID)) {
                sender.sendSystemMessage(Component.literal("不能将领地转让给自己。"));
                return;
            }

            String territoryName = territory.getName();
            if (TerritoryManager.transferTerritory(msg.territoryID, msg.targetUUID, msg.targetName)) {
                sender.sendSystemMessage(Component.literal("已将领地 " + territoryName + " 转让给 " + msg.targetName + "。"));
                ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetUUID);
                if (target != null) {
                    target.sendSystemMessage(Component.literal("你已成为领地 " + territoryName + " 的新领主。"));
                }
            } else {
                sender.sendSystemMessage(Component.literal("领地转让失败。"));
            }
        });
    }
}
