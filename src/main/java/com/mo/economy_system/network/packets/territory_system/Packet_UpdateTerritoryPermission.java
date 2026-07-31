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

public class Packet_UpdateTerritoryPermission implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_UpdateTerritoryPermission> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_update_territory_permission"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_UpdateTerritoryPermission> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_UpdateTerritoryPermission.encode(packet, buf), Packet_UpdateTerritoryPermission::decode);

    private final UUID territoryID;
    private final UUID targetUUID;
    private final String targetName;
    private final boolean allowed;

    public Packet_UpdateTerritoryPermission(UUID territoryID, UUID targetUUID, String targetName, boolean allowed) {
        this.territoryID = territoryID;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.allowed = allowed;
    }

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_UpdateTerritoryPermission msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUUID(msg.targetUUID);
        buf.writeUtf(msg.targetName);
        buf.writeBoolean(msg.allowed);
    }

    public static Packet_UpdateTerritoryPermission decode(FriendlyByteBuf buf) {
        return new Packet_UpdateTerritoryPermission(buf.readUUID(), buf.readUUID(), buf.readUtf(256), buf.readBoolean());
    }

    public static void handle(Packet_UpdateTerritoryPermission msg, IPayloadContext context) {
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
            if (territory.isOwner(msg.targetUUID)) {
                sender.sendSystemMessage(Component.literal("领主不需要额外授权。"));
                return;
            }

            if (TerritoryManager.setTerritoryPermission(msg.territoryID, msg.targetUUID, msg.targetName, msg.allowed)) {
                sender.sendSystemMessage(Component.literal(msg.allowed ? "已添加领地权限。" : "已移除领地权限。"));
            } else {
                sender.sendSystemMessage(Component.literal("领地权限修改失败。"));
            }
        });
    }
}
