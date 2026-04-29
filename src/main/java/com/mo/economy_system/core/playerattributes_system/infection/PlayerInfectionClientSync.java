package com.mo.economy_system.core.playerattributes_system.infection;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.infection_system.Packet_SyncInfectionData;
import net.minecraft.server.level.ServerPlayer;

public class PlayerInfectionClientSync {
    public static void sendInfectionDataToClient(ServerPlayer player, float currentInfection) {
        Packet_SyncInfectionData packet = new Packet_SyncInfectionData(currentInfection);
        EconomySystem_NetworkManager.sendToClient(packet, player);
    }
}
