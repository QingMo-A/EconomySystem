package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryBuffCostTest {
  @Test
  void aggregatesEveryConfiguredCostLevelAndMergesItemIds() {
    Buff buff = buff(List.of(
        new BuffUpgradeCost(List.of(new ItemRequirement("minecraft:diamond", 2)), 3, 4),
        new BuffUpgradeCost(List.of(
            new ItemRequirement("minecraft:diamond", 5),
            new ItemRequirement("minecraft:gold_ingot", 7)), 11, 13)));

    TerritoryBuffCost cost = TerritoryBuffCost.aggregate(buff);

    assertEquals(7, cost.items().get("minecraft:diamond"));
    assertEquals(7, cost.items().get("minecraft:gold_ingot"));
    assertEquals(14, cost.experience());
    assertEquals(17, cost.currency());
  }

  @Test
  void rejectsTotalsOutsideTheTransactionIntegerRange() {
    Buff buff = buff(List.of(
        new BuffUpgradeCost(List.of(), Integer.MAX_VALUE, 0),
        new BuffUpgradeCost(List.of(), 1, 0)));
    assertThrows(IllegalArgumentException.class, () -> TerritoryBuffCost.aggregate(buff));
  }

  private static Buff buff(List<BuffUpgradeCost> costs) {
    return new Buff("speed", "Speed", "minecraft:speed", false, 0, 1, 3,
        false, 0, costs);
  }
}
