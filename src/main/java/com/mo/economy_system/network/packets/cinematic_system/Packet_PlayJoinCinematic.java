package com.mo.economy_system.network.packets.cinematic_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.cinematic.JoinCinematicController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class Packet_PlayJoinCinematic implements CustomPacketPayload {
    public static final Type<Packet_PlayJoinCinematic> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "cinematic_system/packet_play_join_cinematic"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_PlayJoinCinematic> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_PlayJoinCinematic.encode(packet, buf), Packet_PlayJoinCinematic::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_PlayJoinCinematic packet, FriendlyByteBuf buf) {
    }

    public static Packet_PlayJoinCinematic decode(FriendlyByteBuf buf) {
        return new Packet_PlayJoinCinematic();
    }

    public static void handle(Packet_PlayJoinCinematic packet, IPayloadContext context) {
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(Packet_PlayJoinCinematic::handleClient);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        JoinCinematicController.requestStart();
    }
}
