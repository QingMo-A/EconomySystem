package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TerritoryTestFixtures {
  public static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private TerritoryTestFixtures() {}

  public static Summary summary(UUID id, UUID owner, String name) {
    return new Summary(id, owner, "Owner", name, new Position(1, 64, 2),
        new Position(20, 80, 30), "minecraft:the_nether");
  }

  public static Owned owned() {
    List<Rule> rules = List.of(
        new Rule(RuleAction.PLACE_BLOCK, RuleLevel.OWNER_ONLY),
        new Rule(RuleAction.BREAK_BLOCK, RuleLevel.MEMBERS),
        new Rule(RuleAction.USE_ITEM, RuleLevel.EVERYONE),
        new Rule(RuleAction.INTERACT_BLOCK, RuleLevel.MEMBERS),
        new Rule(RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY));
    Buff buff = new Buff("economy:speed", "Speed boost", "minecraft:speed", false, 0, 1, 3,
        true, 2, List.of(
            new BuffUpgradeCost(List.of(new ItemRequirement("minecraft:diamond", 2)), 3, 10),
            new BuffUpgradeCost(List.of(new ItemRequirement("minecraft:emerald", 4),
                new ItemRequirement("minecraft:gold_ingot", 8)), 5, 20)));
    return new Owned(summary(UUID.fromString("10000000-0000-0000-0000-000000000001"), OWNER, "Home"),
        List.of(new Member(UUID.fromString("20000000-0000-0000-0000-000000000001"), "Member A"),
            new Member(UUID.fromString("20000000-0000-0000-0000-000000000002"), "Member B")),
        Optional.of(new Position(5, 70, 6)), rules, List.of(buff));
  }

  public static TerritoryDataResponseMessage response(long requestId) {
    return TerritoryDataResponseMessage.data(requestId, List.of(owned()), List.of(
        summary(UUID.fromString("30000000-0000-0000-0000-000000000001"),
            UUID.fromString("40000000-0000-0000-0000-000000000001"), "Friend")));
  }
}
