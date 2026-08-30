package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

public record RecycleActionResponseMessage(long requestId, RecycleActionStatus status,
                                           int acceptedAmount, int payout,
                                           int highQuotaRemaining, String message)
    implements EconomyNetworkMessage {
  public RecycleActionResponseMessage {
    if (requestId < 0 || acceptedAmount < 0 || payout < 0 || highQuotaRemaining < 0) throw new IllegalArgumentException("invalid recycle action response");
    Objects.requireNonNull(status, "status");
    message = Objects.requireNonNullElse(message, "");
    if (message.length() > EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH) throw new IllegalArgumentException("recycle message too long");
  }
}
