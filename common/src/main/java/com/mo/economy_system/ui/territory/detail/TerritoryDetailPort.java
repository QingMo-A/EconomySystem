package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.UUID;

/** Target adapter for the small set of APIs used by territory detail actions. */
public interface TerritoryDetailPort {
  long nextRequestId();
  void requestTerritory(UUID territoryId, long requestId);
  void requestPlayers();
  void resize(UUID territoryId);
  void submitAccess(UUID territoryId, UUID playerId, boolean allowed);
  void submitRule(UUID territoryId, RuleAction action, RuleLevel level);
  void submitTransfer(UUID territoryId, UUID playerId);
}
