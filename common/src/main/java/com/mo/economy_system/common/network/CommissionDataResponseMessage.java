package com.mo.economy_system.common.network;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.List;
import java.util.Objects;

public record CommissionDataResponseMessage(
    CommissionDataResponseKind kind,
    long requestId,
    long serverNowMillis,
    long nextRefreshAt,
    int maxActivePersonalCommissions,
    List<CommissionInstance> commissions,
    String errorKey) implements EconomyNetworkMessage {
  public CommissionDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0 || serverNowMillis < 0 || nextRefreshAt < 0
        || maxActivePersonalCommissions <= 0) {
      throw new IllegalArgumentException("invalid commission response metadata");
    }
    commissions = List.copyOf(Objects.requireNonNull(commissions, "commissions"));
    if (commissions.size() > EconomyNetworkLimits.MAX_COMMISSION_ENTRIES) {
      throw new IllegalArgumentException("too many commission entries");
    }
    errorKey = Objects.requireNonNullElse(errorKey, "");
    if (errorKey.length() > EconomyNetworkLimits.MAX_COMMISSION_TEXT_LENGTH) {
      throw new IllegalArgumentException("commission error key exceeds limit");
    }
    if (kind == CommissionDataResponseKind.ERROR && errorKey.isBlank()) {
      throw new IllegalArgumentException("commission error response requires an error key");
    }
  }

  public static CommissionDataResponseMessage data(long requestId, long now, long nextRefreshAt,
      int maxActive, List<CommissionInstance> commissions) {
    return new CommissionDataResponseMessage(CommissionDataResponseKind.DATA, requestId, now,
        nextRefreshAt, maxActive, commissions, "");
  }

  public static CommissionDataResponseMessage error(long requestId, long now, String errorKey) {
    return new CommissionDataResponseMessage(CommissionDataResponseKind.ERROR, requestId, now,
        now, 1, List.of(), errorKey);
  }
}
