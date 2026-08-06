package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

public record DeliveryBoxDataRequestMessage(long requestId) implements EconomyNetworkMessage {
  public DeliveryBoxDataRequestMessage {
    if (requestId < 0) throw new IllegalArgumentException("negative delivery request id");
  }
}
