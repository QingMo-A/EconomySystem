package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Requests the current player's balance and, optionally, the account list. */
public record BalanceRequestMessage(boolean includeAccountList) implements EconomyNetworkMessage {
    public BalanceRequestMessage() {
        this(true);
    }
}
