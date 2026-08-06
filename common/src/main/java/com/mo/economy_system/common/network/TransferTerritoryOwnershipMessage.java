package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record TransferTerritoryOwnershipMessage(UUID territoryId, UUID targetPlayerId)
    implements EconomyNetworkMessage {
  public TransferTerritoryOwnershipMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
  }
}
