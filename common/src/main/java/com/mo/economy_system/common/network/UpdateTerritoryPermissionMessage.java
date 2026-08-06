package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record UpdateTerritoryPermissionMessage(
    UUID territoryId, UUID targetPlayerId, boolean allowed) implements EconomyNetworkMessage {
  public UpdateTerritoryPermissionMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
  }
}
