package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.List;
import java.util.Objects;

/** Loader-neutral balance response consumed by the home screen. */
public record BalanceResponseMessage(
        int balance,
        List<AccountBalance> accounts
) implements EconomyNetworkMessage {
    public BalanceResponseMessage {
        accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts"));
    }
}
