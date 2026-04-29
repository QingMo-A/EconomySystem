package com.mo.economy_system.network.packets.playerattribute_system.infection_system;

import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_SyncInfectionData implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncInfectionData> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/infection_system/packet_sync_infection_data"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncInfectionData> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncInfectionData.encode(packet, buf), Packet_SyncInfectionData::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final float currentInfection;

    public Packet_SyncInfectionData(float currentInfection) {
        this.currentInfection = currentInfection;
    }

    public static void encode(Packet_SyncInfectionData packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.currentInfection);
    }

    public static Packet_SyncInfectionData decode(FriendlyByteBuf buf) {
        float current = buf.readFloat();
        return new Packet_SyncInfectionData(current);
    }

    public static void handle(Packet_SyncInfectionData packet, IPayloadContext context) {
        final float safeCurrentInfection = packet.currentInfection;

        context.enqueueWork(() -> processOnMainThread(safeCurrentInfection));
    }

    private static void processOnMainThread(float currentInfection) {
        new ClientRunnable(currentInfection).run();
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements Runnable {
        private final float currentInfection;

        public ClientRunnable(float currentInfection) {
            this.currentInfection = currentInfection;
        }

        @Override
        public void run() {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;

            PlayerInfectionManager.setCurrentInfectionClient(player, this.currentInfection);
        }
    }

    public float getCurrentInfection() {
        return currentInfection;
    }
}
