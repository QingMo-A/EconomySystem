package com.mo.economy_system.ui.recycle;
import java.util.UUID;
public interface RecycleCenterPort {
  long nextRequestId();
  void requestData(long requestId);
  void submit(long requestId, UUID submissionId, String itemId, int amount);
}
