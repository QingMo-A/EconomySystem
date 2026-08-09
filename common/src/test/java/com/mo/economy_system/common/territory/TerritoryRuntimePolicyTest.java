package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritoryRuntimePolicy.BoundaryColumn;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryRuntimePolicyTest {
  private static final UUID OUTSIDER =
      UUID.fromString("00000000-0000-0000-0000-000000000099");

  @Test
  void permissionRulesAndOperatorBypassHaveOneDecisionTable() {
    Owned territory = TerritoryTestFixtures.owned();
    UUID member = territory.authorizedMembers().get(0).playerId();

    assertTrue(TerritoryRuntimePolicy.allows(
        territory, RuleAction.PLACE_BLOCK, territory.summary().ownerId(), false));
    assertFalse(TerritoryRuntimePolicy.allows(
        territory, RuleAction.PLACE_BLOCK, member, false));
    assertTrue(TerritoryRuntimePolicy.allows(
        territory, RuleAction.BREAK_BLOCK, member, false));
    assertFalse(TerritoryRuntimePolicy.allows(
        territory, RuleAction.BREAK_BLOCK, OUTSIDER, false));
    assertTrue(TerritoryRuntimePolicy.allows(
        territory, RuleAction.USE_ITEM, OUTSIDER, false));
    assertTrue(TerritoryRuntimePolicy.allows(
        territory, RuleAction.OPEN_CONTAINER, OUTSIDER, true));
    assertEquals(
        "message.territory.runtime.denied.open_container",
        TerritoryRuntimePolicy.denialMessageKey(RuleAction.OPEN_CONTAINER));
  }

  @Test
  void buffLevelsMapToMinecraftsZeroBasedAmplifier() {
    assertEquals(0, TerritoryRuntimePolicy.effectAmplifier(0));
    assertEquals(0, TerritoryRuntimePolicy.effectAmplifier(1));
    assertEquals(1, TerritoryRuntimePolicy.effectAmplifier(2));
    assertThrows(
        IllegalArgumentException.class, () -> TerritoryRuntimePolicy.effectAmplifier(-1));

    Owned source = TerritoryTestFixtures.owned();
    Buff unlockedLevelOne = copyBuff(source.buffs().get(0), true, 1);
    Buff locked = copyBuff(source.buffs().get(0), false, 3);
    Owned territory = new Owned(
        source.summary(),
        source.authorizedMembers(),
        Optional.empty(),
        source.rules(),
        List.of(unlockedLevelOne, withId(locked, "economy:locked")));
    var effects = TerritoryRuntimePolicy.activeEffects(territory);

    assertEquals(1, effects.size());
    assertEquals("minecraft:speed", effects.get(0).effectId());
    assertEquals(200, effects.get(0).durationTicks());
    assertEquals(0, effects.get(0).amplifier());
  }

  @Test
  void nearestBoundaryGeometryMatchesTheBaselineTieBreakAndClipsRadius() {
    List<BoundaryColumn> columns = TerritoryRuntimePolicy.nearestBoundaryColumns(
        new Position(0, 64, 0), new Position(20, 70, 20), 3, 10, 2);

    assertEquals(
        List.of(
            new BoundaryColumn(0, 8, 64, 70),
            new BoundaryColumn(0, 9, 64, 70),
            new BoundaryColumn(0, 10, 64, 70),
            new BoundaryColumn(0, 11, 64, 70),
            new BoundaryColumn(0, 12, 64, 70)),
        columns);
    assertEquals(
        List.of(new BoundaryColumn(Integer.MIN_VALUE, 0, 1, 1)),
        TerritoryRuntimePolicy.nearestBoundaryColumns(
            new Position(Integer.MIN_VALUE, 1, 0),
            new Position(Integer.MIN_VALUE, 1, 0),
            Integer.MIN_VALUE,
            0,
            16));
  }

  private static Buff copyBuff(Buff value, boolean unlocked, int level) {
    return new Buff(
        value.id(),
        value.displayText(),
        value.effectId(),
        value.initialUnlocked(),
        value.initialLevel(),
        value.singleUpgradeLevel(),
        value.maxLevel(),
        unlocked,
        level,
        value.upgradeCosts());
  }

  private static Buff withId(Buff value, String id) {
    return new Buff(
        id,
        value.displayText(),
        value.effectId(),
        value.initialUnlocked(),
        value.initialLevel(),
        value.singleUpgradeLevel(),
        value.maxLevel(),
        value.unlocked(),
        value.level(),
        value.upgradeCosts());
  }
}
