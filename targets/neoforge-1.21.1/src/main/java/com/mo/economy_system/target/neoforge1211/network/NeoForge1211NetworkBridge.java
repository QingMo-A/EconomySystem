package com.mo.economy_system.target.neoforge1211.network;

import com.mo.economy_system.platform.network.EconomyNetworkBridge;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageType;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211MessageBindings;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211MessageCodecs;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211PayloadAdapters;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForge1211NetworkBridge implements EconomyNetworkBridge {
    @Override
    public void sendToServer(EconomyNetworkMessage message) {
        PacketDistributor.sendToServer(asPayload(message));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, EconomyNetworkMessage message) {
        PacketDistributor.sendToPlayer(player, asPayload(message));
    }

    private static CustomPacketPayload asPayload(EconomyNetworkMessage message) {
        if (message instanceof CustomPacketPayload payload) {
            return payload;
        }

        return commonPayload(message);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CustomPacketPayload commonPayload(EconomyNetworkMessage message) {
        EconomyMessageType messageType = NeoForge1211MessageBindings.registry()
                .typeOf((Class) message.getClass());
        if (messageType != null && NeoForge1211MessageCodecs.supports(messageType)) {
            return NeoForge1211PayloadAdapters.payload(messageType, message);
        }
        throw new IllegalArgumentException(
                "NeoForge 1.21.1 message has no CustomPacketPayload adapter: "
                        + message.getClass().getName()
        );
    }
}
