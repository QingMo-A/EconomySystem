package com.mo.economy_system.common.territory;

import java.util.Objects;

public record TerritoryTeleportOutcome(TerritoryTeleportResult result, String territoryName) {
  public TerritoryTeleportOutcome {
    Objects.requireNonNull(result, "result");
    territoryName = territoryName == null ? "" : territoryName;
  }
  public static TerritoryTeleportOutcome of(TerritoryTeleportResult result) { return new TerritoryTeleportOutcome(result, ""); }
  public static TerritoryTeleportOutcome success(String name) { return new TerritoryTeleportOutcome(TerritoryTeleportResult.SUCCESS, name); }
}
