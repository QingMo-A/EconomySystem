package com.mo.economy_system.network.packets.storybook_system;

import com.mo.economy_system.core.storybook_system.StoryBookDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Packet_UpdateStoryBookOrder {
    private final List<Integer> orderedFragmentIds;

    public Packet_UpdateStoryBookOrder(List<Integer> orderedFragmentIds) {
        this.orderedFragmentIds = orderedFragmentIds;
    }

    public static void encode(Packet_UpdateStoryBookOrder packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.orderedFragmentIds.size());
        for (Integer fragmentId : packet.orderedFragmentIds) {
            buf.writeVarInt(fragmentId);
        }
    }

    public static Packet_UpdateStoryBookOrder decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Integer> orderedIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            orderedIds.add(buf.readVarInt());
        }
        return new Packet_UpdateStoryBookOrder(orderedIds);
    }

    public static void handle(Packet_UpdateStoryBookOrder packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                StoryBookDataManager.updateFragmentOrderForPlayer(player.getUUID(), packet.orderedFragmentIds);
            }
        });
        context.setPacketHandled(true);
    }
}
