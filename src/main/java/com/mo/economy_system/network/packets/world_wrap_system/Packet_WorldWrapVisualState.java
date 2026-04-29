package com.mo.economy_system.network.packets.world_wrap_system;

import com.mo.economy_system.client.world_wrap_system.WorldWrapClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_WorldWrapVisualState implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_WorldWrapVisualState> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "world_wrap_system/packet_world_wrap_visual_state"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_WorldWrapVisualState> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_WorldWrapVisualState.encode(packet, buf), Packet_WorldWrapVisualState::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final boolean showBoundaryWarning;
    private final boolean playTransition;

    public Packet_WorldWrapVisualState(boolean showBoundaryWarning, boolean playTransition) {
        this.showBoundaryWarning = showBoundaryWarning;
        this.playTransition = playTransition;
    }

    public static void encode(Packet_WorldWrapVisualState packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.showBoundaryWarning);
        buf.writeBoolean(packet.playTransition);
    }

    public static Packet_WorldWrapVisualState decode(FriendlyByteBuf buf) {
        return new Packet_WorldWrapVisualState(buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(Packet_WorldWrapVisualState packet, IPayloadContext context) {
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_WorldWrapVisualState packet) {
        WorldWrapClientEffects.handleVisualState(packet.showBoundaryWarning, packet.playTransition);
    }
}
