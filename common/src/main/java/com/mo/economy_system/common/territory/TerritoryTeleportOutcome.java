package com.mo.economy_system.common.territory;

import java.util.Objects;
import com.mo.economy_system.common.network.EconomyNetworkLimits;

public record TerritoryTeleportOutcome(TerritoryTeleportResult result, String territoryName) {
  public TerritoryTeleportOutcome {
    Objects.requireNonNull(result, "result");
    territoryName = territoryName == null ? "" : territoryName;
    if (result == TerritoryTeleportResult.SUCCESS) {
      if (territoryName.isBlank() || territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH)
        throw new IllegalArgumentException("SUCCESS requires a valid territoryName");
    } else if (!territoryName.isEmpty()) {
      throw new IllegalArgumentException("failure outcome cannot carry territoryName");
    }
  }
  public static TerritoryTeleportOutcome of(TerritoryTeleportResult result) { return new TerritoryTeleportOutcome(result, ""); }
  public static TerritoryTeleportOutcome success(String name) { return new TerritoryTeleportOutcome(TerritoryTeleportResult.SUCCESS, name); }
}
