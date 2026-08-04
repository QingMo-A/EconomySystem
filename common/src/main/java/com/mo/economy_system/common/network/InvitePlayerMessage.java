package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record InvitePlayerMessage(UUID territoryId, UUID targetPlayerId)
    implements EconomyNetworkMessage {
  public InvitePlayerMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(targetPlayerId, "targetPlayerId");
  }
}
