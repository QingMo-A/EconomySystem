package com.mo.economy_system.ui.commission;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import java.util.UUID;

public sealed interface CommissionCenterEvent
    permits CommissionCenterEvent.Initialize, CommissionCenterEvent.DataLoaded,
    CommissionCenterEvent.DataFailed, CommissionCenterEvent.ActionClicked,
    CommissionCenterEvent.Tick, CommissionCenterEvent.Selected {
  record Initialize(long nowNanos) implements CommissionCenterEvent {}
  record DataLoaded(CommissionDataResponseMessage response) implements CommissionCenterEvent {}
  record DataFailed(String errorKey) implements CommissionCenterEvent {}
  record ActionClicked(CommissionCenterAction action, UUID commissionId, int amount)
      implements CommissionCenterEvent {}
  record Tick(long nowNanos) implements CommissionCenterEvent {}
  record Selected(UUID commissionId) implements CommissionCenterEvent {}
}
