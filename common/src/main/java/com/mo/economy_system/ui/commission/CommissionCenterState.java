package com.mo.economy_system.ui.commission;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CommissionCenterState(
    List<CommissionInstance> commissions,
    long nextRefreshAt,
    long serverNowMillis,
    int maxActivePersonalCommissions,
    UUID selectedCommissionId,
    ScreenState screenState,
    String errorKey,
    long requestId) {
  public CommissionCenterState {
    commissions = List.copyOf(Objects.requireNonNull(commissions, "commissions"));
    Objects.requireNonNull(screenState, "screenState");
    errorKey = errorKey == null ? "" : errorKey;
    if (maxActivePersonalCommissions <= 0) throw new IllegalArgumentException("max active must be positive");
  }

  public CommissionInstance selected() {
    return commissions.stream().filter(value -> value.commissionId().equals(selectedCommissionId()))
        .findFirst().orElse(null);
  }
}
