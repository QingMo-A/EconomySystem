package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryPermissionAction;
import com.mo.economy_system.core.territory_system.TerritoryPermissionLevel;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_UpdateTerritoryRule implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_UpdateTerritoryRule> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_update_territory_rule"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_UpdateTerritoryRule> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_UpdateTerritoryRule.encode(packet, buf), Packet_UpdateTerritoryRule::decode);

    private final UUID territoryID;
    private final TerritoryPermissionAction action;
    private final TerritoryPermissionLevel level;

    public Packet_UpdateTerritoryRule(UUID territoryID, TerritoryPermissionAction action, TerritoryPermissionLevel level) {
        this.territoryID = territoryID;
        this.action = action;
        this.level = level;
    }

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_UpdateTerritoryRule msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeEnum(msg.action);
        buf.writeEnum(msg.level);
    }

    public static Packet_UpdateTerritoryRule decode(FriendlyByteBuf buf) {
        return new Packet_UpdateTerritoryRule(buf.readUUID(), buf.readEnum(TerritoryPermissionAction.class), buf.readEnum(TerritoryPermissionLevel.class));
    }

    public static void handle(Packet_UpdateTerritoryRule msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (sender == null) return;

            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NOT_EXIST));
                return;
            }
            if (!territory.isOwner(sender.getUUID())) {
                sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION));
                return;
            }
            if (TerritoryManager.setTerritoryRule(msg.territoryID, msg.action, msg.level)) {
                sender.sendSystemMessage(Component.literal("已设置 " + msg.action.getDisplayName() + " 权限为 " + msg.level.getDisplayName()));
            }
        });
    }
}
