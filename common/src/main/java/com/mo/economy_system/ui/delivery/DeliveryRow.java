package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import java.util.Objects;
import java.util.UUID;

public record DeliveryRow(DeliveryBoxEntrySnapshot entry, String displayName) {
  public DeliveryRow(DeliveryBoxEntrySnapshot entry) { this(entry, ""); }
  public DeliveryRow {
    Objects.requireNonNull(entry, "entry");
    displayName = Objects.requireNonNullElse(displayName, "").trim();
  }

  public UUID entryId() {
    return entry.entryId();
  }
}
