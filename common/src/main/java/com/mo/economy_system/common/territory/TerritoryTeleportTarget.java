package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.mo.economy_system.common.network.EconomyNetworkLimits;

public record TerritoryTeleportTarget(UUID territoryId, String territoryName, UUID ownerId,
    Set<UUID> authorizedPlayerIds, String dimensionId, Optional<Position> backpoint) {
  public TerritoryTeleportTarget {
    Objects.requireNonNull(territoryId, "territoryId"); Objects.requireNonNull(territoryName, "territoryName");
    if (territoryName.isBlank() || territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH)
      throw new IllegalArgumentException("invalid territoryName");
    Objects.requireNonNull(ownerId, "ownerId"); authorizedPlayerIds = Set.copyOf(Objects.requireNonNull(authorizedPlayerIds, "authorizedPlayerIds"));
    Objects.requireNonNull(dimensionId, "dimensionId");
    if (dimensionId.isBlank() || dimensionId.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH)
      throw new IllegalArgumentException("invalid dimensionId");
    backpoint = Objects.requireNonNull(backpoint, "backpoint");
  }
  public boolean permits(UUID playerId) { return playerId != null && (ownerId.equals(playerId) || authorizedPlayerIds.contains(playerId)); }
}
