package com.mo.economy_system.platform.network;

import java.util.UUID;

/** Loader-neutral message delivery operations used by shared code. */
public interface EconomyNetworkBridge {
    void sendToServer(EconomyNetworkMessage message);

    /** Sends to an online player identified by stable player UUID. */
    void sendToPlayer(UUID playerId, EconomyNetworkMessage message);
}
