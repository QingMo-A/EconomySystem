package com.mo.economy_system.api.territory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stable read-only public view of EconomySystem territory ownership and geometry. */
public interface EconomyTerritoryApi {
  List<TerritoryView> territories();

  Optional<TerritoryView> territory(UUID territoryId);

  List<TerritoryView> territoriesByOwner(UUID ownerId);

  /** Finds the territory containing this position in the session's bound level/dimension. */
  Optional<TerritoryView> territoryAt(int x, int y, int z);

  Relationship relationship(UUID territoryId, UUID playerId);

  enum Relationship {
    OWNER,
    MEMBER,
    NONE
  }

  record Position(int x, int y, int z) {}

  record TerritoryView(
      UUID territoryId,
      UUID ownerId,
      String ownerName,
      String name,
      Position pos1,
      Position pos2,
      String dimensionId,
      List<UUID> memberIds) {
    public TerritoryView {
      Objects.requireNonNull(territoryId, "territoryId");
      Objects.requireNonNull(ownerId, "ownerId");
      ownerName = Objects.requireNonNullElse(ownerName, "");
      name = Objects.requireNonNullElse(name, "");
      Objects.requireNonNull(pos1, "pos1");
      Objects.requireNonNull(pos2, "pos2");
      dimensionId = Objects.requireNonNull(dimensionId, "dimensionId");
      memberIds = List.copyOf(Objects.requireNonNull(memberIds, "memberIds"));
    }

    public boolean contains(int x, int y, int z) {
      return x >= Math.min(pos1.x(), pos2.x()) && x <= Math.max(pos1.x(), pos2.x())
          && y >= Math.min(pos1.y(), pos2.y()) && y <= Math.max(pos1.y(), pos2.y())
          && z >= Math.min(pos1.z(), pos2.z()) && z <= Math.max(pos1.z(), pos2.z());
    }
  }
}
