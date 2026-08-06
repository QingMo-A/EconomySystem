package com.mo.economy_system.common.network;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record UpdateTerritoryRuleMessage(UUID territoryId, RuleAction action, RuleLevel level)
    implements EconomyNetworkMessage {
  public UpdateTerritoryRuleMessage {
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(level, "level");
  }
}
