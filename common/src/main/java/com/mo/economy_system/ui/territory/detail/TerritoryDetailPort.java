package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.UUID;

/** Target adapter for the APIs used by the unified territory management center. */
public interface TerritoryDetailPort {
  long nextRequestId();
  void requestTerritory(UUID territoryId, long requestId);
  void requestPlayers();
  void resize(UUID territoryId);
  void copyTerritoryId(UUID territoryId);
  void submitAccess(UUID territoryId, UUID playerId, boolean allowed);
  void submitRule(UUID territoryId, RuleAction action, RuleLevel level);
  void submitTransfer(UUID territoryId, UUID playerId);
}
