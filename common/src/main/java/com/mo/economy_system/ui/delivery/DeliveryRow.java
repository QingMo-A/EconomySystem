package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import java.util.Objects;
import java.util.UUID;

public record DeliveryRow(DeliveryBoxEntrySnapshot entry) {
  public DeliveryRow {
    Objects.requireNonNull(entry, "entry");
  }

  public UUID entryId() {
    return entry.entryId();
  }
}
