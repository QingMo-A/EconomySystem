package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryBuffCatalogPolicyTest {
  @Test
  void addsRemovesAndUpdatesMetadataWhileKeepingProgress() {
    Buff old = buff("old", "Old", "minecraft:speed", true, 3, 3);
    Buff removed = buff("removed", "Removed", "minecraft:haste", true, 1, 2);
    var configured = List.of(
        definition("old", "Updated", "minecraft:haste", 1, 2),
        definition("new", "New", "minecraft:jump_boost", 0, 1));

    var result = TerritoryBuffCatalogPolicy.synchronize(List.of(old, removed), configured);

    assertTrue(result.changed());
    assertEquals(List.of("old", "new"), result.buffs().stream().map(Buff::id).toList());
    Buff updated = result.buffs().get(0);
    assertTrue(updated.unlocked());
    assertEquals(2, updated.level());
    assertEquals("minecraft:haste", updated.effectId());
    assertEquals(2, updated.maxLevel());
    assertEquals(0, result.buffs().get(1).level());
  }

  @Test
  void unchangedCatalogIsReportedWithoutAllocatingDifferentState() {
    var definition = definition("speed", "Speed", "minecraft:speed", 0, 3);
    Buff initial = definition.initialBuff();
    var result = TerritoryBuffCatalogPolicy.synchronize(List.of(initial), List.of(definition));
    assertEquals(List.of(initial), result.buffs());
    assertTrue(!result.changed());
  }

  private static TerritoryBuffCatalogPolicy.Definition definition(
      String id, String text, String effect, int initialLevel, int maxLevel) {
    return new TerritoryBuffCatalogPolicy.Definition(
        id, text, effect, false, initialLevel, 1, maxLevel,
        List.of(new BuffUpgradeCost(List.of(), 1, 2)));
  }

  private static Buff buff(
      String id, String text, String effect, boolean unlocked, int level, int maxLevel) {
    return new Buff(id, text, effect, false, 0, 1, maxLevel, unlocked, level,
        List.of(new BuffUpgradeCost(List.of(), 1, 2)));
  }
}
