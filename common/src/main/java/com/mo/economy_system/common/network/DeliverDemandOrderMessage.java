package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

/** Demand fulfillment intent. quantity=0 is the compatibility sentinel for whole remaining order. */
public record DeliverDemandOrderMessage(UUID tradeId, int quantity) implements EconomyNetworkMessage {
  public DeliverDemandOrderMessage {
    Objects.requireNonNull(tradeId, "tradeId");
    if (quantity < 0) throw new IllegalArgumentException("quantity must be non-negative");
  }

  public DeliverDemandOrderMessage(UUID tradeId) {
    this(tradeId, 0);
  }
}
