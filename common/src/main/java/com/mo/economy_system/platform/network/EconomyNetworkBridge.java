package com.mo.economy_system.platform.network;

import net.minecraft.server.level.ServerPlayer;

/** Loader-neutral message delivery operations used by shared code. */
public interface EconomyNetworkBridge {
    void sendToServer(EconomyNetworkMessage message);

    void sendToPlayer(ServerPlayer player, EconomyNetworkMessage message);
}
