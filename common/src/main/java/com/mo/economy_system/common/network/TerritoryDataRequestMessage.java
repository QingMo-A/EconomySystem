package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

public record TerritoryDataRequestMessage(long requestId) implements EconomyNetworkMessage {
  public TerritoryDataRequestMessage {
    if (requestId < 0) throw new IllegalArgumentException("negative territory request id");
  }
}
