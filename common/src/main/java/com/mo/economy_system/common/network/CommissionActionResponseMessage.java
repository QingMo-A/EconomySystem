package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

public record CommissionActionResponseMessage(long requestId, CommissionSubmitStatus status,
                                               String message) implements EconomyNetworkMessage {
  public CommissionActionResponseMessage {
    if (requestId < 0) throw new IllegalArgumentException("commission request id must be non-negative");
    Objects.requireNonNull(status, "status");
    message = Objects.requireNonNullElse(message, "");
    if (message.length() > EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH) {
      throw new IllegalArgumentException("commission action message exceeds limit");
    }
  }
}
