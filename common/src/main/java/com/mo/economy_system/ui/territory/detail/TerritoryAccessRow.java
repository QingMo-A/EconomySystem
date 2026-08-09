package com.mo.economy_system.ui.territory.detail;

import java.util.Objects;
import java.util.UUID;

public record TerritoryAccessRow(UUID playerId, String playerName, boolean allowed) {
  public TerritoryAccessRow {
    Objects.requireNonNull(playerId, "playerId");
    if (playerName == null || playerName.isBlank()) throw new IllegalArgumentException("playerName");
  }
}
