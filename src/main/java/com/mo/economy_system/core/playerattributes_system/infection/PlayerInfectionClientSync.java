package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.infection_system.Packet_SyncInfectionData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;

public class PlayerInfectionClientSync {
    public static void sendInfectionDataToClient(ServerPlayer player, int currentInfection) {
        Packet_SyncInfectionData packet = new Packet_SyncInfectionData(currentInfection);
        EconomySystem_NetworkManager.INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
