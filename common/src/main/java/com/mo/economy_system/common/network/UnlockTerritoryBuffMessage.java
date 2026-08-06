package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record UnlockTerritoryBuffMessage(UUID territoryId, String buffId)
    implements EconomyNetworkMessage {
  public UnlockTerritoryBuffMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    validateBuffId(buffId);
  }

  static void validateBuffId(String value) {
    Objects.requireNonNull(value, "buffId");
    if (value.isBlank() || value.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH) {
      throw new IllegalArgumentException("invalid territory buff id");
    }
  }
}
