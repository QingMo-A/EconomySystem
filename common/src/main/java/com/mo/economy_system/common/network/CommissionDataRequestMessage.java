package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

public record CommissionDataRequestMessage(long requestId) implements EconomyNetworkMessage {
  public CommissionDataRequestMessage {
    if (requestId < 0) throw new IllegalArgumentException("commission request id must be non-negative");
  }
}
