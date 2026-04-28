package com.mo.economy_system.network.packets.world_wrap_system;

import com.mo.economy_system.client.world_wrap_system.WorldWrapClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_WorldWrapVisualState {
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

    public static void handle(Packet_WorldWrapVisualState packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> handleClient(packet));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_WorldWrapVisualState packet) {
        WorldWrapClientEffects.handleVisualState(packet.showBoundaryWarning, packet.playTransition);
    }
}
