package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record RemoveTerritoryMessage(UUID territoryId) implements EconomyNetworkMessage {
  public RemoveTerritoryMessage { Objects.requireNonNull(territoryId, "territoryId"); }
}
