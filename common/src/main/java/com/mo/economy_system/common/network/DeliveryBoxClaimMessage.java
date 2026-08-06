package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record DeliveryBoxClaimMessage(UUID entryId, long requestId)
    implements EconomyNetworkMessage {
  public DeliveryBoxClaimMessage {
    Objects.requireNonNull(entryId, "entryId");
    if (requestId < 0) throw new IllegalArgumentException("negative delivery request id");
  }
}
