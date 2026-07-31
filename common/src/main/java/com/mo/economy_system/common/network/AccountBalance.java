package com.mo.economy_system.common.network;

import java.util.Objects;

/** Loader-neutral account entry used by balance response messages. */
public record AccountBalance(String playerName, int balance) {
    public AccountBalance {
        Objects.requireNonNull(playerName, "playerName");
    }
}
