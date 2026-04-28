package com.mo.economy_system.network.packets.npc_system;

import com.mo.economy_system.core.npc_system.NpcInteractionType;
import com.mo.economy_system.core.npc_system.NpcManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_NpcInteractionRequest {
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

    public static void handle(Packet_NpcInteractionRequest packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NpcManager.handleInteraction(player, packet.npcId, packet.entityId, packet.interactionType);
            }
        });
        context.setPacketHandled(true);
    }
}
