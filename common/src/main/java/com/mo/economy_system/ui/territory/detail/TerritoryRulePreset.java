package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;

/** Small client-side permission presets; the server still authoritatively applies every rule. */
public enum TerritoryRulePreset {
  PRIVATE,
  STANDARD,
  OPEN;

  public RuleLevel levelFor(RuleAction action) {
    return switch (this) {
      case PRIVATE -> RuleLevel.OWNER_ONLY;
      case STANDARD -> RuleLevel.MEMBERS;
      case OPEN -> switch (action) {
        case PLACE_BLOCK, BREAK_BLOCK, OPEN_CONTAINER -> RuleLevel.MEMBERS;
        case USE_ITEM, INTERACT_BLOCK -> RuleLevel.EVERYONE;
      };
    };
  }

  public String id() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
