package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record UpgradeTerritoryBuffMessage(UUID territoryId, String buffId)
    implements EconomyNetworkMessage {
  public UpgradeTerritoryBuffMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    UnlockTerritoryBuffMessage.validateBuffId(buffId);
  }
}
