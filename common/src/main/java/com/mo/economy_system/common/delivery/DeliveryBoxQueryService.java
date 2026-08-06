package com.mo.economy_system.common.delivery;

import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class DeliveryBoxQueryService {
  private DeliveryBoxQueryService() {}

  public static DeliveryBoxDataResponseMessage query(
      DeliveryBoxDataRequestMessage request, UUID ownerId, DeliveryBoxRepository repository) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(repository, "repository");
    List<DeliveryBoxEntrySnapshot> entries =
        Objects.requireNonNull(repository.list(ownerId), "delivery entries");
    return DeliveryBoxDataResponseMessage.data(request.requestId(), entries);
  }
}
