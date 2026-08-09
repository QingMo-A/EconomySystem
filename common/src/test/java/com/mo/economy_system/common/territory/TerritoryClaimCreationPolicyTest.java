package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryClaimCreationPolicyTest {
  private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TERRITORY = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Test
  void createsTheSameDefaultsForEveryTarget() {
    Position first = new Position(3, 64, 5);
    Position second = new Position(9, 64, 11);
    TerritoryClaimService.Request request = new TerritoryClaimService.Request(
        OWNER, "Owner", "Home", "minecraft:overworld", first, second);
    var definition = new TerritoryBuffCatalogPolicy.Definition(
        "speed", "Speed", "minecraft:speed", false, 0, 2, 5,
        List.of(new BuffUpgradeCost(List.of(), 1, 2)));

    var created = TerritoryClaimCreationPolicy.create(request, TERRITORY, List.of(definition));

    assertEquals(TERRITORY, created.summary().territoryId());
    assertEquals(List.of(), created.authorizedMembers());
    assertEquals(java.util.Optional.of(first), created.backpoint());
    assertEquals(5, created.rules().size());
    assertTrue(created.rules().stream().allMatch(rule ->
        rule.level() == TerritorySnapshots.RuleLevel.MEMBERS));
    assertEquals(definition.initialBuff(), created.buffs().get(0));
  }
}
