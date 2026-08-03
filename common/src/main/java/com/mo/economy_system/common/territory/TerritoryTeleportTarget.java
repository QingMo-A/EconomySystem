package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record TerritoryTeleportTarget(UUID territoryId, String territoryName, UUID ownerId,
    Set<UUID> authorizedPlayerIds, String dimensionId, Optional<Position> backpoint) {
  public TerritoryTeleportTarget {
    Objects.requireNonNull(territoryId, "territoryId"); Objects.requireNonNull(territoryName, "territoryName");
    Objects.requireNonNull(ownerId, "ownerId"); authorizedPlayerIds = Set.copyOf(authorizedPlayerIds);
    Objects.requireNonNull(dimensionId, "dimensionId"); backpoint = Objects.requireNonNull(backpoint, "backpoint");
  }
  public boolean permits(UUID playerId) { return ownerId.equals(playerId) || authorizedPlayerIds.contains(playerId); }
}
