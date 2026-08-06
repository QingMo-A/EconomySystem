package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record SingleTerritoryDataRequestMessage(UUID territoryId, long requestId)
    implements EconomyNetworkMessage {
  public SingleTerritoryDataRequestMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    if (requestId < 0) throw new IllegalArgumentException("negative single territory request id");
  }
}
