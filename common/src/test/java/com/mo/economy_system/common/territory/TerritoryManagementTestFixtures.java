package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TerritoryManagementTestFixtures {
  public static final UUID OWNER =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final UUID TERRITORY =
      UUID.fromString("10000000-0000-0000-0000-000000000001");
  public static final UUID MEMBER =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  private TerritoryManagementTestFixtures() {}

  public static Owned owned() {
    Summary summary = new Summary(
        TERRITORY,
        OWNER,
        "Owner",
        "Home",
        new Position(0, 64, 0),
        new Position(10, 80, 10),
        "minecraft:overworld");
    List<Rule> rules = Arrays.stream(RuleAction.values())
        .map(action -> new Rule(action, RuleLevel.MEMBERS))
        .toList();
    Buff buff = new Buff(
        "economy_system:speed",
        "Speed",
        "minecraft:speed",
        false,
        0,
        1,
        3,
        false,
        0,
        List.of(new BuffUpgradeCost(
            List.of(new ItemRequirement("minecraft:diamond", 2)), 3, 10)));
    return new Owned(
        summary,
        List.of(new Member(MEMBER, "Member")),
        Optional.of(new Position(1, 65, 1)),
        rules,
        List.of(buff));
  }
}
