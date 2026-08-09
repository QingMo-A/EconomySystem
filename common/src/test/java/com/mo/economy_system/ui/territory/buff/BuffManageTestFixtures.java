package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import java.util.List;

final class BuffManageTestFixtures {
  private BuffManageTestFixtures() {}

  static Buff buff(String id, boolean unlocked, int level, int maxLevel,
                   int itemCount, int experience, int currency) {
    return new Buff(id, id + " name", "effect." + id, false, 0, 1, maxLevel, unlocked,
        level, List.of(new BuffUpgradeCost(
            itemCount == 0 ? List.of() : List.of(new ItemRequirement("minecraft:diamond", itemCount)),
            experience, currency)));
  }

  static BuffResourceSnapshot resources(int diamonds, int experience) {
    return new BuffResourceSnapshot(java.util.Map.of("minecraft:diamond", diamonds),
        experience, true);
  }
}
