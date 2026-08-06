package com.mo.economy_system.common.network;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record DeliveryBoxDataResponseMessage(
    DeliveryBoxResponseKind kind, long requestId, List<DeliveryBoxEntrySnapshot> entries)
    implements EconomyNetworkMessage {
  public DeliveryBoxDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0) throw new IllegalArgumentException("negative delivery request id");
    Objects.requireNonNull(entries, "entries");
    if (entries.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("null delivery entry");
    }
    entries = List.copyOf(entries);
    if (kind == DeliveryBoxResponseKind.ERROR && !entries.isEmpty()) {
      throw new IllegalArgumentException("error delivery response cannot carry entries");
    }
    if (entries.size() > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
      throw new IllegalArgumentException("too many delivery entries");
    }
    Set<UUID> ids = new HashSet<>();
    for (DeliveryBoxEntrySnapshot entry : entries) {
      if (!ids.add(entry.entryId())) throw new IllegalArgumentException("duplicate delivery entry");
    }
  }

  public static DeliveryBoxDataResponseMessage data(
      long requestId, List<DeliveryBoxEntrySnapshot> entries) {
    return new DeliveryBoxDataResponseMessage(DeliveryBoxResponseKind.DATA, requestId, entries);
  }

  public static DeliveryBoxDataResponseMessage error(long requestId) {
    return new DeliveryBoxDataResponseMessage(DeliveryBoxResponseKind.ERROR, requestId, List.of());
  }
}
