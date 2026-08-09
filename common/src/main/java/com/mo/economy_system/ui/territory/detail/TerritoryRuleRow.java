package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.Objects;

public record TerritoryRuleRow(RuleAction action, RuleLevel level) {
  public TerritoryRuleRow {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(level, "level");
  }
}
