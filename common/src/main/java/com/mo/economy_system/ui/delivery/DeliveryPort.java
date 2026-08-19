package com.mo.economy_system.ui.delivery;

import java.util.UUID;

public interface DeliveryPort {
  long nextRequestId();
  void requestData(long requestId);
  void markRead(UUID mailId, long requestId);
  void delete(UUID mailId, long requestId);
  void claim(UUID mailId, UUID entryId, long requestId);
  void claimAll(UUID mailId, long requestId);
}
