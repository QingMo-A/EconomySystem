package com.mo.economy_system.common.network;

import java.util.Objects;
import java.util.UUID;

/** Loader-neutral player identity used by account and territory screens. */
public record PlayerSummary(UUID playerId, String playerName) {
    public PlayerSummary {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
