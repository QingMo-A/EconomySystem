package com.mo.economy_system.network;

import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211MessageBindings;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211ProtocolRegistrar;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class EconomySystem_NetworkManager {
    public static void register(IEventBus modEventBus) {
        // Fail bootstrap immediately if this target drifts from the canonical
        // NeoForge 1.21.1 protocol manifest.
        NeoForge1211MessageBindings.registry();
        modEventBus.addListener(EconomySystem_NetworkManager::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        NeoForge1211ProtocolRegistrar.register(event);
    }

    public static void sendToClient(EconomyNetworkMessage packet, ServerPlayer player) {
        EconomyServices.platform().network().sendToPlayer(player.getUUID(), packet);
    }

    public static void sendToClient(ServerPlayer player, EconomyNetworkMessage packet) {
        sendToClient(packet, player);
    }

    public static void sendToServer(EconomyNetworkMessage packet) {
        EconomyServices.platform().network().sendToServer(packet);
    }
}
