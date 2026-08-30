package com.mo.economy_system.ui.commission_public;

import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import java.util.UUID;

/** Events consumed by the loader-neutral public commission screen controller. */
public sealed interface PublicCommissionCenterEvent
    permits PublicCommissionCenterEvent.Initialize,
            PublicCommissionCenterEvent.DataLoaded,
            PublicCommissionCenterEvent.DataFailed,
            PublicCommissionCenterEvent.ActionResult,
            PublicCommissionCenterEvent.ActionClicked,
            PublicCommissionCenterEvent.Selected,
            PublicCommissionCenterEvent.Tick {
  record Initialize(long nowNanos) implements PublicCommissionCenterEvent {}

  record DataLoaded(PublicCommissionDataResponseMessage response)
      implements PublicCommissionCenterEvent {
    public DataLoaded {
      if (response == null) throw new NullPointerException("response");
    }
  }

  record DataFailed(long requestId, String errorKey) implements PublicCommissionCenterEvent {
    public DataFailed {
      if (requestId < 0) throw new IllegalArgumentException("request id must be non-negative");
    }
  }

  record ActionResult(PublicCommissionActionResponseMessage response)
      implements PublicCommissionCenterEvent {
    public ActionResult {
      if (response == null) throw new NullPointerException("response");
    }
  }

  record ActionClicked(PublicCommissionCenterAction action, UUID commissionId, int amount)
      implements PublicCommissionCenterEvent {
    public ActionClicked {
      if (action == null) throw new NullPointerException("action");
    }
  }

  record Selected(UUID commissionId) implements PublicCommissionCenterEvent {}

  record Tick(long nowNanos) implements PublicCommissionCenterEvent {}
}
