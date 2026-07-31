package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.item.items.Item_ClaimWand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_ModifyMode implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ModifyMode> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_modify_mode"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ModifyMode> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ModifyMode.encode(packet, buf), Packet_ModifyMode::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final UUID territoryUUID;

    public Packet_ModifyMode(UUID territoryUUID) {
        this.territoryUUID = territoryUUID;
    }

    public static void encode(Packet_ModifyMode msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryUUID);
    }

    public static Packet_ModifyMode decode(FriendlyByteBuf buf) {
        return new Packet_ModifyMode(buf.readUUID());
    }

    public static void handle(Packet_ModifyMode msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryUUID);
            if (territory == null || !territory.isOwner(player.getUUID()) || !territory.getDimension().equals(player.serverLevel().dimension())) {
                player.sendSystemMessage(Component.literal("你没有权限修改这个领地。"));
                return;
            }

            Item_ClaimWand.startResizing(player, msg.territoryUUID);
        });
    }
}
