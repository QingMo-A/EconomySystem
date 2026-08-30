package com.mo.economy_system.common.network.commission_public;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable, bounded public commission snapshot sent to a client. */
public record PublicCommissionDataResponseMessage(
    PublicCommissionDataResponseKind kind,
    long requestId,
    long serverNowMillis,
    List<PublicCommission> commissions,
    String errorKey) implements EconomyNetworkMessage {
  public PublicCommissionDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0 || serverNowMillis < 0) {
      throw new IllegalArgumentException("invalid public commission response metadata");
    }
    commissions = List.copyOf(Objects.requireNonNull(commissions, "commissions"));
    if (commissions.size() > EconomyNetworkLimits.MAX_COMMISSION_ENTRIES) {
      throw new IllegalArgumentException("too many public commission entries");
    }
    if (new HashSet<>(commissions.stream().map(PublicCommission::commissionId).toList()).size()
        != commissions.size()) {
      throw new IllegalArgumentException("duplicate public commission id");
    }
    errorKey = Objects.requireNonNullElse(errorKey, "");
    if (errorKey.length() > EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH) {
      throw new IllegalArgumentException("public commission error key exceeds limit");
    }
    if (kind == PublicCommissionDataResponseKind.ERROR && errorKey.isBlank()) {
      throw new IllegalArgumentException("public commission error response requires an error key");
    }
  }

  public static PublicCommissionDataResponseMessage data(long requestId, long now,
                                                           List<PublicCommission> commissions) {
    return new PublicCommissionDataResponseMessage(PublicCommissionDataResponseKind.DATA,
        requestId, now, commissions, "");
  }

  public static PublicCommissionDataResponseMessage error(long requestId, long now,
                                                            String errorKey) {
    return new PublicCommissionDataResponseMessage(PublicCommissionDataResponseKind.ERROR,
        requestId, now, List.of(), errorKey);
  }
}
