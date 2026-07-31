package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.Objects;
import java.util.UUID;

/** Requests a server-authoritative transfer to one online player. */
public record TransferMessage(UUID targetPlayerId, int amount) implements EconomyNetworkMessage {
    public TransferMessage {
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
    }
}
