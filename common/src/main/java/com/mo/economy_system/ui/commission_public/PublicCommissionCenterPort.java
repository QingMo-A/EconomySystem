package com.mo.economy_system.ui.commission_public;

import java.util.UUID;

/** Target adapter port for public commission requests and submissions. */
public interface PublicCommissionCenterPort {
  long nextRequestId();

  void requestData(long requestId);

  void submit(long requestId, UUID commissionId, UUID submissionId, int amount);
}
