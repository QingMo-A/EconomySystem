package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record ConfirmDemandOrderMessage(UUID tradeId) implements EconomyNetworkMessage {
    public ConfirmDemandOrderMessage { Objects.requireNonNull(tradeId, "tradeId"); }
}
