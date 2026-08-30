package com.mo.economy_system.ui.commission_public;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable presentation state for the server-wide public commission page. */
public record PublicCommissionCenterState(
    List<PublicCommission> commissions,
    long serverNowMillis,
    UUID selectedCommissionId,
    ScreenState screenState,
    String errorKey,
    long requestId,
    boolean submitInFlight,
    PublicCommissionSubmitStatus lastSubmitStatus,
    String actionMessage) {
  public PublicCommissionCenterState {
    commissions = List.copyOf(Objects.requireNonNull(commissions, "commissions"));
    if (serverNowMillis < 0 || requestId < -1) {
      throw new IllegalArgumentException("invalid public commission state metadata");
    }
    Objects.requireNonNull(screenState, "screenState");
    errorKey = Objects.requireNonNullElse(errorKey, "");
    actionMessage = Objects.requireNonNullElse(actionMessage, "");
  }

  public PublicCommission selected() {
    if (selectedCommissionId == null) return null;
    return commissions.stream()
        .filter(value -> value.commissionId().equals(selectedCommissionId))
        .findFirst()
        .orElse(null);
  }
}
