package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record CommissionSubmitMessage(long requestId, UUID commissionId, UUID submissionId, int amount)
    implements EconomyNetworkMessage {
  public CommissionSubmitMessage {
    if (requestId < 0) throw new IllegalArgumentException("commission request id must be non-negative");
    Objects.requireNonNull(commissionId, "commissionId");
    Objects.requireNonNull(submissionId, "submissionId");
    if (amount <= 0 || amount > EconomyNetworkLimits.MAX_COMMISSION_SUBMIT_AMOUNT) {
      throw new IllegalArgumentException("invalid commission submit amount");
    }
  }
}
