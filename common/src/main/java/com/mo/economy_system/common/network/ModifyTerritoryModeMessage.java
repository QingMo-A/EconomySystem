package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record ModifyTerritoryModeMessage(UUID territoryId) implements EconomyNetworkMessage {
  public ModifyTerritoryModeMessage {
    Objects.requireNonNull(territoryId, "territoryId");
  }
}
