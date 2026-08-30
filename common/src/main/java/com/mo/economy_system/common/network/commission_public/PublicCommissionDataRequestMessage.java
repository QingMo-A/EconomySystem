package com.mo.economy_system.common.network.commission_public;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;

/** Requests the current server-authoritative public commission snapshot. */
public record PublicCommissionDataRequestMessage(long requestId) implements EconomyNetworkMessage {
  public PublicCommissionDataRequestMessage {
    if (requestId < 0) throw new IllegalArgumentException("public commission request id must be non-negative");
  }
}
