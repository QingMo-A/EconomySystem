package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

import java.util.Objects;
import java.util.UUID;

/** Protocol 12 intent: the server resolves every mutable order field by trade ID. */
public record PurchaseSalesOrderMessage(UUID tradeId) implements EconomyNetworkMessage {
    public PurchaseSalesOrderMessage {
        Objects.requireNonNull(tradeId, "tradeId");
    }
}
