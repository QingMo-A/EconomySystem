package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.List;
import java.util.Objects;

/** Returns known economy account identities to territory management screens. */
public record ServerPlayerListResponseMessage(
        List<PlayerSummary> players
) implements EconomyNetworkMessage {
    public ServerPlayerListResponseMessage {
        players = List.copyOf(Objects.requireNonNull(players, "players"));
    }
}
