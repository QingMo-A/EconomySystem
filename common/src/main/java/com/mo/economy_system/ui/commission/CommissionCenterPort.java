package com.mo.economy_system.ui.commission;

import java.util.UUID;

public interface CommissionCenterPort {
  long nextRequestId();
  void requestData(long requestId);
  void submit(long requestId, UUID commissionId, UUID submissionId, int amount);
}
