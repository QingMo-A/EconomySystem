package com.mo.economy_system.common.network.commission_public;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

/** Requests one atomic partial or complete contribution to a public commission. */
public record PublicCommissionSubmitMessage(
    long requestId,
    UUID commissionId,
    UUID submissionId,
    int amount) implements EconomyNetworkMessage {
  public PublicCommissionSubmitMessage {
    if (requestId < 0) throw new IllegalArgumentException("public commission request id must be non-negative");
    Objects.requireNonNull(commissionId, "commissionId");
    Objects.requireNonNull(submissionId, "submissionId");
    if (amount <= 0 || amount > EconomyNetworkLimits.MAX_COMMISSION_SUBMIT_AMOUNT) {
      throw new IllegalArgumentException("invalid public commission submit amount");
    }
  }
}
