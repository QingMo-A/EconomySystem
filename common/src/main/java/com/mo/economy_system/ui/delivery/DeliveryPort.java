package com.mo.economy_system.ui.delivery;

import java.util.UUID;

public interface DeliveryPort {
  long nextRequestId();

  void requestData(long requestId);

  void claim(UUID entryId, long requestId);
}
