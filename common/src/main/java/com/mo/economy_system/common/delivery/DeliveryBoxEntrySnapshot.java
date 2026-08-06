package com.mo.economy_system.common.delivery;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;
import java.util.Objects;
import java.util.UUID;

public record DeliveryBoxEntrySnapshot(UUID entryId, ItemStackSnapshot item, String source) {
  public DeliveryBoxEntrySnapshot {
    Objects.requireNonNull(entryId, "entryId");
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(source, "source");
    if (source.isBlank() || source.length() > EconomyNetworkLimits.MAX_DELIVERY_SOURCE_LENGTH) {
      throw new IllegalArgumentException("invalid delivery source");
    }
    if (!ItemStackSnapshotValidator.validate(item).isSuccess()) {
      throw new IllegalArgumentException("invalid delivery item snapshot");
    }
  }
}
