package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record RecycleSubmitMessage(long requestId, UUID submissionId, String itemId, int amount)
    implements EconomyNetworkMessage {
  public RecycleSubmitMessage {
    if (requestId < 0) throw new IllegalArgumentException("requestId must be non-negative");
    Objects.requireNonNull(submissionId, "submissionId");
    itemId = Objects.requireNonNull(itemId, "itemId").trim();
    if (itemId.isEmpty() || amount <= 0 || amount > EconomyNetworkLimits.MAX_RECYCLE_SUBMIT_AMOUNT) throw new IllegalArgumentException("invalid recycle submission");
  }
}
