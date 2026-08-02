package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record DeliverDemandOrderMessage(UUID tradeId) implements EconomyNetworkMessage {
  public DeliverDemandOrderMessage {
    Objects.requireNonNull(tradeId, "tradeId");
  }
}
