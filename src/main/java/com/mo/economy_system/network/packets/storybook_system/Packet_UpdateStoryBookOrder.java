package com.mo.economy_system.network.packets.storybook_system;

import com.mo.economy_system.core.storybook_system.StoryBookDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class Packet_UpdateStoryBookOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_UpdateStoryBookOrder> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "storybook_system/packet_update_story_book_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_UpdateStoryBookOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_UpdateStoryBookOrder.encode(packet, buf), Packet_UpdateStoryBookOrder::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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

    public static void handle(Packet_UpdateStoryBookOrder packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                StoryBookDataManager.updateFragmentOrderForPlayer(player.getUUID(), packet.orderedFragmentIds);
            }
        });
    }
}
