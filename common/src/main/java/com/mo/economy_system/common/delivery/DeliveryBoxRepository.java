package com.mo.economy_system.common.delivery;

import java.util.List;
import java.util.UUID;

public interface DeliveryBoxRepository {
  List<DeliveryBoxEntrySnapshot> list(UUID ownerId);

  Reservation reserve(UUID ownerId, UUID entryId);

  interface Reservation {
    DeliveryBoxEntrySnapshot entry();

    CommitResult commit(DeliveryBoxLedger.DirtyMarker dirty);

    void release();
  }

  enum CommitResult {
    REMOVED,
    /** Persistence failed and the entry was restored to the repository. */
    PERSIST_FAILED,
    /** The repository cannot prove whether the entry was removed. */
    STATE_UNKNOWN
  }
}
