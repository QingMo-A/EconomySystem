package com.mo.economy_system.ui.territory;

import java.util.Objects;
import java.util.UUID;

public record MemberRow(UUID playerId, String playerName) {
    public MemberRow {
        Objects.requireNonNull(playerId, "playerId");
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("playerName cannot be blank");
        }
    }
}
