package com.mo.economy_system.network.packets.playerattribute_system.infection_system;

import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_SyncInfectionData {
    private final int currentInfection;

    public Packet_SyncInfectionData(int currentInfection) {
        this.currentInfection = currentInfection;
    }

    public static void encode(Packet_SyncInfectionData packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.currentInfection);
    }

    public static Packet_SyncInfectionData decode(FriendlyByteBuf buf) {
        int current = buf.readInt();
        return new Packet_SyncInfectionData(current);
    }

    public static void handle(Packet_SyncInfectionData packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final int safeCurrentInfection = packet.currentInfection;

        context.enqueueWork(() -> processOnMainThread(safeCurrentInfection));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(int currentInfection) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(currentInfection));
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final int currentInfection;

        public ClientRunnable(int currentInfection) {
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

    public int getCurrentInfection() {
        return currentInfection;
    }
}
