package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.Objects;
import java.util.UUID;

public final class TerritoryModifyModeService {
  private TerritoryModifyModeService() {}

  public static TerritoryManagementResult start(
      ModifyTerritoryModeMessage message,
      UUID playerId,
      String dimensionId,
      Repository repository,
      Starter starter) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(dimensionId, "dimensionId");
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(starter, "starter");
    try {
      Summary territory = repository.find(message.territoryId());
      if (territory == null) return TerritoryManagementResult.NOT_FOUND;
      if (!territory.ownerId().equals(playerId)) return TerritoryManagementResult.NOT_OWNER;
      if (!territory.dimensionId().equals(dimensionId)) return TerritoryManagementResult.WRONG_DIMENSION;
      return starter.start(message.territoryId())
          ? TerritoryManagementResult.SUCCESS
          : TerritoryManagementResult.STATE_UNKNOWN;
    } catch (RuntimeException failure) {
      return TerritoryManagementResult.STATE_UNKNOWN;
    }
  }

  @FunctionalInterface
  public interface Repository {
    Summary find(UUID territoryId);
  }

  @FunctionalInterface
  public interface Starter {
    boolean start(UUID territoryId);
  }
}
