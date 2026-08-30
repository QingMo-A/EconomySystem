package com.mo.economy_system.common.network.commission_public;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

/** Bounded result for a public commission contribution. */
public record PublicCommissionActionResponseMessage(
    long requestId,
    PublicCommissionSubmitStatus status,
    int acceptedAmount,
    int payout,
    String message) implements EconomyNetworkMessage {
  public PublicCommissionActionResponseMessage {
    if (requestId < 0) throw new IllegalArgumentException("public commission request id must be non-negative");
    Objects.requireNonNull(status, "status");
    if (acceptedAmount < 0 || acceptedAmount > EconomyNetworkLimits.MAX_COMMISSION_SUBMIT_AMOUNT) {
      throw new IllegalArgumentException("invalid accepted public commission amount");
    }
    if (payout < 0) throw new IllegalArgumentException("invalid public commission payout");
    message = Objects.requireNonNullElse(message, "");
    if (message.length() > EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH) {
      throw new IllegalArgumentException("public commission action message exceeds limit");
    }
  }
}
