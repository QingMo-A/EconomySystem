package com.mo.economy_system.network.packets.npc_system;

import com.mo.economy_system.core.npc_system.NpcInteractionType;
import com.mo.economy_system.core.npc_system.NpcManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_NpcInteractionRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_NpcInteractionRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "npc_system/packet_npc_interaction_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_NpcInteractionRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_NpcInteractionRequest.encode(packet, buf), Packet_NpcInteractionRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final int npcId;
    private final int entityId;
    private final NpcInteractionType interactionType;

    public Packet_NpcInteractionRequest(int npcId, int entityId, NpcInteractionType interactionType) {
        this.npcId = npcId;
        this.entityId = entityId;
        this.interactionType = interactionType;
    }

    public static void encode(Packet_NpcInteractionRequest packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.npcId);
        buf.writeVarInt(packet.entityId);
        buf.writeEnum(packet.interactionType);
    }

    public static Packet_NpcInteractionRequest decode(FriendlyByteBuf buf) {
        return new Packet_NpcInteractionRequest(buf.readVarInt(), buf.readVarInt(), buf.readEnum(NpcInteractionType.class));
    }

    public static void handle(Packet_NpcInteractionRequest packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                NpcManager.handleInteraction(player, packet.npcId, packet.entityId, packet.interactionType);
            }
        });
    }
}
