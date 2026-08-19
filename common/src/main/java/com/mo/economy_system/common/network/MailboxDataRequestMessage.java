package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

public record MailboxDataRequestMessage(long requestId) implements EconomyNetworkMessage {
  public MailboxDataRequestMessage {
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
  }
}
