package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative policy for setting a territory backpoint at the actor's current position. */
public final class TerritoryBackpointService {
  private TerritoryBackpointService() {}

  public static TerritoryManagementResult execute(
      UUID actorId,
      String dimensionId,
      Position point,
      Repository repository,
      Diagnostics diagnostics) {
    if (actorId == null
        || dimensionId == null
        || dimensionId.isBlank()
        || dimensionId.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH
        || point == null
        || repository == null
        || diagnostics == null
        || !TerritoryGeometry.validCoordinate(point)) {
      return TerritoryManagementResult.INVALID_TARGET;
    }

    final Owned territory;
    try {
      territory = repository.findAt(dimensionId, point.x(), point.z());
    } catch (RuntimeException failure) {
      warn(diagnostics, "lookup", actorId, null, failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    if (territory == null) return TerritoryManagementResult.NOT_FOUND;

    UUID territoryId = territory.summary().territoryId();
    if (!dimensionId.equals(territory.summary().dimensionId())
        || !TerritoryGeometry.rectangle(
                territory.summary().pos1(), territory.summary().pos2())
            .contains(point.x(), point.z())) {
      warn(
          diagnostics,
          "lookup-integrity",
          actorId,
          territoryId,
          new IllegalStateException("repository returned a territory outside the requested point"));
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    if (!actorId.equals(territory.summary().ownerId())) {
      return TerritoryManagementResult.NOT_OWNER;
    }
    if (territory.backpoint().equals(Optional.of(point))) {
      return TerritoryManagementResult.SUCCESS;
    }

    final RepositoryResult result;
    try {
      result = repository.setBackpoint(
          territoryId, actorId, territory.backpoint(), point);
    } catch (RuntimeException failure) {
      warn(diagnostics, "persist", actorId, territoryId, failure);
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
    if (result == null) return TerritoryManagementResult.STATE_UNKNOWN;
    return switch (result) {
      case UPDATED, UNCHANGED -> TerritoryManagementResult.SUCCESS;
      case NOT_FOUND -> TerritoryManagementResult.NOT_FOUND;
      case OWNER_CHANGED -> TerritoryManagementResult.NOT_OWNER;
      case SNAPSHOT_CHANGED, STATE_UNKNOWN -> TerritoryManagementResult.STATE_UNKNOWN;
      case PERSIST_FAILED -> TerritoryManagementResult.PERSIST_FAILED;
    };
  }

  private static void warn(
      Diagnostics diagnostics,
      String stage,
      UUID actorId,
      UUID territoryId,
      RuntimeException failure) {
    try {
      diagnostics.warning(stage, actorId, territoryId, failure);
    } catch (RuntimeException ignored) {
      // Diagnostics cannot change the authoritative result.
    }
  }

  public interface Repository {
    Owned findAt(String dimensionId, int x, int z);

    RepositoryResult setBackpoint(
        UUID territoryId,
        UUID expectedOwner,
        Optional<Position> expectedBackpoint,
        Position newBackpoint);
  }

  public enum RepositoryResult {
    UPDATED,
    UNCHANGED,
    NOT_FOUND,
    OWNER_CHANGED,
    SNAPSHOT_CHANGED,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String stage, UUID actorId, UUID territoryId, RuntimeException failure);

    static Diagnostics noop() {
      return (stage, actorId, territoryId, failure) -> {};
    }
  }
}
